package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbRpc;
import fun.lib.actor.api.cb.RpcContext;
import fun.lib.actor.api.cb.RpcFuture;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
/**
 * 本地rpc示例,直接调用本进程内actor的自定义方法
 * @author lostsky
 *
 */
public final class RpcLocal {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		//启动入口actor，开始消息循环		
		mgr.start(AskActor.class);
	}

	
	private static class AskActor extends DFActor{
		public AskActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		private int answerId = 0;
		@Override
		public void onStart(Object param) {
			//创建被调用actor
			answerId = sys.createActor(AnswerActor.class);
			//启动定时器
			timer.timeout(1000, 0);
		}
		@Override
		public void onTimeout(int requestId) {
			RpcFuture future = sys.callMethod(answerId, "doMath", 168, "square");
			if(future.isSendSucc()){ //发送消息成功  可选择添加回调
				future.addListener(new CbRpc() {
					@Override
					public int onResponse(int cmd, Object payload) {
						log.info("recv answer: "+ payload);
						return 0;
					}
					@Override
					public int onFailed(int code) {   //失败时的回调, 见 RpcError.java
						log.error("rcpCall failed, code="+code);
						return 0;
					}
				}, 5000);  //设置超时5000毫秒
			}
			timer.timeout(1000, 0);
		}
	}
	
	private static class AnswerActor extends DFActor{
		public AnswerActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
		//被调用方法需满足如下参数列表: public (int,Object,RpcContext)
		public void doMath(int cmd, Object payload, RpcContext ctx){
			log.info("recv ask: cmd="+cmd+", payload="+payload);
			//响应请求
			ctx.response(0, "square("+cmd+")="+cmd*cmd+", now="+System.currentTimeMillis());
		}
	}
}
