package fun.lib.actor.core;

import fun.lib.actor.api.DFActorNet;
import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFActorUdpDispatcher;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.api.http.DFHttpServerHandler;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.DFUdpServerCfg;

public final class DFActorNetWrapper  implements DFActorNet{
	
	private final int id;
	private final DFActorManager _mgr;
	
	public DFActorNetWrapper(int id) {
		this.id = id;
		_mgr = DFActorManager.get();
	}
	
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
				.setTcpDecodeType(DFActorDefine.TCP_DECODE_LENGTH);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, port);
	}
	@Override
	public void doTcpServer(int port, int protocol) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpDecodeType(protocol);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, port);
	}
	
	//
	public final void doTcpServerClose(int port){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListenClose(port);
	}
	public final int doTcpConnect(final DFTcpClientCfg cfg, final int requestId){
		return _mgr.doTcpConnect(cfg, id, requestId);
	}
	public final int doTcpConnect(final DFTcpClientCfg cfg, final int requestId, final DFActorTcpDispatcher dispatcher){
		return _mgr.doTcpConnect(cfg, id, dispatcher, requestId);
	}
	//udp
	public final void doUdpServer(final DFUdpServerCfg cfg, DFActorUdpDispatcher listener, final int requestId){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doUdpListen(cfg, id, listener, requestId);
	}
	public final void doUdpServerClose(int port){
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doUdpListenClose(port);
	}
	
	@Override
	public void doHttpServer(int port, DFHttpServerHandler handler) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpDecodeType(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, port);
	}
	@Override
	public void doHttpServer(int port, DFHttpServerHandler handler, DFHttpDispatcher dispatcher) {
		DFTcpServerCfg cfg = new DFTcpServerCfg(port)
				.setTcpDecodeType(DFActorDefine.TCP_DECODE_HTTP)
				.setUserHandler(handler);
		final DFSocketManager mgr = DFSocketManager.get();
		mgr.doTcpListen(cfg, id, dispatcher, port);
	}
}
