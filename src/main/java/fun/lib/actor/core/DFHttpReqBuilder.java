package fun.lib.actor.core;

import fun.lib.actor.api.http.DFHttpCliReq;

public final class DFHttpReqBuilder {

	public static DFHttpCliReq build(){
		return new DFHttpCliReqWrap();
	}
}
