package fun.lib.actor.po;

import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFTcpDecoder;
import fun.lib.actor.api.DFTcpEncoder;
import fun.lib.actor.core.DFActorDefine;

public final class DFTcpServerCfg {
	public final int port;
	public final int workerThreadNum;
	public final int bossThreadNum;
	//
	private volatile int soRecvBufLen = 2048;
	private volatile int soSendBufLen = 2048;
	private volatile boolean tcpNoDelay = true;
	private volatile boolean keepAlive = true;
	private volatile int soBackLog = 128;
	
	private volatile int tcpDecodeType = DFActorDefine.TCP_DECODE_RAW;
	private volatile int tcpMsgMaxLength = 4096;
	
	private volatile String wsUri = "";
	
	private volatile DFTcpDecoder decoder = null;
	private volatile DFTcpEncoder encoder = null;
	
	private volatile Object userHandler = null;
	/**
	 * 
	 * @param port 监听端口
	 * @param workerThreadNum 通信层处理消息的线程数
	 * @param bossThreadNum 通信层处理连接的线程数
	 */
	public DFTcpServerCfg(int port, int workerThreadNum, int bossThreadNum) {
		this.port = port;
		if(workerThreadNum < 1){
			workerThreadNum = 1;
		}
		this.workerThreadNum = workerThreadNum;
		this.bossThreadNum = bossThreadNum;
	}
	public DFTcpServerCfg(int port) {
		this(port, Runtime.getRuntime().availableProcessors(), 0);
	}
	
	public String getWsUri(){
		return wsUri;
	}
	public DFTcpServerCfg setWsUri(String wsUri){
		this.wsUri = wsUri;
		return this;
	}
	//
	public int getSoRecvBufLen(){
		return soRecvBufLen;
	}
	public DFTcpServerCfg setSoRecvBufLen(int len){
		this.soRecvBufLen = len;
		return this;
	}
	public int getSoSendBufLen(){
		return soSendBufLen;
	}
	public DFTcpServerCfg setSoSendBufLen(int len){
		this.soSendBufLen = len;
		return this;
	}
	public int getSoBackLog(){
		return soBackLog;
	}
	public DFTcpServerCfg setSoBackLog(int backLog){
		this.soBackLog = backLog;
		return this;
	}
	
	public boolean isTcpNoDelay(){
		return tcpNoDelay;
	}
	public DFTcpServerCfg setTcpNoDelay(boolean tcpNoDelay){
		this.tcpNoDelay = tcpNoDelay;
		return this;
	}
	public boolean isKeepAlive(){
		return keepAlive;
	}
	public DFTcpServerCfg setKeepAlive(boolean keepAlive){
		this.keepAlive = keepAlive;
		return this;
	}
	
	public int getTcpDecodeType(){
		return tcpDecodeType;
	}
	public DFTcpServerCfg setTcpDecodeType(int tcpDecodeType){
		if(tcpDecodeType == DFActorDefine.TCP_DECODE_LENGTH 
				||tcpDecodeType == DFActorDefine.TCP_DECODE_RAW 
				||tcpDecodeType == DFActorDefine.TCP_DECODE_WEBSOCKET 
				||tcpDecodeType == DFActorDefine.TCP_DECODE_HTTP
				){ //valid
			
		}else{ //invalid
			tcpDecodeType = DFActorDefine.TCP_DECODE_RAW;
		}
		this.tcpDecodeType = tcpDecodeType;
		return this;
	}
	public int getTcpMsgMaxLength(){
		return tcpMsgMaxLength;
	}
	public DFTcpServerCfg setTcpMsgMaxLength(int maxLength){
		this.tcpMsgMaxLength = maxLength;
		return this;
	}
	public DFTcpDecoder getDecoder() {
		return decoder;
	}
	public DFTcpServerCfg setDecoder(DFTcpDecoder decoder) {
		this.decoder = decoder;
		return this;
	}
	public DFTcpEncoder getEncoder() {
		return encoder;
	}
	public DFTcpServerCfg setEncoder(DFTcpEncoder encoder) {
		this.encoder = encoder;
		return this;
	}
	public Object getUserHandler() {
		return userHandler;
	}
	public DFTcpServerCfg setUserHandler(Object userHandler) {
		this.userHandler = userHandler;
		return this;
	}
	
}




