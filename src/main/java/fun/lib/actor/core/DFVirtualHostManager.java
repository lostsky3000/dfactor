package fun.lib.actor.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public final class DFVirtualHostManager {

	private final static DFVirtualHostManager instance = new DFVirtualHostManager();
	
	private DFVirtualHostManager() {
		// TODO Auto-generated constructor stub
	}
	
	public static DFVirtualHostManager get(){
		return instance;
	}
	
	private final Map<Integer, IVirtualHost> _mapHost = new ConcurrentHashMap<>();
	private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
	private final ReadLock _lockRead = _lock.readLock();
	private final WriteLock _lockWrite = _lock.writeLock();
	
	protected void addHost(IVirtualHost host){
		if(_hasShutdown){
			return ;
		}
//		_lockWrite.lock();
//		try{
			_mapHost.put(host.getPort(), host);
//		}finally{
//			_lockWrite.unlock();
//		}
	}
	
	protected IVirtualHost getHost(int port){
		IVirtualHost host = null;
//		_lockRead.lock();
//		try{
			host = _mapHost.get(port);
//		}finally{
//			_lockRead.unlock();
//		}
		return host;
	}
	
	private boolean _hasShutdown = false;
	protected void shutdown(){
		if(_hasShutdown){
			return ;
		}
		_hasShutdown = true;
		LinkedList<IVirtualHost> lsHost = new LinkedList<>();
		_lockWrite.lock();
		try{
			Iterator<IVirtualHost> it = _mapHost.values().iterator();
			while(it.hasNext()){
				lsHost.offer(it.next());
			}
			_mapHost.clear();
		}finally{
			_lockWrite.unlock();
		}
		for(IVirtualHost host : lsHost){
			host.close();
		}
	}
	
}
