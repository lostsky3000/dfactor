package fun.lib.actor.core;

import com.google.protobuf.GeneratedMessageV3.Builder;

public interface IScriptAPI {

	//sys function
	public int newActor(Object template, Object name, Object param, Object initCfg);
	public int send(Object dst, int cmd, Object payload);
	public int ret(int cmd, Object payload);
	public void timeout(int delay, int requestId);
	//buf function
	public IScriptBuffer newBuf(int capacity);
	//proto function
	public Object bufToProto(IScriptBuffer buf, String className);
	public IScriptBuffer protoToBuf(Builder<?> builder);
	public Object getProtoBuilder(String className);
	//lock function
	public boolean lockWrite(Object var, Object func);
	public boolean lockRead(Object var, Object func);
	//tcp function
	public boolean doTcpServer(Object cfg, Object func);
	public boolean doTcpConnect(Object cfg, Object func);
	public boolean tcpSend(Integer channelId, Object msg);
	
	public String bufToString(Object buf);
	
	//log function
	public void logV(Object msg);
	public void logD(Object msg);
	public void logI(Object msg);
	public void logW(Object msg);
	public void logE(Object msg);
	public void logF(Object msg);
}
