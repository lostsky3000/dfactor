package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbHttpClient;
import fun.lib.actor.api.http.DFHttpCliReq;
import fun.lib.actor.api.http.DFHttpCliRsp;
import fun.lib.actor.api.http.DFHttpMethod;
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
			DFHttpCliReq req = DFHttpReqBuilder.build()
					.method(DFHttpMethod.GET).end();
			
			net.doHttpClient(DFTcpClientCfg.newCfg("www.baidu.com", 80)
								.setReqData(req),
					new CbHttpClient() {
						@Override
						public int onHttpResponse(Object msg, boolean isSucc, String errMsg) {
							DFHttpCliRsp rsp = (DFHttpCliRsp) msg;
							if(isSucc){
								log.info("recv rsp, status="+rsp.getStatus() 
									+", contentType="+rsp.getContentType()+", isBinary="+rsp.isBinary()); 
							}else{
								log.info("conn http server failed, err="+errMsg);
							}
							return 0;
						}
					});
		}
		
		
		public EntryActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
		
	}
}
