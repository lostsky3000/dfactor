package fun.lib.actor.po;

public final class DFWebResInfo {

	private volatile int version = 0;
	public final String path;
	
	public DFWebResInfo(String path) {
		this.path = path;
	}
	
	public int increaseVersion(){
		return ++version;
	}
	public int getVersion(){
		return this.version;
	}
}
