package fun.lib.actor.kcp;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.socket.DatagramPacket;

public final class Kcp implements KcpChannel{
	
	public static final int KCP_HEAD_SIZE = 10;
	public static final byte FLAG = 123;
	public static final byte ACT_ACK = 108;
	public static final byte ACT_DATA = 109;
	private static final int PACK_ID_MIN = 1;
	//
	private final ConcurrentLinkedQueue<ByteBuf> queueWaitSend;
	private final LinkedList<DatagramPacket> queueRecvRaw;
	private volatile boolean closed = false;
	private boolean released = false;
	private final KcpListener listener;
	private final InetSocketAddress remoteAddr;
	private final KcpConfigInner cfg;
	//
	private final DatagramPacket[] arrRecvWnd;
	private int recvWndL = 0;
	private int recvWndR = 0;
	//
	private final KcpSegment[] arrSendWnd;
	private int sendWndL = 0;
	private int sendWndR = 0;
	private int sendIdCount = 0;
	private long _tmNow = 0;
	private long _tmLastRecv = 0;
	private boolean _hasClosed = false;
	
	private final int _connId;
	//flag(1byte)+connId(4byte)+act(1byte)
	
	public Kcp(KcpListener listener, KcpConfigInner cfg, InetSocketAddress remoteAddr, int connId,
			long tmNow) {
		this.listener = listener;
		this.remoteAddr = remoteAddr;
		this.queueWaitSend = new ConcurrentLinkedQueue<>();
		this.queueRecvRaw = new LinkedList<>();
		this.cfg = cfg;
		this._connId = connId;
		//
		arrRecvWnd = new DatagramPacket[cfg.recvWnd + 6];
		recvWndL = PACK_ID_MIN;
		recvWndR = recvWndL + cfg.recvWnd;
		//
		arrSendWnd = new KcpSegment[cfg.sendWnd + 6];
		sendWndL = PACK_ID_MIN;
		sendWndR = sendWndL + cfg.sendWnd;
		sendIdCount = PACK_ID_MIN;
		//
		_tmNow = tmNow;
		_tmLastRecv = _tmNow;
	}
	
	public int onReceiveRaw(DatagramPacket pack){
		if(closed){
			pack.release();
			return 1;
		}
		_tmLastRecv = _tmNow;
		final ByteBuf buf = pack.content();
		final byte act = buf.readByte();
		if(act == ACT_ACK){  //ack packet
			_procAck(buf.readInt(), buf.readInt());
			pack.release();
		}else if(act == ACT_DATA){ //data packet
			final int packId = buf.readInt();
			if(packId < recvWndR){ //valid id
				if(!_recvWndPack(packId, pack)){	//pack duplicate	
					pack.release();
				}
				//send ack
				//flag(1byte) + connId(4byte) + act(1byte) + packId(4byte) + recvWndL(4byte) 
				final ByteBuf bufAck = PooledByteBufAllocator.DEFAULT.ioBuffer(14);
				bufAck.writeByte(FLAG).writeInt(_connId).writeByte(ACT_ACK)
					.writeInt(packId).writeInt(recvWndL);
				final DatagramPacket packAck = new DatagramPacket(bufAck, remoteAddr);
				listener.onOutput(packAck);  //notify logic new msg in
			}else{ //invalid pack id
				final ByteBuf bufTmp = pack.content();
				bufTmp.readLong();
				final String strTmp = (String) bufTmp.readCharSequence(bufTmp.readableBytes(), Charset.forName("utf-8"));
				System.out.println("Invalid packId="+packId+", recvWndR="+recvWndR
						+", data="+strTmp);
				pack.release();
			}
		}else{ //invalid pack
			pack.release();
		}
		return 0;
	}
	private boolean _recvWndPack(final int packId, final DatagramPacket pack){
		final int idx = packId%cfg.recvWnd;
		if(arrRecvWnd[idx] == null){ //valid
			if(packId < recvWndL){ //has read
				return false;
			}else{ //
				arrRecvWnd[idx] = pack;
				DatagramPacket tmpPack = arrRecvWnd[(recvWndL)%cfg.recvWnd];
				while(tmpPack != null){
					listener.onInput(tmpPack, this);
					arrRecvWnd[(recvWndL)%cfg.recvWnd] = null;
					tmpPack = arrRecvWnd[(++recvWndL)%cfg.recvWnd];
				}
				recvWndR = recvWndL + cfg.recvWnd;
				return true;
			}
		}else{ //ignore
			return false;
		}
	}
	
