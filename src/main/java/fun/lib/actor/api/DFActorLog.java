package fun.lib.actor.api;

public interface DFActorLog {

	public void verb(final String msg);
	public void debug(final String msg);
	public void info(final String msg);
	public void warn(final String msg);
	public void error(final String msg);
	public void fatal(final String msg);
	
}
