package fun.lib.actor.core;

public final class DFActorMessage {

	protected int srcId;
	protected int dstId;
	protected int sessionId;
	protected byte subject;
	protected int cmd;
	protected Object payload;
	protected Object context;
	protected Object userHandler;
	protected Object payload2;
	protected boolean isCb = false;
	protected String method = null;
	
	public DFActorMessage(int srcId, int dstId, int sessionId, 
			int subject, int cmd, Object payload, Object context, Object userHandler, boolean isCb, 
			Object payload2, String method) {
		this.srcId = srcId;
		this.dstId = dstId;
		this.sessionId = sessionId;
		this.subject = (byte)subject;
		this.cmd = cmd;
		this.payload = payload;	
		this.context = context;
		this.userHandler = userHandler;
		this.isCb = isCb;
		this.payload2 = payload2;
		this.method = method;
	}
	
	protected void reset(int srcId, int dstId, int sessionId, 
			int subject, int cmd, Object payload, Object context, Object userHandler, boolean isCb, 
			Object payload2, String method){
		this.srcId = srcId;
		this.dstId = dstId;
		this.sessionId = sessionId;
		this.subject = (byte)subject;
		this.cmd = cmd;
		this.payload = payload;
		this.context = context;
		this.userHandler = userHandler;
		this.isCb = isCb;
		this.payload2 = payload2;
		this.method = method;
	}
	
	
}
