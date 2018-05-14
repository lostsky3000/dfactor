package fun.lib.actor.api.cb;

import fun.lib.actor.api.DFActorDb;
import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.api.DFActorMongo;
import fun.lib.actor.api.DFActorNet;
import fun.lib.actor.api.DFActorRedis;
import fun.lib.actor.api.DFActorSystem;
import fun.lib.actor.api.DFActorTimer;

public interface CallHereContext {

	public String getActorName();
	
	public void callback(int cmd, Object payload);
	
	public DFActorLog getLog();
	public DFActorSystem getSys();
	public DFActorNet getNet();
	public DFActorTimer getTimer();
	public DFActorRedis getRedis();
	public DFActorDb getDb();
	public DFActorMongo getMongo();
}
