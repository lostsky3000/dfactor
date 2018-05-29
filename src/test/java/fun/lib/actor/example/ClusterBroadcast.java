package fun.lib.actor.example;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFClusterManager;
import fun.lib.actor.helper.DFActorLogLevel;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFActorManagerConfig;


/**
 * 集群结点广播示例
 * 模拟3个"game"类型结点和一个"chat"类型结点组网
 * "game-1"结点每隔2秒交替进行"game"类型和全局类型广播
 * @author lostsky
 *
 */

public final class ClusterBroadcast {
	
	private static int NODE_IDX = 0; // 0~3
	private static final String[] ARR_NODE_NAME = {"game-1", "game-2", "game-3", "chat-1"};
	private static final String[] ARR_NODE_TYPE = {"game", "game", "game", "chat"};
	
	public static void main(String[] args) {
		String nodeName = ARR_NODE_NAME[NODE_IDX];  //当前启动结点名字
		String nodeType = ARR_NODE_TYPE[NODE_IDX];  //当前启动结点类型
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
			log.info("onStart");
			if(NODE_IDX == 0){  //
				timer.timeout(2000, 0);
			}
		}
		
		private int reqCount = 0;
		@Override
		public void onTimeout(int requestId) {
			if(NODE_IDX == 0){
				if(++reqCount%2 == 0){ //向"game"类型结点广播
					int nodeNum = sys.getNodeNumByType("game");
					if(nodeNum > 0){
						sys.sendToClusterByType("game", "EntryActor", NODE_IDX, "reqByType from "+ARR_NODE_NAME[NODE_IDX]);
					}
				}else{ //向全部结点广播
					int nodeNum = sys.getAllNodeNum();
					if(nodeNum > 0){
						sys.sendToClusterAll("EntryActor", NODE_IDX, "reqForAll from "+ARR_NODE_NAME[NODE_IDX]);
					}
				}
				timer.timeout(2000, 0);
			}
		}
		@Override
		public int onClusterMessage(String srcType, String srcNode, String srcActor, int cmd, Object payload) {
			log.info(ARR_NODE_NAME[NODE_IDX]+"::onClusterMessage, from="+srcNode+", cmd="+cmd+", payload="+payload);
			return 0;
		}
		
	}
}
