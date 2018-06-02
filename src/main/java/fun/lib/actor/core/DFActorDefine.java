package fun.lib.actor.core;

public final class DFActorDefine {

	public static final int VERSION = 100000;
	
	public static final int CONSUME_AUTO = 0;
	public static final int CONSUME_SINGLE = 1;
//	public static final int CONSUME_QUARTER = 2;
	public static final int CONSUME_HALF = 2;
	public static final int CONSUME_ALL = 3;
		
	//
	public static final int SUBJECT_START = 1;
	public static final int SUBJECT_TIMER = 2;
	public static final int SUBJECT_SCHEDULE = 3;
	public static final int SUBJECT_NET = 4;
	public static final int SUBJECT_USER = 5;
	public static final int SUBJECT_CLUSTER = 6;
	public static final int SUBJECT_RPC = 7;
	public static final int SUBJECT_RPC_FAIL = 8;
	public static final int SUBJECT_NODE_EVENT = 9;
	
	//
	protected static final int NET_TCP_LISTEN_RESULT = 1;
	protected static final int NET_TCP_LISTEN_CLOSED = 2;
	protected static final int NET_TCP_CONNECT_RESULT = 3;
	protected static final int NET_TCP_CONNECT_OPEN = 4;
	public static final int NET_TCP_CONNECT_CLOSE = 5;
	public static final int NET_TCP_MESSAGE = 6;
	public static final int NET_TCP_MESSAGE_TEXT = 7;
	public static final int NET_TCP_MESSAGE_CUSTOM = 8;
	
	protected static final int NET_UDP_LISTEN_RESULT = 9;
	protected static final int NET_UDP_LISTEN_CLOSED = 10;
	protected static final int NET_UDP_MESSAGE = 11;
	
	public static final int NET_KCP_MESSAGE = 12;
	public static final int NET_KCP_ACTIVE = 13;
	public static final int NET_KCP_INACTIVE = 14;
	
	//
	public static final int MSG_AUTO_RELEASE = 0;
	public static final int MSG_MANUAL_RELEASE = -2409;
	
	//
	public static final int TCP_DECODE_LENGTH = 1;
	public static final int TCP_DECODE_RAW = 2;
	public static final int TCP_DECODE_WEBSOCKET = 3;
	public static final int TCP_DECODE_HTTP = 4;
	
	//
	protected static final String ACTOR_NAME_LOG = "SYSTEM_LOG_lostsky";
	protected static final String ACTOR_NAME_DEF_PFX = "DFActorDef_";
	protected static final int ACTOR_ID_LOG = 1;
	//
	public static final int ACTOR_ID_APP_BEGIN = 1000;
	
}
