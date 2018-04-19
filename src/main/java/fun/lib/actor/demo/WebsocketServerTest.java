package fun.lib.actor.demo;

import java.util.HashMap;

import com.funtag.util.system.DFSysUtil;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;
import fun.lib.actor.helper.DFActorLogLevel;
import fun.lib.actor.po.DFTcpServerCfg;
import io.netty.buffer.ByteBuf;


/**
 * websocket服务端示例代码
 * @author lostsky
 * 
 *
 */

public class WebsocketServerTest {

	public static void main(String[] args) {
		//
		final DFActorManager mgr = DFActorManager.get();
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				//.setTimerThreadNum(1)  //定时器线程数，默认为1
				//.setLogLevel(DFActorLogLevel.DEBUG)   //框架日志级别，默认为debug
				//.setBlockWorkerThreadNum(0) //设置阻塞线程的数量(一般用于阻塞io，如数据库io等)，默认为0，不启动
				//.setUseSysLog(useSysLog)   //设置是否使用框架log，默认为true
				.setLogicWorkerThreadNum(2);  //设置处理逻辑的线程数量
				
		//启动入口actor，开始事件循环		
		mgr.start(cfg, "ActorWSTest", ActorWSTest.class, null, 0, DFActorDefine.CONSUME_AUTO);
	}
	
	static class ActorWSTest extends DFActor{
		public ActorWSTest(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}

		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		//监听端口
		private final int serverPort = 10086;
		
		@Override
		public void onStart(Object param) {
			DFTcpServerCfg cfg = new DFTcpServerCfg(serverPort, 2, 1);
			cfg.setTcpNoDelay(true)
				//设置tcp server编码类型为websocket
				.setTcpDecodeType(DFActorDefine.TCP_DECODE_WEBSOCKET)
				//设置websocket uri
				.setWsUri("test");
			
			log.debug("onStart, ready to listen on port "+serverPort);
			
			net.doTcpListen(cfg, serverPort);
		}
		
		private final HashMap<Integer, DFTcpChannel> _mapChannel = new HashMap<>();
		@Override
		public void onTcpConnOpen(int requestId, DFTcpChannel channel) {
			//获取新建连接id
			final int channelId = channel.getChannelId();
			//保存新建连接对象
			if(_mapChannel.containsKey(channelId)){  //exception
				log.error("onTcpConnOpen, channelId duplicated id="+channelId);
			}
			_mapChannel.put(channelId, channel);
			log.debug("onTcpConnOpen, remote="+channel.getRemoteHost()+":"+channel.getRemotePort()
				+", channelId="+channelId);
		}
		@Override
		public void onTcpConnClose(int requestId, DFTcpChannel channel) {
			//获取连接对象id
			final int channelId = channel.getChannelId();
			//将连接对象从保存的在线连接中删除
			_mapChannel.remove(channelId);
			log.debug("onTcpConnClose, remote="+channel.getRemoteHost()+":"+channel.getRemotePort()
				+", channelId="+channelId);
		}
		@Override
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, ByteBuf msg) {
			
			//消息对象交由框架释放
			return DFActorDefine.MSG_AUTO_RELEASE;  //DFActorDefine.MSG_MANUAL_RELEASE
		}
		@Override
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, String msg) {
			log.debug("onTcpRecvMsg, string = "+msg);
			//根据连接id获取连接对象，将消息返回
			channel.write(msg);
			//消息对象交由框架释放
			return DFActorDefine.MSG_AUTO_RELEASE;  //DFActorDefine.MSG_MANUAL_RELEASE
		}
		
		@Override
		public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg) {
			log.debug("onTcpServerListenResult, port="+requestId+", succ="+isSucc+", err="+errMsg);
		}
	}

}
