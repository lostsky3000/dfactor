package fun.lib.actor.po;

public final class DFRedisCfg {

	private String host = null;
	private int port = 0;
	private String auth = null;
	
	private int maxTotal = 10;
	private int maxIdle = 4;
	private int minIdle = 2;
	private int connTimeoutMilli = 5000;
	private int borrowTimeoutMilli = 10000;
	public final String strId;
	
	private DFRedisCfg(String host, int port, String auth) {
		this.host = host;
		this.port = port;
		this.auth = auth;
		//
		this.strId = host+":"+port+"_"+auth;
	}

	public DFRedisCfg setMaxTotal(int maxTotal){
		this.maxTotal = Math.max(1, maxTotal);
		return this;
	}
	public DFRedisCfg setMaxIdle(int maxIdle){
		this.maxIdle = Math.max(1, maxIdle);
		return this;
	}
	public DFRedisCfg setMinIdle(int minIdle){
		this.minIdle = Math.max(1, minIdle);
		return this;
	}
	public DFRedisCfg setConnTimeoutMilli(int timeoutMilli){
		this.connTimeoutMilli = Math.max(100, timeoutMilli);
		return this;
	}
	public DFRedisCfg setBorrowTimeoutMilli(int timeoutMilli){
		this.borrowTimeoutMilli = Math.max(100, timeoutMilli);
		return this;
	}
	
	
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getAuth() {
		return auth;
	}

	public int getMaxTotal() {
		return maxTotal;
	}

	public int getMaxIdle() {
		return maxIdle;
	}

	public int getMinIdle() {
		return minIdle;
	}

	public int getConnTimeoutMilli() {
		return connTimeoutMilli;
	}

	public int getBorrowTimeoutMilli() {
		return borrowTimeoutMilli;
	}
	
	
	public static DFRedisCfg newCfg(String host, int port, String auth){
		return new DFRedisCfg(host, port, auth);
	}
}
