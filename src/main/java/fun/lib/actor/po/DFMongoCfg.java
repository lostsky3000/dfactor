package fun.lib.actor.po;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public final class DFMongoCfg {
	
	private final List<ServerAddress> lsAddr = new ArrayList<>();
	private MongoCredential credential = null;
	private MongoClientOptions options = null;
	
	public DFMongoCfg(String host, int port) {
		lsAddr.add(new ServerAddress(host, port));
	}
	public DFMongoCfg() {
		// TODO Auto-generated constructor stub
	}
	
	//
	public DFMongoCfg addAddress(String host, int port){
		lsAddr.add(new ServerAddress(host, port));
		return this;
	}
	public DFMongoCfg addAddressList(List<ServerAddress> ls){
		lsAddr.addAll(ls);
		return this;
	}
	public DFMongoCfg setCredential(String dbName, String userName, String password){
		credential = MongoCredential.createCredential(userName, dbName, password.toCharArray());
		return this;
	}
	
	public DFMongoCfg setOptions(MongoClientOptions options){
		this.options = options;
		return this;
	}
	
	//
	public List<ServerAddress> getAllAddress(){
		return lsAddr;
	}
	public MongoCredential getCredential(){
		return credential;
	}
	public MongoClientOptions getOptions(){
		return options;
	}
	
	public static DFMongoCfg newCfg(String host, int port){
		return new DFMongoCfg(host, port);
	}
	public static DFMongoCfg newCfg(){
		return new DFMongoCfg();
	}
}
