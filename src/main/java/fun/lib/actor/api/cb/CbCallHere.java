package fun.lib.actor.api.cb;

public interface CbCallHere {

	public void inOtherActor(int cmd, Object payload, CallHereContext ctx);
	
	public void onCallback(int cmd, Object payload);
}
