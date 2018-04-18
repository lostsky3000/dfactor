package fun.lib.actor.api;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.DFUdpServerCfg;

public interface DFActorNet {
	/**
	 * 建立tcp监听
	 * @param cfg 监听参数
	 * @param requestId 该监听的标识
	 */
	public void doTcpListen(final DFTcpServerCfg cfg, final int requestId);
	
	/**
	 * 关闭指定端口的监听
	 * @param port 要关闭监听的端口号
	 */
	public void doTcpListenClose(int port);
	
	/**
	 * 建立tcp连接
	 * @param cfg 连接参数
	 * @param requestId 该连接的标识
	 * @return
	 */
	public int doTcpConnect(final DFTcpClientCfg cfg, final int requestId);
	//udp
	public void doUdpListen(final DFUdpServerCfg cfg, DFUdpDispatcher listener, final int requestId);
	public void doUdpListenClose(int port);
}
