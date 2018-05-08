package fun.lib.actor.core;

import com.mongodb.client.MongoDatabase;

import fun.lib.actor.api.DFActorMongo;
import fun.lib.actor.po.DFMongoCfg;

public final class DFActorMongoWrap implements DFActorMongo{

	private final DFDbManager dbMgr;
	
	protected DFActorMongoWrap() {
		dbMgr = DFDbManager.get();
	}
	
	@Override
	public int initPool(DFMongoCfg cfg) {
		return dbMgr.initMongoPool(cfg);
	}
	@Override
	public MongoDatabase getDatabase(int id, String dbName) {
		return dbMgr.getMongoDatabase(id, dbName);
	}

	@Override
	public void closePool(int id) {
		dbMgr.closeMongoPool(id);
	}

}
