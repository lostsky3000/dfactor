package fun.lib.actor.helper;


public final class ActorLogData{

	public final int level;
	public final String msg;
	public final String actorName;
	
	public ActorLogData(int level, final String msg, final String actorName) {
		this.level = level;
		this.msg = msg;
		this.actorName = actorName;
	}
}
