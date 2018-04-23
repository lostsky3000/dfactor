package fun.lib.actor.core;

public final class DFActorMessage {

	protected int srcId;
	protected int dstId;
	protected int sessionId;
	protected int subject;
	protected int cmd;
	protected Object payload;
	protected Object context;
	protected Object userHandler;
	protected boolean isCb = false;
	
	public DFActorMessage(int srcId, int dstId, int sessionId, 
			int subject, int cmd, Object payload, Object context, Object userHandler, boolean isCb) {
		this.srcId = srcId;
		this.dstId = dstId;
		this.sessionId = sessionId;
		this.subject = subject;
		this.cmd = cmd;
		this.payload = payload;	
		this.context = context;
		this.userHandler = userHandler;
		this.isCb = isCb;
	}
	
	protected void reset(int srcId, int dstId, int sessionId, 
			int subject, int cmd, Object payload, Object context, Object userHandler, boolean isCb){
		this.srcId = srcId;
		this.dstId = dstId;
		this.sessionId = sessionId;
		this.subject = subject;
		this.cmd = cmd;
		this.payload = payload;
		this.context = context;
		this.userHandler = userHandler;
		this.isCb = isCb;
	}
	
	
}
