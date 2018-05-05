package fun.lib.actor.deprecated;

import java.nio.charset.Charset;
import java.util.ArrayList;

import fun.lib.actor.api.DFUdpChannel;
import fun.lib.actor.api.cb.CbMsgReq;
import fun.lib.actor.api.DFActorUdpDispatcher;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFActorManager;
import fun.lib.actor.core.DFActorManagerConfig;
import fun.lib.actor.define.DFActorErrorCode;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorEvent;
import fun.lib.actor.po.DFUdpServerCfg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.socket.DatagramPacket;

public final class ActorUdpTest extends DFActor implements DFActorUdpDispatcher{

	protected ActorUdpTest(Integer id, String name, Boolean isIoActor) {
		super(id, name, isIoActor);
		// TODO Auto-generated constructor stub
	}

	private DFUdpChannel channel = null;
	@Override
	public int onMessage(int srcId, int cmd, Object payload, CbMsgReq cb) {
		
		return DFActorDefine.MSG_AUTO_RELEASE;
	}

	private final int listenPort = 13500;
	@Override
	public void onStart(Object param) {
		log.debug("onStart");	
		
		_initKcpListen();
		
		sys.timeout(100, 1);
	}
	
	private void _initKcpListen(){
		final DFUdpServerCfg cfg = new DFUdpServerCfg(listenPort, 1, false);
		net.doUdpServer(cfg, this, 1);
	}
	
	@Override
	public void onTimeout(int requestId) {
		
//		doUdpListenClose(listenPort);
	}

	@Override
	public int onQueryMsgActorId(Object msg) {
		return id;
	}
	
	
	public static void main(String[] args) {
		//
		final DFActorManager mgr = DFActorManager.get();
		DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setLogicWorkerThreadNum(4)
				.setBlockWorkerThreadNum(0);
		
		mgr.start(cfg, ActorProp.newProp()
				.name("actor_udp_test")
				.classz(ActorUdpTest.class)
				.param(1000+""));
		
	}
}
