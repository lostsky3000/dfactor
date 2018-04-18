package com.funtag.util.db;

import java.sql.Connection;
import java.util.List;
import java.util.Random;

public final class DFDbMSSrc {

	private final DFDbSrc dbSrcMaster;
	private final List<DFDbSrc> lsSlave;
	private final int slaveNum;
	
	public DFDbMSSrc(DFDbSrc dbSrcMaster, List<DFDbSrc> lsSlave) {
		this.dbSrcMaster = dbSrcMaster;
		this.lsSlave = lsSlave;
		this.slaveNum = lsSlave.size();
	}
	
	public Connection getConnMaster(){
		if(dbSrcMaster != null){
			return dbSrcMaster.getConn();
		}
		return null;
	}
	
	public Connection getConnSlaveRand(final Random rand){
		if(slaveNum > 0){
			final DFDbSrc s = lsSlave.get(rand.nextInt(slaveNum));
			return s.getConn();
		}
		return null;
	}
	
	public Connection getConnSlave(int idx){
		if(idx >= 0 && idx < slaveNum){
			final DFDbSrc s = lsSlave.get(idx);
			return s.getConn();
		}
		return null;
	}
	
	public int getSlaveNum(){
		return slaveNum;
	}
	
	public void close(){
		if(dbSrcMaster != null){
			dbSrcMaster.close();
		}
		if(lsSlave != null){
			for(DFDbSrc s : lsSlave){
				s.close();
			}
		}
	}
	
}
