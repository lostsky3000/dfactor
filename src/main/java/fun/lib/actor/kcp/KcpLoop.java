package fun.lib.actor.kcp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import io.netty.channel.socket.DatagramPacket;

public final class KcpLoop implements Runnable{

	private final int id;
	private final LinkedBlockingQueue<DatagramPacket> queueRecv;
	private final KcpListener listener;
	private final KcpConfigInner kcpCfg;
	
	public KcpLoop(int id, KcpListener listener, KcpConfig kcpCfgOri) {
		this.id = id;
		this.listener = listener;
		queueRecv = new LinkedBlockingQueue<>();
		kcpCfg = KcpConfigInner.copyConfig(kcpCfgOri);
	}
	
	private final HashMap<Integer, Kcp> mapKcp = new HashMap<>();
	
	@Override
	public void run() {
		this.onLoop = true;
		DatagramPacket pack = null;
		Kcp kcp = null;
		long tmStart = 0;
		long tmNow = 0;
		final long intervalDef = 10;
		long interval = 0;
		int recvCount = 0;
		int connId = 0;
		//
		while(this.onLoop){
			try{
				tmStart = System.currentTimeMillis();
				//proc recv
				recvCount = 0;
				pack = queueRecv.poll();
				while(pack != null){
					connId = pack.content().readInt();
					kcp = mapKcp.get(connId);
					if(kcp == null){  //new conn
						kcp = new Kcp(listener, kcpCfg, pack.sender(), connId, tmStart);
						mapKcp.put(connId, kcp);
						//notify
						listener.onChannelActive(kcp, connId);
					}
					kcp.onReceiveRaw(pack);
					//
					if(++recvCount < 1000){
						pack = queueRecv.poll();
					}else{
						break;
					}
				}
				//proc update
				tmNow = System.currentTimeMillis();
				final Iterator<Kcp> itKcp = mapKcp.values().iterator();
				while(itKcp.hasNext()){
					kcp = itKcp.next();
					if(!kcp.isClosed()){
						kcp.onUpdate(tmNow);
					}else{
						itKcp.remove();
					}
				}
				//wait
				interval = intervalDef - System.currentTimeMillis() + tmStart;
				if(interval > 5){  
//					pack = queueRecv.poll(interval, TimeUnit.MILLISECONDS);
					Thread.sleep(interval);
				}
			}catch(Throwable e){
				e.printStackTrace();
			}
		}
		//
		this.release();
	}
	
	private void release(){
		while(!queueRecv.isEmpty()){
			final DatagramPacket pack = queueRecv.poll();
			pack.release();
		}
		final Iterator<Kcp> itKcp = mapKcp.values().iterator();
		while(itKcp.hasNext()){
			itKcp.next().release();
		}
	}
	
	public int onReceive(DatagramPacket pack){
		if(onLoop){
			if(queueRecv.offer(pack)){
				return 0;
			}
		}
		pack.release();
		return 1;
	}
	private volatile boolean onLoop = false;

	public void stop(){
		this.onLoop = false;
	}
}
