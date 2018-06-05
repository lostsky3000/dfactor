package fun.lib.actor.example;

import com.alibaba.fastjson.JSONObject;

import fun.lib.actor.api.DFSerializable;
import fun.lib.actor.api.cb.RpcContext;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.define.RpcParamType;
import fun.lib.actor.example.po.ClusterCustomMsg;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFActorManagerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.CharsetUtil;

/**
 * 集群内结点之间通信，多种消息类型示例
 * 目前支持String,Json,byte[],ByteBuf,自定义,五种消息类型
 * 发送方传入什么参数类型，接收方就会收到什么参数类型
 * @author lostsky
 *
 */
public final class ClusterParam {
	private static int NODE_IDX = 1; // 0:以game-1身份启动    1:以game-2身份启动
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
			_doSend();
			timer.timeout(2000, 0);
		}
		private int sendCount = 0;
		//发起rpc调用
		private void _doSend(){
			if(sys.isNodeOnline(ARR_NODE_NAME[1])){  //game-2 is online
				String dstNode = ARR_NODE_NAME[1];
				String dstActor = ARR_ACTOR_NAME[1];
				int type = 1 + ++sendCount%5;
				if(RpcParamType.CUSTOM == type){ //自定义消息类型，需实现DFSerializable.java接口
					ClusterCustomMsg msg = new ClusterCustomMsg()
							.setId(1001).setAge(25).setName("lostsky3000");
					sys.rpcNode(dstNode, dstActor, "doMath", type, msg); 
					log.info("doSend, cmd="+type+", type=custom");
				}else if(RpcParamType.STRING == type){ //String类型
					sys.rpcNode(dstNode, dstActor, "doMath", type, "lostsky3000"); 
					log.info("doSend, cmd="+type+", type=string");
				}else if(RpcParamType.JSON == type){ //Json类型
					JSONObject msg = new JSONObject();
					msg.put("id", 1001); msg.put("age", 25); msg.put("name", "lostsky3000");
					sys.rpcNode(dstNode, dstActor, "doMath", type, msg); 
					log.info("doSend, cmd="+type+", type=json");
				}else if(RpcParamType.BYTE_BUF == type){ //ByteBuf类型
					ByteBuf msg = UnpooledByteBufAllocator.DEFAULT.heapBuffer(16);
					msg.writeCharSequence("lostsky3000", CharsetUtil.UTF_8);
					sys.rpcNode(dstNode, dstActor, "doMath", type, msg); 
					log.info("doSend, cmd="+type+", type=byteBuf");
				}else if(RpcParamType.BYTE_ARR == type){ //byte[]类型
					byte[] msg = "lostsky3000".getBytes(CharsetUtil.UTF_8);
					sys.rpcNode(dstNode, dstActor, "doMath", type, msg); 
					log.info("doSend, cmd="+type+", type=byteArray");
				}
			}
		}
	}
	//
	private static class AnswerActor extends DFActor{
		public AnswerActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
		}
		//被调用方法需满足如下参数列表: public (int,Object,RpcContext)
		public void doMath(int cmd, Object payload, RpcContext ctx){
			if(payload instanceof DFSerializable){  //自定义消息类型
				ClusterCustomMsg msg = (ClusterCustomMsg) payload;
				log.info("recv customMsg: cmd="+cmd+", "+msg);
			}else if(payload instanceof String){  //String类型
				log.info("recv stringMsg: cmd="+cmd+", "+payload);
			}else if(payload instanceof JSONObject){ //Json类型
				log.info("recv jsonMsg: cmd="+cmd+", "+payload);
			}else if(payload instanceof ByteBuf){ //ByteBuf类型
				ByteBuf msg = (ByteBuf) payload;
				log.info("recv byteBufMsg: cmd="+cmd+", "+msg.readCharSequence(msg.readableBytes(), CharsetUtil.UTF_8));
			}else if(payload instanceof byte[]){  //byte[]类型
				byte[] msg = (byte[]) payload;
				log.info("recv byteArrMsg: cmd="+cmd+", "+new String(msg, CharsetUtil.UTF_8));
			}
		}
	}
}
