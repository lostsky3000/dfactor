package fun.lib.actor.example;
import fun.lib.actor.api.http.DFHttpSvrRequest;
import fun.lib.actor.api.http.DFHttpServerHandler;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.SslConfig;
/**
 * HttpsServer示例
 * @author lostsky
 *
 */
public final class HttpsServer {

	public static void main(String[] args) {
		DFActorManager.get()
			.start(EntryActor.class); //启动入口actor，开始消息循环		
	}
	
	private static class EntryActor extends DFActor{
		@Override
		public void onStart(Object param) {
			//测试证书&私钥生成命令： openssl req -x509 -newkey rsa:2048 -nodes -days 365 -keyout private.pem -out cert.crt
			//证书文件路径
			String certPath = "/var/dfactor/cert.crt";
			//私钥文件路径
			String pemPath = "/var/dfactor/private.pem";
			
			net.doHttpServer(DFTcpServerCfg.newCfg(443)
								.setSslConfig(SslConfig.newCfg()
												.certPath(certPath)
												.pemPath(pemPath)), 
							new DFHttpServerHandler() {
								@Override
								public int onHttpRequest(DFHttpSvrRequest req) {
									//response
									req.response("echo from ssl server, uri="+req.getUri())
										.send();
									return DFActorDefine.MSG_AUTO_RELEASE;
								}
								@Override
								public void onListenResult(boolean isSucc, String errMsg) {
									log.info("listen result: isSucc="+isSucc+", err="+errMsg);
									if(!isSucc){
										DFActorManager.get().shutdown();
									}
								}});
			//start http server
		}
		public EntryActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
	}
	
}