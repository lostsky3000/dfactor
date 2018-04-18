package fun.lib.actor.kcp;

public final class KcpConfigInner {

	protected final int sendWnd;
	protected final int recvWnd;
	protected final short fastResendTrig;
	protected final short resendMax;
	protected final int sendTimeout;
	protected final int idleTimeout;
	
	protected KcpConfigInner(int sendWnd, int recvWnd, short fastResendTrig,
			short resendMax, int sendTimeout, int idleTimeout) {
		this.sendWnd = sendWnd;
		this.recvWnd = recvWnd;
		this.fastResendTrig = fastResendTrig;
		this.resendMax = resendMax;
		this.sendTimeout = sendTimeout;
		this.idleTimeout = idleTimeout;
	}
	
	
	//
	public static KcpConfigInner copyConfig(final KcpConfig cfgOri){
		final KcpConfigInner cfg = new KcpConfigInner(cfgOri.getSendWnd(),
				cfgOri.getRecvWnd(), cfgOri.getFastResendTrig(),
				cfgOri.getResendMax(), cfgOri.getSendTimeout(),
				cfgOri.getIdleTimeout());
		return cfg;
	}
}
