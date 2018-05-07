package fun.lib.actor.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import org.apache.tomcat.jdbc.pool.DataSource;

import com.funtag.util.db.DFDbUtil;

import fun.lib.actor.po.DFDbCfg;

public final class DFMysqlManager {

	protected DFMysqlManager() {
		// TODO Auto-generated constructor stub
	}
	
	private final ReentrantReadWriteLock lockDb = new ReentrantReadWriteLock();
	private final ReadLock lockDbRead = lockDb.readLock();
	private final WriteLock lockDbWrite = lockDb.writeLock();
	private final HashMap<Integer, DbPoolWrap> mapDb = new HashMap<>();
	private int idCount = 1;
	
	protected int initPool(DFDbCfg cfg){
		final DataSource dbSrc = DFDbUtil.createMysqlDbSource(cfg.getUrl(), cfg.getUser(), cfg.getPwd(), 
				cfg.getInitSize(), cfg.getMaxActive(), cfg.getMaxWait(), cfg.getMaxIdle(), cfg.getMinIdle());
		DbPoolWrap wrap = new DbPoolWrap(dbSrc);
		//
		int curId = idCount;
		lockDbWrite.lock();
		try{
			mapDb.put(curId, wrap);
			if(idCount >= Integer.MAX_VALUE){
				idCount = 1;
			}else{
				++idCount;
			}
		}finally{
			lockDbWrite.unlock();
		}
		return curId;
	}
	protected Connection getConn(int id){
		Connection conn = null;
		lockDbRead.lock();
		try{
			final DbPoolWrap wrap = mapDb.get(id);
			if(wrap != null){
				conn = wrap.getConn();
			}
		}finally{
			lockDbRead.unlock();
		}
		return conn;
	}
	protected void closePool(int id){
		DbPoolWrap wrap = null;
		lockDbWrite.lock();
		try{
			wrap = mapDb.remove(id);
		}finally{
			lockDbWrite.unlock();
		}
		if(wrap != null){
			wrap.close();
		}
	}
	protected void closeAllPool(){
		LinkedList<DbPoolWrap> lsTmp = new LinkedList<>();
		lockDbWrite.lock();
		try{
			Iterator<DbPoolWrap> it = mapDb.values().iterator();
			while(it.hasNext()){
				lsTmp.offer(it.next());
			}
			mapDb.clear();
		}finally{
			lockDbWrite.unlock();
		}
		//
		Iterator<DbPoolWrap> it = lsTmp.iterator();
		while(it.hasNext()){
			it.next().close();
			it.remove();
		}
	}
	
	class DbPoolWrap{
		private DataSource dbSrc = null;
		private DbPoolWrap(DataSource src) {
			dbSrc = src;
		}
		private Connection getConn(){
			Connection conn = null;
			if(dbSrc != null){
				try {
					return dbSrc.getConnection();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			return conn;
		}
		private void close(){
			if(dbSrc != null){
				dbSrc.close();
				dbSrc = null;
			}
		}
	}
}
