package fun.lib.actor.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
	private final HashMap<String,Integer> mapDbId = new HashMap<>();
	
	private int idCount = 1;
	
	protected int initPool(DFDbCfg cfg) throws Throwable{
		lockDbRead.lock();
		try{
			Integer idExist = mapDbId.get(cfg.strId);
			if(idExist != null){ //exist
				return idExist;
			}
		}finally{
			lockDbRead.unlock();
		}
		//
		DataSource dbSrc = DFDbUtil.createMysqlDbSource(cfg.getUrl(), cfg.getUser(), cfg.getPwd(), 
				cfg.getInitSize(), cfg.getMaxActive(), cfg.getMaxWait(), cfg.getMaxIdle(), cfg.getMinIdle());
		//conn test
		boolean testFail = false;
		Connection conn = null;
		Throwable eOut = null;
		try{
			conn = dbSrc.getConnection();
			Statement stmt = conn.createStatement();
			stmt.executeQuery("select 1");
		}catch(Throwable e){
			testFail = true;
			eOut = e;
		}finally{
			if(conn != null){
				conn.close();
			}
			if(testFail){
				if(dbSrc != null){
					dbSrc.close(); dbSrc = null;
				}
			}
		}
		if(eOut != null){
			throw eOut;
		}
		//
		DbPoolWrap wrap = new DbPoolWrap(dbSrc, cfg.strId);
		lockDbWrite.lock();
		try{
			int curId = idCount;
			mapDb.put(curId, wrap);
			mapDbId.put(cfg.strId, curId);
			if(idCount >= Integer.MAX_VALUE){
				idCount = 1;
			}else{
				++idCount;
			}
			return curId;
		}finally{
			lockDbWrite.unlock();
		}
	}
	protected Connection getConn(int id) throws SQLException{
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
			if(wrap != null){
				mapDbId.remove(wrap.strId);
			}
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
			mapDbId.clear();
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
		private final String strId;
		private DbPoolWrap(DataSource src,String strId) {
			dbSrc = src;
			this.strId = strId;
		}
		private Connection getConn() throws SQLException{
			Connection conn = null;
			if(dbSrc != null){
				try {
					return dbSrc.getConnection();
				} catch (SQLException e) {
					throw e;
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
