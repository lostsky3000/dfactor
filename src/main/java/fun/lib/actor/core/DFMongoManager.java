package fun.lib.actor.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoDatabase;

import fun.lib.actor.po.DFMongoCfg;

public final class DFMongoManager {

	protected DFMongoManager() {
		// TODO Auto-generated constructor stub
	}
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final ReadLock lockRead = lock.readLock();
	private final WriteLock lockWrite = lock.writeLock();
	private final HashMap<Integer, MongoClient> mapPool = new HashMap<>();
	private int idCount = 1;
	
	protected int initPool(DFMongoCfg cfg){
		MongoClient cli = null;
		MongoCredential credential = cfg.getCredential();
		MongoClientOptions options = cfg.getOptions();
		if(credential != null && options != null){
			cli = new MongoClient(cfg.getAllAddress(), credential, options);
		}else if(credential != null){
			cli = new MongoClient(cfg.getAllAddress(), credential, MongoClientOptions.builder().build());
		}else if(options != null){
			cli = new MongoClient(cfg.getAllAddress(), options);
		}else{
			cli = new MongoClient(cfg.getAllAddress());
		}
		//
		int curId = idCount;
		lockWrite.lock();
		try{
			mapPool.put(curId, cli);
			if(idCount >= Integer.MAX_VALUE){
				idCount = 1;
			}else{
				++idCount;
			}
		}finally{
			lockWrite.unlock();
		}
		return curId;
	}
	
	protected MongoDatabase getDatabase(int id, String dbName){
		MongoDatabase db = null;
		lockRead.lock();
		try{
			MongoClient cli = mapPool.get(id);
			if(cli != null){
				db = cli.getDatabase(dbName);
			}
		}finally{
			lockRead.unlock();
		}
		return db;
	}
	
	protected void closePool(int id){
		MongoClient cli = null;
		lockWrite.lock();
		try{
			cli = mapPool.remove(id);
		}finally{
			lockWrite.unlock();
		}
		if(cli != null){
			cli.close();
			cli = null;
		}
	}
	
	protected void closeAllPool(){
		LinkedList<MongoClient> lsTmp = new LinkedList<>();
		lockWrite.lock();
		try{
			Iterator<MongoClient> it = mapPool.values().iterator();
			while(it.hasNext()){
				lsTmp.offer(it.next());
				it.remove();
			}
		}finally{
			lockWrite.unlock();
		}
		//
		Iterator<MongoClient> it = lsTmp.iterator();
		while(it.hasNext()){
			it.next().close();
			it.remove();
		}
	}
	
	
}
