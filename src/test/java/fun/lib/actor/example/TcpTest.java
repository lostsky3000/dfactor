package fun.lib.actor.example;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * tcp服务端和客户端通信示例代码
 * @author lostsky
 *
 */
public class TcpTest {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setClientIoThreadNum(1)     //设置作为客户端向外连接时，通信层io使用的线程数
				.setLogicWorkerThreadNum(2);  //设置处理逻辑的线程数量
		//启动入口actor，开始事件循环		
		mgr.start(cfg, "Server", Server.class);
	}
	//server
	private static class Server extends DFActor{
		public Server(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		private final int serverPort = 10001;
		@Override
		public void onStart(Object param) {
			DFTcpServerCfg cfg = new DFTcpServerCfg(serverPort, 2, 1);
			cfg.setTcpNoDelay(true)
				//设置tcp server编码类型为raw,接收原始socket二进制流，前两个字节标识消息长度
				.setTcpDecodeType(DFActorDefine.TCP_DECODE_RAW);
			
			log.info("onStart, ready to listen on port "+serverPort);
			//启动端口监听
			net.doTcpListen(cfg, serverPort);
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
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, ByteBuf msg) {
			//获取消息体长度
			final int msgLen = msg.readableBytes();
			log.info("recv cli msg, len="+msgLen);
			//构造返回的消息
			final ByteBuf bufOut = PooledByteBufAllocator.DEFAULT.ioBuffer(msgLen);
			//拷贝收到的消息数据
			bufOut.writeBytes(msg);
			//向客户端返回
			channel.write(bufOut);
			//消息对象交由框架释放
			return DFActorDefine.MSG_AUTO_RELEASE;  //DFActorDefine.MSG_MANUAL_RELEASE
		}
		@Override
		public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg) {
			log.info("onTcpServerListenResult, port="+requestId+", succ="+isSucc+", err="+errMsg);
			//创建一个actor模拟客户端发送
			sys.createActor("actorTcpCliTest", Client.class, new Integer(serverPort));
		}
		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			// TODO Auto-generated method stub
			return 0;
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
			DFTcpClientCfg cfg = new DFTcpClientCfg("127.0.0.1", serverPort);
			cfg.setConnTimeout(5000) //设置连接超时，毫秒
				.setTcpNoDelay(true) //禁用nagle算法
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
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, ByteBuf msg) {
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
		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			// TODO Auto-generated method stub
			return 0;
		}
	}
}
