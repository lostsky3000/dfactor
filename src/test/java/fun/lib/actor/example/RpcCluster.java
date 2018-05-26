package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbRpc;
import fun.lib.actor.api.cb.RpcContext;
import fun.lib.actor.api.cb.RpcFuture;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFActorManagerConfig;


/**
 * 集群内结点之间rpc通信示例
 * @author lostsky
 *
 */
public final class RpcCluster {
	private static int NODE_IDX = 0; // 0:以game-1身份启动    1:以game-2身份启动
	private static final String[] ARR_NODE_NAME = {"game-1", "game-2"};
	private static final String[] ARR_ACTOR_NAME = {"AskActor", "AnswerActor"};
	
	public static void main(String[] args) {
		String nodeName = ARR_NODE_NAME[NODE_IDX];  //当前启动结点名字
		DFActorClusterConfig cfgCluster = 
				DFActorClusterConfig.newCfg(nodeName);   //结点名字，一个结点在集群内名字必须唯一(可不设置，默认会自动生成唯一名字)
		
		DFActorManagerConfig cfgStart = new DFActorManagerConfig()
				.setClusterConfig(cfgCluster);    //设置集群配置，开启集群功能
		//设置业务入口actor，集群初始化和发现完毕后将启动该actor
		ActorProp prop = ActorProp.newProp()
				.classz(NODE_IDX==0?AskActor.class:AnswerActor.class)
				.name(ARR_ACTOR_NAME[NODE_IDX]);
		DFActorManager.get().start(cfgStart, prop);
	}
	
	private static class AskActor extends DFActor{
		public AskActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			timer.timeout(2000, 0);
		}
		@Override
		public void onTimeout(int requestId) {
			if(sys.isNodeOnline(ARR_NODE_NAME[1])){  //game-2 is online
				RpcFuture future = sys.callClusterMethod(ARR_NODE_NAME[1], ARR_ACTOR_NAME[1]+1, "doMath", 168, "square"); 
				if(future.isSendSucc()){  //调用发送成功，可添加结果监听
					future.addListener(new CbRpc() {
						@Override
						public int onResponse(int cmd, Object payload) {
							log.info("rpcResponse: "+payload);
							return 0;
						}
						@Override
						public int onFailed(int code) {	//调用失败的回调(发送失败 or 回调超时) 见 RpcError.java
							log.error("rpcCall failed, code="+code);
							return 0;
						}
					}, 5000);  //设置回调超时5000毫秒
				}
			}
			timer.timeout(2000, 0);
		}
	}
	
	private static class AnswerActor extends DFActor{
		public AnswerActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
		
		//被调用方法需满足如下参数列表: public (int,Object,RpcContext)
		public void doMath(int cmd, Object payload, RpcContext ctx){
			log.info("recv ask, cmd="+cmd+", payload="+payload+", srcNode="+ctx.getSrcNode()+", srcActor="+ctx.getSrcActor());
			//响应请求
			ctx.response(0, "result: square("+cmd+")="+ cmd*cmd);
		}
	}

}
