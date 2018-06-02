package fun.lib.actor.core;

public final class DFJsEvent {
	
	public String type;
	public boolean succ;
	public String err;
	
	public DFJsEvent(String type, boolean succ, String err) {
		this.type = type;
		this.succ = succ;
		this.err = err;
	}
}
