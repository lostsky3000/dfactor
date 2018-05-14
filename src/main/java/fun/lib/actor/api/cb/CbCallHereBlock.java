package fun.lib.actor.api.cb;

public interface CbCallHereBlock {

	public void inBlockActor(int cmd, Object payload, CallHereContext ctx);
	
	public void onCallback(int cmd, Object payload);
}
