package fun.lib.actor.example;

import java.net.InetSocketAddress;

import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.api.http.DFHttpSvrReq;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.DFTcpServerCfg;

public final class HttpServerBenchmark {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		//启动入口actor，开始消息循环		
		mgr.start(EntryActor.class);
	}

	
	private static class EntryActor extends DFActor{
		public EntryActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
		private int[] arrProcId = null;
		private int reqCount = 0;
		@Override
		public void onStart(Object param) {
			final int cpuNum = Runtime.getRuntime().availableProcessors();
			log.info("cpuNum = "+cpuNum);
			arrProcId = new int[cpuNum];
			for(int i=0; i<cpuNum; ++i){
				arrProcId[i] = sys.createActor(ProcActor.class, i);
			}
			
			DFTcpServerCfg cfg = DFTcpServerCfg.newCfg(8080);
			net.httpSvr(cfg, new CbHttpServer() {
				@Override
				public void onListenResult(boolean isSucc, String errMsg) {
					if(isSucc){
						log.info("httpSvr succ");
					}else{
						log.error("httpSvr failed: "+errMsg);
					}
				}
				@Override
				public int onHttpRequest(Object msg) {
					DFHttpSvrReq req = (DFHttpSvrReq) msg;
					req.response("rsp from mainActor").send();
					return 0;
				}
			}, new DFHttpDispatcher() {
				@Override
				public int onQueryMsgActorId(int port, InetSocketAddress addrRemote, Object msg) {
					return arrProcId[++reqCount%cpuNum];
				}
			});
		}
	}
	
	
	private static class ProcActor extends DFActor{
		public ProcActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		private int idx = 0;
		@Override
		public void onStart(Object param) {
			idx = (Integer)param;
		}
		@Override
		public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
			DFHttpSvrReq req = (DFHttpSvrReq) msg;
			req.response("rsp from procActor_"+idx).send();
			return 0;
		}
	}
}
