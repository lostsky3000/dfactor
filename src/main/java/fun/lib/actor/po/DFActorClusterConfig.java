package fun.lib.actor.po;

import java.util.LinkedList;

public final class DFActorClusterConfig {

	private DFActorClusterConfig(){
		
	}
	private DFActorClusterConfig(String nodeName){
		setNodeName(nodeName);
	}
	
	private String nodeName = null;
	private String nodeType = "cluster.node.test";
	private int ioThreadNum = 1;
	private String secretKey = "lostsky3000winthegame_dada";
	//
	private int basePort = 30203;
	private String clusterName = "cluster.name.test";
	private IPRange ipRange = null;
	private LinkedList<String> lsSpecifyIp = null;
	private boolean pingTest = false;
	
	//
	public String getClusterName(){
		return this.clusterName;
	}
	public DFActorClusterConfig setClusterName(String name){
		if(name != null && name.length() > 0){
			this.clusterName = name;
		}
		return this;
	}
	
	public int getBasePort(){
		return this.basePort;
	}
	public DFActorClusterConfig setBasePort(int port){
		this.basePort = port;
		return this;
	}
	
	public IPRange getIPRange(){
		return this.ipRange;
	}
	public DFActorClusterConfig setIPRange(String ipBegin, String ipEnd){
		this.ipRange = new IPRange(ipBegin, ipEnd);
		return this;
	}
	
	public LinkedList<String> getSpecifyIPList(){
		return this.lsSpecifyIp;
	}
	public DFActorClusterConfig addSpecifyIP(String ip){
		if(lsSpecifyIp == null){
			lsSpecifyIp = new LinkedList<>();
		}
		lsSpecifyIp.add(ip);
		ipRange = null;
		return this;
	}
	public DFActorClusterConfig addSpecifyIPList(String[] arrIP){
		if(lsSpecifyIp == null){
			lsSpecifyIp = new LinkedList<>();
		}
		int num = arrIP.length;
		for(int i=0; i<num; ++i){
			lsSpecifyIp.add(arrIP[i]);
		}
		ipRange = null;
		return this;
	}
	public boolean isPingTest(){
		return this.pingTest;
	}
	public DFActorClusterConfig setPingTest(boolean pingTest){
		this.pingTest = pingTest;
		return this;
	}
	public int getIoThreadNum(){
		return ioThreadNum;
	}
	public DFActorClusterConfig setIoThreadNum(int ioThNum){
		ioThreadNum = Math.max(1, ioThNum);
		return this;
	}
	
	//
	public String getNodeName(){
		return this.nodeName;
	}
	public DFActorClusterConfig setNodeName(String nodeName){
		if(nodeName != null && nodeName.length() > 0){
			this.nodeName = nodeName;
		}
		return this;
	}
	public String getNodeType(){
		return this.nodeType;
	}
	public DFActorClusterConfig setNodeType(String nodeType){
		if(nodeType != null && nodeType.length() > 0){
			this.nodeType = nodeType;
		}
		return this;
	}
	
	public String getSecretKey(){
		return this.secretKey;
	}
	public DFActorClusterConfig setSecretKey(String secretKey){
		if(secretKey != null && secretKey.length() > 0){
			this.secretKey = secretKey;
		}
		return this;
	}
	
	//
	public static DFActorClusterConfig newCfg(String nodeName){
		return new DFActorClusterConfig(nodeName);
	}
}
