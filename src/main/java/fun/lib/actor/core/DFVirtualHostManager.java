package fun.lib.actor.core;

import java.util.concurrent.ConcurrentHashMap;

public final class DFVirtualHostManager {

	private final static DFVirtualHostManager instance = new DFVirtualHostManager();
	
	private DFVirtualHostManager() {
		// TODO Auto-generated constructor stub
	}
	
	public static DFVirtualHostManager get(){
		return instance;
	}
	
	private final ConcurrentHashMap<Integer, DFVirtualHost> _mapHost = new ConcurrentHashMap<>();
	
	protected void addHost(DFVirtualHost host){
		_mapHost.put(host.getPort(), host);
	}
	
	protected DFVirtualHost getHost(int port){
		return _mapHost.get(port);
	}
	
}
