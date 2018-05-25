package fun.lib.actor.core;

public final class DFNodeInfo {
	
	protected final String host;
	protected final int port;
	protected final String idAddr;
	
	protected DFNodeInfo(String host, int port) {
		this.host = host;
		this.port = port;
		this.idAddr = host+":"+port;
	}
}
