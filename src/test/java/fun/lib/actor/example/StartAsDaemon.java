package fun.lib.actor.example;

import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;

/**
 * daemon模式启动dfactor示例, 加载外部jar执行逻辑
 * @author lostsky
 *
 */
public final class StartAsDaemon {

	public static void main(String[] args) {
		
		String dirJar = "/var/dfactor/ext"; //jar文件存放目录
		String entryFullName = "fun.test.spi.TestEntryActor"; //启动actor全路径名，要求继承DFActor
		String param = "launch param"; //传入启动actor的参数
		DFActorManagerConfig cfg = new DFActorManagerConfig();
		//
		DFActorManager.get().startAsDaemon(cfg, dirJar, entryFullName, param);
	}

}
