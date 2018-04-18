package com.funtag.util.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DFDbMSConfig {

	public final DFDbConfig master;
	private final List<DFDbConfig> lsSlave;
	public final int slaveNum;
	
	public DFDbMSConfig(DFDbConfig master, List<DFDbConfig> lsSlave) {
		this.master = master;
		this.lsSlave = new ArrayList<>(lsSlave.size());
		for(DFDbConfig cfg : lsSlave){
			this.lsSlave.add(cfg);
		}
		slaveNum = this.lsSlave.size();
	}
	
	public DFDbConfig getSlave(int idx){
		if(idx >= 0 && idx < slaveNum){
			return lsSlave.get(idx);
		}
		return null;
	}
	
}