	private void _procAck(int packId, int rcvWndNxt){
		int idx = 0;
		KcpSegment seg = null;
		if(rcvWndNxt > sendWndL){ //remove all pack before rcvWndNxt
			for(int i=sendWndL; i<rcvWndNxt; ++i){ 
				idx = i%cfg.sendWnd;
				seg = arrSendWnd[idx];
				if(seg != null){  //remove
					seg.release();
					arrSendWnd[idx] = null;
				}
			}
			sendWndL = rcvWndNxt;
			sendWndR = sendWndL + cfg.sendWnd;
		}
		if(packId > sendWndL && packId < sendIdCount){ //fast resend check
			idx = packId%cfg.sendWnd;
			seg = arrSendWnd[idx];
			if(seg != null){
				seg.release();
			}
			arrSendWnd[idx] = null;
			for(int i=sendWndL; i<packId; ++i){
				seg = arrSendWnd[i%cfg.sendWnd];
				if(seg != null){  //fast resend count
					if(seg.doFastResendCheck(_tmNow)){ //resend max, closed
						this.close();
						return ;
					}
				}
			}
		}
	}
	
	public int onUpdate(long tmNow){
		if(closed){
			this.release();
			_onCloseNotify(1);
			return 1;
		}
		_tmNow = tmNow;
		//proc send
		while(sendIdCount < sendWndR && !queueWaitSend.isEmpty()){ //send wnd has free space
			final ByteBuf tmpBuf = queueWaitSend.poll();
			tmpBuf.setByte(0, FLAG);
			tmpBuf.setInt(1, _connId);
			tmpBuf.setByte(5, ACT_DATA);
			tmpBuf.setInt(6, sendIdCount);
			final KcpSegment seg = new KcpSegment(tmpBuf);
			arrSendWnd[sendIdCount%cfg.sendWnd] = seg;
			++sendIdCount;
			//notify send
			seg.doSend(_tmNow);
		}
		//proc ack timeout
		KcpSegment seg = null;
		for(int i=sendWndL; i<sendIdCount; ++i){
			seg = arrSendWnd[i%cfg.sendWnd];
			if(seg != null){
				if(seg.doSendTimeoutCheck(_tmNow)){ //resend count max
					this.close();
					_onCloseNotify(2);
					return 2;
				}
			}
		}
		//check idle timeout
		if(_tmNow - _tmLastRecv >= cfg.idleTimeout){
			this.close();
			_onCloseNotify(3);
			return 3;
		}
		return 0;
	}
	
	private void _onCloseNotify(int code){
		if(!_hasClosed){
			_hasClosed = true;
			listener.onChannelInactive(this, code);
		}
	}
	
	protected void release(){
		if(released){
			return ;
		}
		while(!queueWaitSend.isEmpty()){
			final ByteBuf buf = queueWaitSend.poll();
			buf.release();
		}
		while(!queueRecvRaw.isEmpty()){
			final DatagramPacket pack = queueRecvRaw.poll();
			pack.release();
		}
		//
		for(int i=0; i<cfg.recvWnd; ++i){
			final DatagramPacket pack = arrRecvWnd[i];
			if(pack != null){
				pack.release();
				arrRecvWnd[i] = null;
			}
		}
		//
		for(int i=0; i<cfg.sendWnd; ++i){
			final KcpSegment seg = arrSendWnd[i];
			if(seg != null){
				seg.release();
				arrSendWnd[i] = null;
			}
		}
		released = true;
	}
	protected boolean isClosed(){
		return closed;
	}
	@Override
	public void close() {
		closed = true;
	}
	@Override
	public int write(ByteBuf bufSend) {
		if(closed){
			bufSend.release();
			return 1;
		}
		queueWaitSend.offer(bufSend);
		return 0;
	}
	
	private final class KcpSegment{
		private ByteBuf buf = null;
		private short fastResendTrig = 0;
		private short resendCount = 0;
		private long tmSend = 0;
		private KcpSegment(ByteBuf buf) {
			this.buf = buf;
		}
		private void doSend(long tmSend){
			if(buf != null){
				this.tmSend = tmSend;
				//
				final ByteBuf bufOut = PooledByteBufAllocator.DEFAULT.ioBuffer(buf.readableBytes());
				buf.markReaderIndex();
				bufOut.writeBytes(buf);
				buf.resetReaderIndex();
				listener.onOutput(new DatagramPacket(bufOut, remoteAddr));
			}
		}
		private boolean doFastResendCheck(long tmNow){
			if(++fastResendTrig >= cfg.fastResendTrig){  //resend
				fastResendTrig = 0;
				if(++resendCount > cfg.resendMax){
					return true;
				}
				doSend(tmNow);
			}
			return false;
		}
		private boolean doSendTimeoutCheck(long tmNow){
			if(tmNow - tmSend >= cfg.sendTimeout){
				if(++resendCount > cfg.resendMax){
					return true;
				}
				doSend(tmNow);
			}
			return false;
		}
		private void release(){
			if(buf != null){
				buf.release();
				buf = null;
			}
		}
	}

	@Override
	public int getConnId() {
		return this._connId;
	}
	
	//
	public int getRecvWndR(){
		return this.recvWndR;
	}
	public int getRecvWndL(){
		return this.recvWndL;
	}
}



