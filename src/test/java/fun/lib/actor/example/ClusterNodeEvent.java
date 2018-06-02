package fun.lib.actor.example;

import fun.lib.actor.api.cb.CbNode;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFActorManagerConfig;
import fun.lib.actor.po.DFNode;

/**
 * 监听集群内结点事件示例
 * 一共启动4个结点，3种类型
 * @author lostsky
 *
 */

public final class ClusterNodeEvent {
	
	private static int IDX_NODE = 3;  //0~3
	private static final String[] ARR_NODE_NAME = {"game-1", "game-2", "db-1", "chat-1"};
	private static final String[] ARR_NODE_TYPE = {"game", "game", "db", "chat"};
	
	public static void main(String[] args){
		String nodeName = ARR_NODE_NAME[IDX_NODE];
		String nodeType = ARR_NODE_TYPE[IDX_NODE];
		DFActorClusterConfig cfgCluster = 
				DFActorClusterConfig.newCfg(nodeName)   //结点名字，一个结点在集群内名字必须唯一(可不设置，默认会自动生成唯一名字)
					.setNodeType(nodeType);
		DFActorManagerConfig cfgStart = new DFActorManagerConfig()
				.setClusterConfig(cfgCluster);    //设置集群配置，开启集群功能
		//设置业务入口actor，集群初始化和发现完毕后将启动该actor
		ActorProp prop = ActorProp.newProp()
				.classz(EntryActor.class)
				.name("EntryActor");
		DFActorManager.get().start(cfgStart, prop);
	}
	
	private static class EntryActor extends DFActor{
		public EntryActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onStart(Object param) {
			if(IDX_NODE == 0){
				//监听所有结点的事件
				sys.listenNodeAll(new CbNode() {
					@Override
					public void onNodeRemove(DFNode node) {
						log.info("listenAllCb, nodeRemove: "+node);
					}
					@Override
					public void onNodeAdd(DFNode node) {
						log.info("listenAllCb, nodeAdd: "+node);
					}
				});
				//监听"db"类型结点的事件
				sys.listenNodeByType("db", new CbNode() {
					@Override
					public void onNodeRemove(DFNode node) {
						log.info("listenByType, nodeRemove: "+node);
					}
					@Override
					public void onNodeAdd(DFNode node) {
						log.info("listenByType, nodeAdd: "+node);
					}
				});
				//监听名字为"chat-1"结点的事件
				sys.listenNodeByName("chat-1", new CbNode() {
					@Override
					public void onNodeRemove(DFNode node) {
						log.info("listenByName, nodeRemove: "+node);
					}
					@Override
					public void onNodeAdd(DFNode node) {
						log.info("listenByName, nodeAdd: "+node);
					}
				});
			}
			
		}
	}
	
}
