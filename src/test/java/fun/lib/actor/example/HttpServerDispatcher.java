package fun.lib.actor.example;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.http.DFActorHttpDispatcher;
import fun.lib.actor.api.http.DFHttpRequest;
import fun.lib.actor.api.http.DFHttpServerHandler;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;

public final class HttpServerDispatcher {

	public static void main(String[] args) {
		DFActorManager.get().start("EntryActor", EntryActor.class);
	}
	
	private static class EntryActor extends DFActor implements DFActorHttpDispatcher{
		
		private Map<String,Integer> mapUri = new HashMap<>();
		@Override
		public void onStart(Object param) {
			int actorId = sys.createActor("/index", IndexActor.class);
			mapUri.put("/index", actorId);
			actorId = sys.createActor("/user", UserActor.class);
			mapUri.put("/user", actorId);
			//
			net.doHttpServer(8080, new DFHttpServerHandler() {
				@Override
				public void onListenResult(boolean isSucc, String errMsg) {
					log.info("listen result: isSucc="+isSucc+", err="+errMsg);
					if(!isSucc){
						DFActorManager.get().shutdown();
					}
				}
				@Override
				public void onHttpRequest(DFHttpRequest req) {
					//response
					req.response("echo from server, entryModule, reqUri="+req.getUri()+", curThread="+Thread.currentThread().getName())
						.send();
				}
			}, this);  //start http server
		}
		@Override
		public int onQueryMsgActorId(int port, InetSocketAddress addrRemote, Object msg) {
			DFHttpRequest req = (DFHttpRequest) msg;
			Integer actorId = mapUri.get(req.getUri());
			if(actorId != null && actorId != 0){ //有处理actor的映射
				return actorId;
			}
			return id;  //默认转发给本actor
		}
		
		public EntryActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
	}
	
	private static class IndexActor extends DFActor{
		@Override
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
			DFHttpRequest req = (DFHttpRequest) msg;
			//response
			req.response("echo from server, indexModule, reqUri="+req.getUri()+", curThread="+Thread.currentThread().getName())
				.send();
			return DFActorDefine.MSG_AUTO_RELEASE;
		}
		public IndexActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
	}
	
	private static class UserActor extends DFActor{
		@Override
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
			DFHttpRequest req = (DFHttpRequest) msg;
			//response
			req.response("echo from server, userModule, reqUri="+req.getUri()+", curThread="+Thread.currentThread().getName())
				.send();
			return DFActorDefine.MSG_AUTO_RELEASE;
		}
		public UserActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
		
	}
}
