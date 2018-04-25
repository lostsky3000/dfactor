package fun.lib.actor.core;

import fun.lib.actor.helper.DFActorLogLevel;

public final class DFActorManagerConfig {

	private volatile int logicWorkerThreadNum = Math.max(2, Runtime.getRuntime().availableProcessors()); 
	private volatile int logLevel = DFActorLogLevel.DEBUG;
	private volatile int timerThreadNum = 1;
	private volatile int clientIoThreadNum = 1;
	
	private volatile int blockWorkerThreadNum = 1;
	
	private volatile boolean useSysLog = true;
	private volatile int sysLogConsumeType = DFActorDefine.CONSUME_AUTO;
	
	private volatile int timerTickPerWheel = 10000;
	
	public DFActorManagerConfig() {
		// TODO Auto-generated constructor stub
	}
	
	public int getSysLogConsumeType(){
		return sysLogConsumeType;
	}
	public DFActorManagerConfig setSysLogConsumeType(int consumeType){
		if(consumeType>=DFActorDefine.CONSUME_AUTO &&
				consumeType<=DFActorDefine.CONSUME_ALL){
			sysLogConsumeType = consumeType;
		}
		return this;
	}
	
	public int getBlockWorkerThreadNum(){
		return blockWorkerThreadNum;
	}
	public DFActorManagerConfig setBlockWorkerThreadNum(int blockWorkerThreadNum){
		this.blockWorkerThreadNum = blockWorkerThreadNum;
		return this;
	}
	
	public int getClientIoThreadNum(){
		return clientIoThreadNum;
	}
	public DFActorManagerConfig setClientIoThreadNum(int clientIoThreadNum){
		this.clientIoThreadNum = clientIoThreadNum;
		return this;
	}
	
	public int getTimerThreadNum(){
		return timerThreadNum;
	}
	public DFActorManagerConfig setTimerThreadNum(int timerThreadNum){
		this.timerThreadNum = timerThreadNum;
		return this;
	}
	
	public int getLogicWorkerThreadNum() {
		return logicWorkerThreadNum;
	}
	public DFActorManagerConfig setLogicWorkerThreadNum(int logicWorkerThreadNum) {
		this.logicWorkerThreadNum = logicWorkerThreadNum;
		return this;
	}

	public boolean isUseSysLog() {
		return useSysLog;
	}
	public DFActorManagerConfig setUseSysLog(boolean useSysLog) {
		this.useSysLog = useSysLog;
		return this;
	}
	

	public int getTimerTickPerWheel() {
		return timerTickPerWheel;
	}

	public DFActorManagerConfig setTimerTickPerWheel(int timerTickPerWheel) {
		this.timerTickPerWheel = timerTickPerWheel;
		return this;
	}

	public int getLogLevel() {
		return logLevel;
	}
	public DFActorManagerConfig setLogLevel(int logLevel) {
		this.logLevel = logLevel;
		return this;
	}
	
	
}
