package com.funtag.util.db;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

public final class DFDbUtil {

	public static DataSource createMysqlDbSource(String url, String user, String pwd, 
			int initSize, int maxActive, int maxWait, int maxIdle, int minIdle){
		PoolProperties p = new PoolProperties();
		p.setDriverClassName("com.mysql.jdbc.Driver");
	    p.setUrl(url);
	    p.setUsername(user);
	    p.setPassword(pwd);
	    p.setJmxEnabled(true);
	    p.setTestWhileIdle(true);
	    p.setTestOnBorrow(false);
	    p.setTestOnReturn(false);
	    p.setValidationQuery("SELECT 1");
	    p.setValidationInterval(30000);
	    p.setTimeBetweenEvictionRunsMillis(30000);
	    p.setMaxActive(maxActive);
	    p.setInitialSize(initSize);
	    p.setMaxWait(maxWait);   //conn timeout 
	    p.setRemoveAbandonedTimeout(60);
	    p.setMinEvictableIdleTimeMillis(30000);
	    p.setMaxIdle(maxIdle);
	    p.setMinIdle(minIdle);
	    p.setLogAbandoned(true);
	    p.setRemoveAbandoned(true);
	    p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"+
	      "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
	    final DataSource dbSrc = new DataSource();
	    dbSrc.setPoolProperties(p);
	    return dbSrc;
	}
}
