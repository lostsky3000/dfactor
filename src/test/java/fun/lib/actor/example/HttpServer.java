package fun.lib.actor.example;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map.Entry;

import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.DFTcpDecoder;
import fun.lib.actor.api.http.DFHttpContentType;
import fun.lib.actor.api.http.DFHttpMethod;
import fun.lib.actor.api.http.DFHttpRequest;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;
import fun.lib.actor.po.DFTcpServerCfg;

/**
 * http服务端示例(包含post+get),contentType类型见DFHttpContentType
 * @author lostsky
 *
 */
public final class HttpServer {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		//启动配置参数
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setLogicWorkerThreadNum(2);  //设置逻辑线程数量
		//启动入口actor，开始消息循环		
		mgr.start(cfg, "EntryActor", EntryActor.class);
	}

	
	private static class EntryActor extends DFActor implements DFTcpDecoder, DFActorTcpDispatcher{
		public EntryActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onStart(Object param) {
			log.info("onStart,  curThread="+Thread.currentThread().getName());
			sys.timeout(DFActor.transTimeRealToTimer(1000), 1);	
		}
		
		private final int serverPort = 8080;
		@Override
		public void onTimeout(int requestId) {
			log.info("onTimeout,  curThread="+Thread.currentThread().getName());
			//
			DFTcpServerCfg cfg = new DFTcpServerCfg(serverPort, 2, 1)
					.setTcpDecodeType(DFActorDefine.TCP_DECODE_HTTP)
					.setDecoder(this);  //设置解码器
			net.doTcpListen(cfg, serverPort, this); //设置消息转发器
		}
		
		@Override
		public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg) {
			log.info("listen result: port="+requestId+", isSucc="+isSucc+", err="+errMsg);
			if(!isSucc){
				DFActorManager.get().shutdown();
			}
		}

		@Override
		public Object onDecode(Object msgRaw) {
			DFHttpRequest req = (DFHttpRequest) msgRaw;
			log.info("onDecode, uri="+req.getUri()+", contentType="+req.getContentType()+",  curThread="+Thread.currentThread().getName());
			//此处可解码也可返回null不解码
			return null;
		}
		@Override
		public int onMessageUnsafe(int channelId, InetSocketAddress addrRemote, Object msg) {
			//上面onDecode解码器未做解码处理， 此处msg为原始http请求
			DFHttpRequest req = (DFHttpRequest) msg;
			log.info("onMessageUnsafe, uri="+req.getUri()+", contentType="+req.getContentType()+",  curThread="+Thread.currentThread().getName());
			return id; //转发给本actor
		}
		@Override
		public int onTcpRecvMsgCustom(int requestId, DFTcpChannel channel, Object msg) {
			DFHttpRequest req = (DFHttpRequest) msg;
			boolean isPairData = true;  //默认数据为键值对
			if(req.getMethod().equalsIgnoreCase(DFHttpMethod.GET)){ //get
				log.info("onTcpRecvMsgCustom, uri="+req.getUri()+", contentType="+req.getContentType()
					+", method=GET"
					+",  curThread="+Thread.currentThread().getName());
			}else if(req.getMethod().equalsIgnoreCase(DFHttpMethod.POST)){ //post
				log.info("onTcpRecvMsgCustom, uri="+req.getUri()+", contentType="+req.getContentType()
					+", method=POST"
					+",  curThread="+Thread.currentThread().getName());
				String contentType = req.getContentType();
				if(contentType!=null && !contentType.equalsIgnoreCase(DFHttpContentType.FORM)){
					isPairData = false;
				}
			}
			//get headers
			Iterator<Entry<String,String>> it = req.getHeaderIterator();
			while(it.hasNext()){
				Entry<String,String> en = it.next();
				log.info("header: key="+en.getKey()+", val="+en.getValue());
			}
			if(isPairData){ //数据为键值对
				it = req.getQueryDataIterator();
				if(it != null){
					while(it.hasNext()){
						Entry<String,String> en = it.next();
						log.info("queryData: key="+en.getKey()+", val="+en.getValue());
					}
				}
			}else{ //application data
				log.info("applicationData = "+req.getApplicationData());
			}
			
			//返回成功
			channel.writeHttpRspWithError(200);
			return DFActorDefine.MSG_AUTO_RELEASE;
		}
		
		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			// TODO Auto-generated method stub
			return 0;
		}
		@Override
		public int onConnActiveUnsafe(int channelId, InetSocketAddress addrRemote) {
			// TODO Auto-generated method stub
			return 0;
		}
		@Override
		public int onConnInactiveUnsafe(int channelId, InetSocketAddress addrRemote) {
			// TODO Auto-generated method stub
			return 0;
		}
	}
}
