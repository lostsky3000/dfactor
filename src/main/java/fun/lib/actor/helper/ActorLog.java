package fun.lib.actor.helper;

import com.funtag.util.log.DFLogFactory;
import com.funtag.util.log.DFLogger;

import fun.lib.actor.api.DFMsgBack;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;

public final class ActorLog extends DFActor{
	
	private final DFLogger log = DFLogFactory.create(ActorLog.class);
	private final StringBuffer sb = new StringBuffer();
	//
	protected ActorLog(Integer id, String name, Integer consumeType, Boolean isIoActor) {
		super(id, name, consumeType, isIoActor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int onMessage(int srcId, int cmd, Object payload, DFMsgBack cb) {
		final ActorLogData data = (ActorLogData) payload;
		sb.setLength(0);
		final String str = sb.append(data.actorName).append("(").append(srcId).append(")")
				.append(": ").append(data.msg).toString();
		switch(data.level){
		case DFLogFactory.LEVEL_VERB:
			log.V(str);
			break;
		case DFLogFactory.LEVEL_DEBUG:
			log.D(str);
			break;
		case DFLogFactory.LEVEL_INFO:
			log.I(str);
			break;
		case DFLogFactory.LEVEL_WARN:
			log.W(str);
			break;
		case DFLogFactory.LEVEL_ERROR:
			log.E(str);
			break;
		case DFLogFactory.LEVEL_FATAL:
			log.F(str);
			break;
		}
		return DFActorDefine.MSG_AUTO_RELEASE;
	}

	@Override
	public void onStart(Object param) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSchedule(long dltMilli) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTimeout(int requestId) {
		// TODO Auto-generated method stub
		
	}

	
}
