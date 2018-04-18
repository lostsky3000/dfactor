package fun.lib.actor.kcp;

public final class KcpConfig {
	
	private int recvWnd = 20;
	private int sendWnd = 20;
	private short fastResendTrig = 1;
	private short resendMax = 30;
	private int sendTimeout = 50;
	private int idleTimeout = 4000;
	
	public int getIdleTimeout(){
		return this.idleTimeout;
	}
	public KcpConfig setIdleTimeout(int idleTimeout){
		this.idleTimeout = idleTimeout;
		return this;
	}
	
	public int getSendTimeout(){
		return this.sendTimeout;
	}
	public KcpConfig setSendTimeout(int sendTimeout){
		this.sendTimeout = sendTimeout;
		return this;
	}
	
	public short getResendMax(){
		return this.resendMax;
	}
	public KcpConfig setResendMax(short resendMax){
		this.resendMax = resendMax;
		return this;
	}
	
	public short getFastResendTrig(){
		return this.fastResendTrig;
	}
	public KcpConfig setFastResendTrig(short fastResendTrig){
		this.fastResendTrig = fastResendTrig;
		return this;
	}
	
	public int getSendWnd(){
		return this.sendWnd;
	}
	public KcpConfig setSendWnd(int sendWnd){
		if(sendWnd > 0){
			this.sendWnd = sendWnd;
		}
		return this;
	}
	//
	public int getRecvWnd(){
		return this.recvWnd;
	}
	public KcpConfig setRecvWnd(int recvWnd){
		if(recvWnd > 0){
			this.recvWnd = recvWnd;
		}
		return this;
	}
}
