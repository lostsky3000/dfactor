package fun.lib.actor.po;

public final class DFDbCfg {
	
	private final String url;
	private final String user;
	private final String pwd;
	private int initSize = 4;
	private int maxActive = 10;
	private int maxWait = 10;
	private int maxIdle = 4;
	private int minIdle = 2;
	
	private DFDbCfg(String host, int port, String dbName, String user, String pwd) {
		//jdbc:mysql://host:port/dbName?useUnicode=true&characterEncoding=UTF-8
		this.url = "jdbc:mysql://"+host+":"+port+"/"+dbName+"?useUnicode=true&characterEncoding=UTF-8";
		this.user = user;
		this.pwd = pwd;
	}
	
	//set
	public DFDbCfg setInitSize(int initSize){
		this.initSize = initSize;
		return this;
	}
	public DFDbCfg setMaxActive(int maxActive){
		this.maxActive = maxActive;
		return this;
	}
	public DFDbCfg setMaxWait(int maxWait){
		this.maxWait = maxWait;
		return this;
	}
	public DFDbCfg setMaxIdle(int maxIdle){
		this.maxIdle = maxIdle;
		return this;
	}
	public DFDbCfg setMinIdle(int minIdle){
		this.minIdle = minIdle;
		return this;
	}
	
	
	//get
	public String getUrl() {
		return url;
	}
	public String getUser() {
		return user;
	}
	public String getPwd() {
		return pwd;
	}
	public int getInitSize() {
		return initSize;
	}
	public int getMaxActive() {
		return maxActive;
	}
	public int getMaxWait() {
		return maxWait;
	}
	public int getMaxIdle() {
		return maxIdle;
	}
	public int getMinIdle() {
		return minIdle;
	}
	
	public static DFDbCfg newCfg(String host, int port, String dbName, String user, String pwd){
		return new DFDbCfg(host, port, dbName, user, pwd);
	}
}
