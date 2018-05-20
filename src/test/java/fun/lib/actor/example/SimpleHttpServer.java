package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.http.DFHttpSvrReq;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorManagerConfig;
import fun.lib.actor.po.DFTcpServerCfg;
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
			net.doHttpServer(8080, new CbHttpServer() {
				@Override
				public int onHttpRequest(Object msg) {
					DFHttpSvrReq req = (DFHttpSvrReq) msg;
					//response
					req.response("echo from server, uri="+req.getUri())
						.send();
					return MSG_AUTO_RELEASE;
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
		public EntryActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
	}
}
