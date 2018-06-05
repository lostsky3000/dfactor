package fun.lib.actor.api;

public interface DFActorLog {

	public void verb(Object msg);
	public void debug(Object msg);
	public void info(Object msg);
	public void warn(Object msg);
	public void error(Object msg);
	public void fatal(Object msg);
	
}
