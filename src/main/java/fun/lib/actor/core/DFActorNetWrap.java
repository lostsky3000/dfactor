package fun.lib.actor.core;

import fun.lib.actor.api.DFActorNet;
import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFActorUdpDispatcher;
import fun.lib.actor.api.cb.CbHttpClient;
import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.DFUdpServerCfg;

public final class DFActorNetWrap  implements DFActorNet{
	
	private final int id;
	private final DFActorManager _mgr;
	
	public DFActorNetWrap(int id) {
		this.id = id;
		_mgr = DFActorManager.get();
	}
	
	//tcp server
	@Override
	public void doTcpServer(DFTcpServerCfg cfg){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, cfg.port);
	}
	public void doTcpServer(DFTcpServerCfg cfg, Object dispatcher) {
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, dispatcher, cfg.port);
	}
	@Override
	public void doTcpServer(int port) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, port);
	}
	@Override
	public void doTcpServer(int port, Object dispatcher) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, dispatcher, cfg.port);
	}
	@Override
	public void doTcpServer(int port, int protocol) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(protocol);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, cfg.port);
	}
	//http
	@Override
	public void doHttpServer(int port, CbHttpServer handler) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, port);
	}
	@Override
	public void doHttpServer(int port, CbHttpServer handler, DFHttpDispatcher dispatcher) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, dispatcher, port);
	}	
	@Override
	public void doHttpServer(DFTcpServerCfg cfg, CbHttpServer handler) {
		cfg.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, cfg.port);
	}
	@Override
	public void doHttpServer(DFTcpServerCfg cfg, CbHttpServer handler, DFHttpDispatcher dispatcher) {
		cfg.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, dispatcher, cfg.port);
	}
	
	//close tcp server
	public final void doTcpServerClose(int port){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListenClose(port);
	}
	//tcp client
	public final int doTcpConnect(final DFTcpClientCfg cfg, final int requestId){
		return _mgr.doTcpConnect(cfg, id, requestId);
	}
	public final int doTcpConnect(final DFTcpClientCfg cfg, final int requestId, final DFActorTcpDispatcher dispatcher){
		return _mgr.doTcpConnect(cfg, id, dispatcher, requestId);
	}
	//udp server
	public final void doUdpServer(final DFUdpServerCfg cfg, DFActorUdpDispatcher listener, final int requestId){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doUdpListen(cfg, id, listener, requestId);
	}
	public final void doUdpServerClose(int port){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doUdpListenClose(port);
	}
	//http client
	@Override
	public void doHttpClient(DFTcpClientCfg cfg, CbHttpClient handler) {
		cfg.setTcpProtocol(DFActorDefine.TCP_DECODE_HTTP)
			.setUserHandler(handler);
		_mgr.doTcpConnect(cfg, id, 0);
	}

	
}
