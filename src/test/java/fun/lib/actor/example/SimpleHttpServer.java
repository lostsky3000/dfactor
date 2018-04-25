package fun.lib.actor.example;
import fun.lib.actor.api.http.DFHttpRequest;
import fun.lib.actor.api.http.DFHttpServerHandler;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
/**
 * 简单httpserver示例
 * @author lostsky
 *
 */
public final class SimpleHttpServer {

	public static void main(String[] args) {
		DFActorManager.get()
			.start(EntryActor.class); //启动入口actor，开始消息循环		
	}
	
	private static class EntryActor extends DFActor{
		@Override
		public void onStart(Object param) {
			net.doHttpServer(8080, new DFHttpServerHandler() {
				@Override
				public void onHttpRequest(DFHttpRequest req) {
					//response
					req.response("echo from server, uri="+req.getUri())
						.send();
				}
				@Override
				public void onListenResult(boolean isSucc, String errMsg) {
					log.info("listen result: isSucc="+isSucc+", err="+errMsg);
					if(!isSucc){
						DFActorManager.get().shutdown();
					}
				}
				
			});  //start http server
		}
		public EntryActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
	}
}
