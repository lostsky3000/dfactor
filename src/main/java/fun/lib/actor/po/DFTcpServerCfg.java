package fun.lib.actor.po;

import fun.lib.actor.api.DFTcpDecoder;
import fun.lib.actor.api.DFTcpEncoder;
import fun.lib.actor.core.DFActorDefine;
import io.netty.channel.EventLoopGroup;

public final class DFTcpServerCfg {
	public final int port;
	public final int workerThreadNum;
	public final int bossThreadNum;
	public final EventLoopGroup ioGroup;
	//
	private volatile int soRecvBufLen = 2048;
	private volatile int soSendBufLen = 2048;
	private volatile boolean tcpNoDelay = true;
	private volatile boolean keepAlive = true;
	private volatile int soBackLog = 1024;
	
	private volatile int tcpProtocol = DFActorDefine.TCP_DECODE_RAW;
	private volatile int tcpMsgMaxLength = 1024*64;
	
	private volatile String wsUri = "";
	
	private volatile DFTcpDecoder decoder = null;
	private volatile DFTcpEncoder encoder = null;
	
	private volatile Object userHandler = null;
	
	private volatile DFSSLConfig sslConfig = null;
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
		ioGroup = null;
	}
	public DFTcpServerCfg(int port) {
		this(port, Math.min(4, Runtime.getRuntime().availableProcessors()), 1);
	}
	public DFTcpServerCfg(int port, EventLoopGroup ioGroup){
		this.port = port;
		this.ioGroup = ioGroup;
		this.workerThreadNum = 0;
		this.bossThreadNum = 0;
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
	
	public int getTcpProtocol(){
		return tcpProtocol;
	}
	public DFTcpServerCfg setTcpProtocol(int tcpProtocol){
		if(tcpProtocol == DFActorDefine.TCP_DECODE_LENGTH 
				||tcpProtocol == DFActorDefine.TCP_DECODE_RAW 
				||tcpProtocol == DFActorDefine.TCP_DECODE_WEBSOCKET 
				||tcpProtocol == DFActorDefine.TCP_DECODE_HTTP
				){ //valid
			
		}else{ //invalid
			tcpProtocol = DFActorDefine.TCP_DECODE_RAW;
		}
		this.tcpProtocol = tcpProtocol;
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
	public DFSSLConfig getSslConfig(){
		return sslConfig;
	}
	public DFTcpServerCfg setSslConfig(DFSSLConfig cfg) {
		this.sslConfig = cfg;
		return this;
	}
	
	//
	public static DFTcpServerCfg newCfg(int port){
		return new DFTcpServerCfg(port);
	}
}




