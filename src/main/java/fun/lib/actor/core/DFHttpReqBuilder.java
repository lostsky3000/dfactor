package fun.lib.actor.core;

import fun.lib.actor.api.http.DFHttpCliRequest;

public final class DFHttpReqBuilder {

	public static DFHttpCliRequest build(){
		return new DFHttpCliReqWrap();
	}
}
