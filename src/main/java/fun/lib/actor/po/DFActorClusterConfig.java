package fun.lib.actor.po;

import java.util.LinkedList;

public final class DFActorClusterConfig {

	private DFActorClusterConfig(){
		
	}
	
	private int listenPort = 30203;
	private String clusterName = "cluster.name.test";
	private IPRange ipRange = null;
	private LinkedList<String> lsWhiteIp = null;
	
	public String getClusterName(){
		return this.clusterName;
	}
	public DFActorClusterConfig setClusterName(String name){
		this.clusterName = name;
		return this;
	}
	
	public int getListenPort(){
		return this.listenPort;
	}
	public DFActorClusterConfig setListenPort(int port){
		this.listenPort = port;
		return this;
	}
	
	public IPRange getIPRange(){
		return this.ipRange;
	}
	public DFActorClusterConfig setIPRange(String ipBegin, String ipEnd){
		this.ipRange = new IPRange(ipBegin, ipEnd);
		return this;
	}
	
	public LinkedList<String> getIPWhiteList(){
		return this.lsWhiteIp;
	}
	public DFActorClusterConfig addIpWhite(String ip){
		if(lsWhiteIp == null){
			lsWhiteIp = new LinkedList<>();
		}
		lsWhiteIp.offer(ip);
		return this;
	}
	
	//
	public static DFActorClusterConfig newCfg(){
		return new DFActorClusterConfig();
	}
}
