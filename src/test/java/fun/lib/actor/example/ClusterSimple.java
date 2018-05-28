package fun.lib.actor.example;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.helper.DFActorLogLevel;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFActorManagerConfig;


/**
 * 集群功能简单示例
 * 模拟集群的两个结点"game-1"和"game-2"互相发现，互发消息
 * @author lostsky
 *
 */

public final class ClusterSimple {
	
	private static int NODE_IDX = 0; // 0:以game-1身份启动    1:以game-2身份启动
	private static final String[] ARR_NODE_NAME = {"game-1", "game-2"};
	
	
	public static void main(String[] args) {
		String nodeName = ARR_NODE_NAME[NODE_IDX];  //当前启动结点名字
		DFActorClusterConfig cfgCluster = 
				DFActorClusterConfig.newCfg(nodeName);   //结点名字，一个结点在集群内名字必须唯一(可不设置，默认会自动生成唯一名字)
		//以下为可选参数，根据实际应用情况设置
//				.setClusterName("MyGameCluster")  //集群名字，只有集群名字相同的结点才会互相发现并连接
//		.setNodeType("game");    //结点类型(可以给某一类功能的结点取同一个类型名字，实现诸如广播等功能)
//				.setSecretKey("1234567")    //结点间通信秘钥，可防止其它未授权结点连接，只有秘钥相同的结点才能互相通信
//				.addSpecifyIP("192.168.123.7");   //添加指定服务器地址(可以使结点发现更快速)
//				.setIPRange("192.168.123.5", "192.168.123.16"); //使用ip地址段的方式来发现结点
		//如果既未指定服务器地址，也未设置ip地址段，则结点会向全局域网内的服务器广播搜索
		
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
			if(NODE_IDX == 0){  //由game-1结点定时向game-2发送消息
				timer.timeout(1000, 10001);
			}
		}
		@Override
		public void onTimeout(int requestId) {
			if(NODE_IDX == 0){  //game-1 ask, game-2 answer
				//检测game-2是否已经加入集群
				String selfNode = ARR_NODE_NAME[NODE_IDX];
				String dstNode = ARR_NODE_NAME[(NODE_IDX+1)%2];
				if(sys.isNodeOnline(dstNode)){   //game-2已经加入集群，发送消息
					sys.sendToCluster(dstNode, "EntryActor", NODE_IDX, "ask from "+selfNode+", tm="+System.currentTimeMillis());
				}
				timer.timeout(1000, 10001);
			}
		}
		@Override
		public int onClusterMessage(String srcType, String srcNode, String srcActor, int cmd, Object payload) {
			log.info("onClusterMessage, from="+srcNode+", cmd="+cmd+", payload="+payload);
			if(NODE_IDX == 1){  //game-2收到game-1消息，回复
				sys.sendToCluster(srcNode, srcActor, 1, "answer from game-2, tm="+System.currentTimeMillis());
			}
			return 0;
		}
		
	}
}
