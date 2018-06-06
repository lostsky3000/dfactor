package fun.lib.actor.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import fun.lib.actor.api.DFActorNet;
import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFActorUdpDispatcher;
import fun.lib.actor.api.cb.CbHttpClient;
import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.DFUdpServerCfg;
import io.netty.buffer.ByteBuf;

public final class DFActorNetWrap  implements DFActorNet{
	
	private final int id;
	private final DFActorManager _mgr;
	
	public DFActorNetWrap(int id) {
		this.id = id;
		_mgr = DFActorManager.get();
	}
	
	//tcp server
	@Override
	public void tcpSvr(DFTcpServerCfg cfg){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, cfg.port);
	}
	public void tcpSvr(DFTcpServerCfg cfg, Object dispatcher) {
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, dispatcher, cfg.port);
	}
	@Override
	public void tcpSvr(int port) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, port);
	}
	@Override
	public void tcpSvr(int port, Object dispatcher) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, dispatcher, cfg.port);
	}
	@Override
	public void tcpSvr(int port, int protocol) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(protocol);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, cfg.port);
	}
	//http
	@Override
	public void httpSvr(int port, CbHttpServer handler) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, port);
	}
	@Override
	public void httpSvr(int port, CbHttpServer handler, DFHttpDispatcher dispatcher) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, dispatcher, port);
	}	
	@Override
	public void httpSvr(DFTcpServerCfg cfg, CbHttpServer handler) {
		cfg.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, cfg.port);
	}
	@Override
	public void httpSvr(DFTcpServerCfg cfg, CbHttpServer handler, DFHttpDispatcher dispatcher) {
		cfg.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, dispatcher, cfg.port);
	}
	
	//close tcp server
	public final void closeTcpSvr(int port){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListenClose(port);
	}
	//tcp client
	public final int tcpCli(final DFTcpClientCfg cfg, final int requestId){
		return _mgr.doTcpConnect(cfg, id, requestId);
	}
	public final int tcpCli(final DFTcpClientCfg cfg, final int requestId, final DFActorTcpDispatcher dispatcher){
		return _mgr.doTcpConnect(cfg, id, dispatcher, requestId);
	}
	//udp server
	public final void udpSvr(final DFUdpServerCfg cfg, DFActorUdpDispatcher listener, final int requestId){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doUdpListen(cfg, id, listener, requestId);
	}
	public final void closeUdpSvr(int port){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doUdpListenClose(port);
	}
	//http client
	@Override
	public void httpCli(DFTcpClientCfg cfg, CbHttpClient handler) {
		cfg.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
			.setUserHandler(handler);
		_mgr.doTcpConnect(cfg, id, 0);
	}

	@Override
	public int udpCli(ByteBuf buf, InetSocketAddress addr) {
		return _mgr.doUdpSend(buf, addr);
	}

	
}
