package fun.lib.actor.core;

import fun.lib.actor.api.cb.CbNode;

public final class RegNodeReq {

	protected final int type;
	protected final String value;
	protected final int srcId;
	protected final CbNode cb;
	
	protected RegNodeReq(int type, String value, int srcId, CbNode cb) {
		this.type = type;
		this.value = value;
		this.srcId = srcId;
		this.cb = cb;
	}
	
	public static final int ALL = 0;
	public static final int NODE_TYPE = 1;
	public static final int NODE_NAME = 2;
	
}
