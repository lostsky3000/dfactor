package fun.lib.actor.example;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.http.DFHttpCliRequest;
import fun.lib.actor.api.http.DFHttpCliResponse;
import fun.lib.actor.api.http.DFHttpClientHandler;
import fun.lib.actor.api.http.DFHttpMethod;
import fun.lib.actor.api.http.DFHttpSvrReponse;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFHttpReqBuilder;
import fun.lib.actor.po.DFTcpClientCfg;

public final class HttpClient {

	public static void main(String[] args) {
		DFActorManager.get().start(EntryActor.class);
	}

	private static class EntryActor extends DFActor{
		@Override
		public void onStart(Object param) {
			DFHttpCliRequest req = DFHttpReqBuilder.build()
					.method(DFHttpMethod.GET).end();
			
			net.doHttpClient(DFTcpClientCfg.newCfg("www.baidu.com", 80)
								.setReqData(req),
					new DFHttpClientHandler() {
						@Override
						public int onHttpResponse(DFHttpCliResponse rsp, boolean isSucc, String errMsg) {
							if(isSucc){
								log.info("recv rsp, status="+rsp.getStatusCode() 
									+", contentType="+rsp.getContentType()+", isBinary="+rsp.isBinary()); 
							}else{
								log.info("conn http server failed, err="+errMsg);
							}
							return DFActorDefine.MSG_AUTO_RELEASE;
						}
					});
		}
		
		
		public EntryActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
		
	}
}
