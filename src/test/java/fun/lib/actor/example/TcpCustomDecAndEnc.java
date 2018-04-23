package fun.lib.actor.example;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.DFTcpDecoder;
import fun.lib.actor.api.DFTcpEncoder;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * tcp自定义消息编解码示例
 * @author lostsky
 *
 */
public class TcpCustomDecAndEnc {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setClientIoThreadNum(1);    //设置作为客户端向外连接时，通信层io使用的线程数
		//启动入口actor，开始事件循环		
		mgr.start(cfg, "Server", Server.class);
	}
	//server
	private static class Server extends DFActor implements DFTcpDecoder, DFTcpEncoder{
		public Server(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		private final int serverPort = 10001;
		@Override
		public void onStart(Object param) {
			DFTcpServerCfg cfg = new DFTcpServerCfg(serverPort, 2, 1) 
				.setTcpDecodeType(DFActorDefine.TCP_DECODE_RAW) //设置tcp server编码类型为raw,接收原始socket二进制流，前两个字节标识消息长度
				.setDecoder(this)  //设置自定义消息解码器  ByteBuf->String
				.setEncoder(this); //设置自定义消息编码器  String->ByteBuf
			log.info("onStart, ready to listen on port "+serverPort);
			//启动端口监听
			net.doTcpServer(cfg, serverPort);
		}
		@Override
		public void onTcpConnOpen(int requestId, DFTcpChannel channel) {
			//获取新建连接id
			final int channelId = channel.getChannelId();
			log.info("onTcpConnOpen, remote="+channel.getRemoteHost()+":"+channel.getRemotePort()
				+", channelId="+channelId);
		}
		@Override
		public void onTcpConnClose(int requestId, DFTcpChannel channel) {
			//获取连接对象id
			final int channelId = channel.getChannelId();
			log.info("onTcpConnClose, remote="+channel.getRemoteHost()+":"+channel.getRemotePort()
				+", channelId="+channelId);
		}
		@Override
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
			//msg已由解码器转换为String
			log.info("server onTcpRecvMsgCustom: msg="+msg);
			//向客户端返回
			channel.write("echo from server, tm="+System.currentTimeMillis());  //写入String，由编码器负责编码
			//消息对象交由框架释放
			return DFActorDefine.MSG_AUTO_RELEASE; 
		}
		@Override
		public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg) {
			log.info("onTcpServerListenResult, port="+requestId+", succ="+isSucc+", err="+errMsg);
			//创建一个actor模拟客户端发送
			sys.createActor("actorTcpCliTest", Client.class, new Integer(serverPort));
		}
		//消息解码  ByteBuf->String
		@Override
		public Object onDecode(Object msgRaw) {
			ByteBuf msg = (ByteBuf) msgRaw;  //收到的原始二进制数据
			//获取消息体长度
			final int msgLen = msg.readableBytes();
			//模拟解码
			String dataDec = "recv cli msg: len="+msgLen;
			return dataDec;
		}
		//消息编码 String->ByteBuf
		@Override
		public Object onEncode(Object msgRaw) {
			ByteBuf bufOut = null;
			try {
				byte[] bufRaw = ((String)msgRaw).getBytes("utf-8");
				bufOut = PooledByteBufAllocator.DEFAULT.ioBuffer(bufRaw.length); //分配内存
				bufOut.writeBytes(bufRaw); //写入数据
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return bufOut;
		}
	}	
	//client
	private static class Client extends DFActor{
		public Client(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}		
		//服务端端口
		private int serverPort = 0;
		@Override
		public void onStart(Object param) {
			serverPort = (Integer) param;
			//开始连接服务端
			DFTcpClientCfg cfg = new DFTcpClientCfg("127.0.0.1", serverPort)
				.setConnTimeout(5000) //设置连接超时，毫秒
				.setTcpDecodeType(DFActorDefine.TCP_DECODE_RAW); //设置解码器为raw，头两字节为包长度
			net.doTcpConnect(cfg, serverPort);
			
			//启动定时器定时发送  一秒发送一次
			final int delay = DFActor.transTimeRealToTimer(1000);
			sys.timeout(delay, 10000);
		}
		
		private DFTcpChannel svrChannel = null;
		@Override
		public void onTcpConnOpen(int requestId, DFTcpChannel channel) {
			svrChannel = channel;
			log.debug("onTcpConnOpen, conn svr succ");
		}
		@Override
		public void onTcpConnClose(int requestId, DFTcpChannel channel) {
			svrChannel = null;
			log.debug("onTcpConnOpen, disconnect with svr");
		}
		@Override
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object m) {
			ByteBuf msg = (ByteBuf) m;
			final int msgLen = msg.readableBytes();
			final String str = (String) msg.readCharSequence(msgLen, Charset.forName("utf-8"));
			log.debug("recv msg from svr: "+str);
			
			return DFActorDefine.MSG_AUTO_RELEASE;
		}
		@Override
		public void onTcpClientConnResult(int requestId, boolean isSucc, String errMsg) {
			log.debug("onTcpClientConnResult, requestId="+requestId+", succ="+isSucc+", errMsg="+errMsg);
		}
		
		@Override
		public void onTimeout(int requestId) {
			if(svrChannel != null){ //已连接到服务器
				//构造发送的数据
				final String str = "hello server, "+System.currentTimeMillis();
				try {
					byte[] arrByte = str.getBytes("utf-8");
					final ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(arrByte.length);
					buf.writeBytes(arrByte);
					//向服务端发送
					svrChannel.write(buf);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//启动下一个定时器
			final int delay = DFActor.transTimeRealToTimer(1000);
			sys.timeout(delay, requestId);
		}
	}
}
