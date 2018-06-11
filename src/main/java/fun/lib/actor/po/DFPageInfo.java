package fun.lib.actor.po;

public final class DFPageInfo {

	private volatile int version = 0;
	public final String path;
	
	public DFPageInfo(String path) {
		this.path = path;
	}
	
	public int increaseVersion(){
		return ++version;
	}
	public int getVersion(){
		return this.version;
	}
}
