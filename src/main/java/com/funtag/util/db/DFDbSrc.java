package com.funtag.util.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.tomcat.jdbc.pool.DataSource;

public final class DFDbSrc {

	private volatile DataSource dbSrc = null;
	
	public DFDbSrc(final DataSource dbSrc) {
		this.dbSrc = dbSrc;
	}
	
	public Connection getConn(){
		if(dbSrc != null){
			try {
				return dbSrc.getConnection();
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	public void close(){
		if(dbSrc != null){
			dbSrc.close();
		}
		dbSrc = null;
	}
}
