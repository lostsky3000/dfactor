package fun.lib.actor.example;

import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFActorManagerConfig;

public final class ClusterTest {
	
	public static void main(String[] args) {
		DFActorClusterConfig cfgCluster = DFActorClusterConfig.newCfg();
//				.setPingTest(false)
//				.addSpecifyIP("192.168.123.7");
//				.setIPRange("192.168.123.5", "192.168.123.16");
		DFActorManagerConfig cfgStart = new DFActorManagerConfig()
				.setClusterConfig(cfgCluster);
		
		DFActorManager.get().start(cfgStart, EntryActor.class);
	}

	private static class EntryActor extends DFActor{
		public EntryActor(Integer id, String name, Boolean isBlockActor) {
			super(id, name, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onStart(Object param) {
			log.info("onStart");
		}
	}
}
