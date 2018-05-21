package fun.lib.actor.api;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import fun.lib.actor.api.cb.CbHttpClient;
import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.DFUdpServerCfg;
import io.netty.buffer.ByteBuf;

public interface DFActorNet {
	/**
	 * 建立tcp监听
	 * @param cfg 监听参数
	 */
	public void doTcpServer(DFTcpServerCfg cfg);
	/**
	 * 建立tcp监听
	 * @param cfg 监听参数
	 * @param dispatcher 网络消息及事件分发器
	 */
	public void doTcpServer(DFTcpServerCfg cfg, Object dispatcher);
	/**
	 * 建立tcp监听
	 * @param port 监听端口号
	 */
	public void doTcpServer(int port);
	/**
	 * 建立tcp监听
	 * @param port 监听端口号
	 * @param dispatcher 网络消息及事件分发器
	 */
	public void doTcpServer(int port, Object dispatcher);
	/**
	 * 建立tcp监听
	 * @param port 监听端口号
	 * @param protocol 协议类型，如DFActorDefine.TCP_DECODE_HTTP
	 */
	public void doTcpServer(int port, int protocol);
	
	/**
	 * 关闭指定端口的监听
	 * @param port 要关闭监听的端口号
	 */
	public void doTcpServerClose(int port);
	
	/**
	 * 启动http监听
	 * @param port 端口号
	 * @param handler 处理器
	 */
	public void doHttpServer(int port, CbHttpServer handler);
	/**
	 * 启动http监听
	 * @param cfg 监听参数
	 * @param handler 处理器
	 */
	public void doHttpServer(DFTcpServerCfg cfg, CbHttpServer handler);
	/**
	 * 启动http监听
	 * @param port 端口号
	 * @param handler 处理器
	 * @param dispatcher 消息分发器
	 */
	public void doHttpServer(int port, CbHttpServer handler, DFHttpDispatcher dispatcher);
	/**
	 * 启动http监听
	 * @param cfg 监听参数
	 * @param handler 处理器
	 * @param dispatcher 消息分发器
	 */
	public void doHttpServer(DFTcpServerCfg cfg, CbHttpServer handler, DFHttpDispatcher dispatcher);
	
	
	public void doHttpClient(DFTcpClientCfg cfg, CbHttpClient handler);
	
	/**
	 * 建立tcp连接
	 * @param cfg 连接参数
	 * @param requestId 该连接的标识
	 * @return
	 */
	public int doTcpConnect(final DFTcpClientCfg cfg, final int requestId);
	/**
	 * 建立tcp连接
	 * @param cfg 连接参数
	 * @param requestId 该连接的标识
	 * @param notify 网络消息及事件分发器
	 * @return
	 */
	public int doTcpConnect(final DFTcpClientCfg cfg, final int requestId, final DFActorTcpDispatcher dispatcher);
	//udp
	public void doUdpServer(final DFUdpServerCfg cfg, DFActorUdpDispatcher listener, final int requestId);
	public void doUdpServerClose(int port);
	
	public int doUdpSend(ByteBuf buf, InetSocketAddress addr);
}
