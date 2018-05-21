package fun.lib.actor.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.funtag.util.cipher.DFCipherUtil;
import com.funtag.util.net.DFIpUtil;

import fun.lib.actor.api.DFActorUdpDispatcher;
import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.DFUdpChannel;
import fun.lib.actor.api.cb.CbActorReq;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.DFUdpServerCfg;
import fun.lib.actor.po.IPRange;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

public final class DFClusterActor extends DFActor{
	
	private static final int PORT_RANGE = 20;
	private static final int MAX_PORT = 65536;
	
	public DFClusterActor(Integer id, String name, Boolean isBlockActor) {
		super(id, name, isBlockActor);
		// TODO Auto-generated constructor stub
	}
	
	private String _uniqueId = null;
	private String _selfLanIP = null;
	private String[] _arrLanIPSeg = null;
	private int _curTcpPort = 0;
	private int _curUdpPort = 0;
	private LinkedList<InetAddress> _lsLanHost = null;
	
	private DFActorClusterConfig _cfgCluster = null;
	private ActorProp _propEntry = null;
	@Override
	public void onStart(Object param) {
		log.info("onStart, curThread="+Thread.currentThread().getName());
		//
		HashMap<String,Object> mapParam = (HashMap<String, Object>) param;
		_cfgCluster = (DFActorClusterConfig) mapParam.get("cluster");
		_propEntry = (ActorProp) mapParam.get("entry");
		//get machine info
		if(!_checkMachine()){
			_doShutdown();
			return ;
		}
		//start tcp listen
		_curTcpPort = _cfgCluster.getBasePort();
		_tryTcpListen();
	}
	private void _tryTcpListen(){
		DFTcpServerCfg cfg = new DFTcpServerCfg(_curTcpPort, 1, 0)
			.setSoBackLog(1024)
			.setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
		net.doTcpServer(cfg);
	}
	@Override
	public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg) {
		if(isSucc){
			log.info("listen tcp on port "+_curTcpPort+" succ");
			//try udp listen
			int nextPort = _curTcpPort + 1;
			if(nextPort <= _cfgCluster.getBasePort() + PORT_RANGE && nextPort < MAX_PORT){
				_curUdpPort = nextPort;
				_tryUdpListen();
			}else{
				log.error("listen udp on port "+nextPort+" failed");
				_doShutdown();
			}
		}else{
			int nextPort = _curTcpPort + 1;
			log.error("listen tcp on port "+_curTcpPort+" failed, "+errMsg);
			if(nextPort <= _cfgCluster.getBasePort() + PORT_RANGE && nextPort < MAX_PORT){ //continue
				log.info("retry listen tcp on port "+nextPort);
				_curTcpPort = nextPort;
				_tryTcpListen();
			}else{
				log.error("listen tcp failed, curPort = "+_curTcpPort);
				_doShutdown();
			}
		}
	}
	
	private void _tryUdpListen(){
		DFUdpServerCfg cfg = new DFUdpServerCfg(_curUdpPort, 1, false);
		net.doUdpServer(cfg, new DFActorUdpDispatcher() {
			@Override
			public int onQueryMsgActorId(Object msg) {
				return id;
			}
		}, 10001);
	}
	@Override
	public void onUdpServerListenResult(int requestId, boolean isSucc, String errMsg, DFUdpChannel channel) {
		if(requestId == 10001){
			if(isSucc){  //listen udp succ
				log.info("listen udp on port "+_curUdpPort+" succ");
				//start broadcast
				_tryBroadcast();
			}else{ //listen udp failed
				int nextPort = _curUdpPort + 1;
				log.error("listen udp on port "+_curUdpPort+" failed, "+errMsg);
				if(nextPort <= _cfgCluster.getBasePort() + PORT_RANGE && nextPort < MAX_PORT){ //continue
					log.info("retry listen udp on port "+nextPort);
					_curUdpPort = nextPort;
					_tryUdpListen();
				}else{
					log.error("listen udp failed, curPort = "+_curUdpPort);
					_doShutdown();
				}
			}
		}
	}
	@Override
	public int onUdpServerRecvMsg(int requestId, DFUdpChannel channel, DatagramPacket pack) {
		log.info("recv udp msg: len="+pack.content().readableBytes()+", sender="+pack.sender());
		return 0;
	}
	@Override
	public void onTcpConnOpen(int requestId, DFTcpChannel channel) {
		
	}
	@Override
	public void onTcpConnClose(int requestId, DFTcpChannel channel) {
		
	}
	@Override
	public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	//
	private void _tryBroadcast(){
		boolean bRet = false;
		do{
			log.info("try to search lan host...");
			LinkedList<String> lsSpecifyIp = _cfgCluster.getSpecifyIPList();
			IPRange ipRange = _cfgCluster.getIPRange();
			_lsLanHost = new LinkedList<>();
			if(lsSpecifyIp != null && !lsSpecifyIp.isEmpty()){ //use specify ip list
				log.info("use specify ipList for search");
				Iterator<String> it = lsSpecifyIp.iterator();
				while(it.hasNext()){
					try {
						String host = it.next();
						if(host.equals(_selfLanIP)){ //self
							continue;
						}
						_lsLanHost.add(InetAddress.getByName(host));
					} catch (UnknownHostException e) {
						e.printStackTrace();
						break;
					}
				}
			}else if(ipRange != null){ //use ip range
				log.info("use ipRange for search");
				long nBegin = DFIpUtil.ipToNumber(ipRange.ipBegin);
				long nEnd = DFIpUtil.ipToNumber(ipRange.ipEnd);
				for(long i=nBegin; i<=nEnd; ++i){
					String host = DFIpUtil.numberToIp(i);
					if(host.equals(_selfLanIP)){ //self
						continue;
					}
					try {
						_lsLanHost.add(InetAddress.getByName(host));
					} catch (UnknownHostException e) {
						e.printStackTrace();
						break;
					}
				}
			}else{ //broadcast to all host in lan
				log.info("search all lan host");
				String ipPfx = _arrLanIPSeg[0]+"."+_arrLanIPSeg[1]+"."+_arrLanIPSeg[2]+".";
				for(int i=1; i<=254; ++i){
					String host = ipPfx + i;
					try {
						if(host.equals(_selfLanIP)){ //self
							continue;
						}
						_lsLanHost.add(InetAddress.getByName(host));
					} catch (UnknownHostException e) {
						e.printStackTrace();
						break;
					}
				}
			}
			if(_lsLanHost.isEmpty()){
				log.error("get lanhost failed");
				break;
			}
			bRet = true;
		}while(false);
		if(bRet){
			//broadcast
			_doBroadcast();
		}else{
			log.error("try to broadcast failed");
			_doShutdown();
		}
	}
	private void _doBroadcast(){
		log.info("start broadcast to lanHost, hostNum="+_lsLanHost.size());
		boolean pingTest = _cfgCluster.isPingTest();
		Iterator<InetAddress> it = _lsLanHost.iterator();
		while(it.hasNext()){
			InetAddress addr = it.next();
			if(pingTest){
				try {
					if(!addr.isReachable(100)){
						log.warn("ping unreachable, host="+addr);
						continue;
					}
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
			//
			int maxPort = Math.min(MAX_PORT, _cfgCluster.getBasePort()+PORT_RANGE);
			for(int i=_cfgCluster.getBasePort(); i <= maxPort; ++i){
				_doSendSelfBroadcast(new InetSocketAddress(addr, i));
			}
		}
	}
	
	private void _doSendSelfBroadcast(InetSocketAddress addr){
		log.debug("send selfBroadcast to "+addr);
		ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(20);
		buf.writeCharSequence("heheda", CharsetUtil.UTF_8);
		net.doUdpSend(buf, addr);
	}
	
	private boolean _checkMachine(){
		try {
			_uniqueId = DFCipherUtil.getUUIDDigest();
			log.info("uniqueId = "+_uniqueId);
			//
			Enumeration<NetworkInterface> emNet = NetworkInterface.getNetworkInterfaces();
			while(emNet.hasMoreElements()){
				NetworkInterface net = emNet.nextElement();
				if(!net.isLoopback() && !net.isVirtual()){
					Enumeration<InetAddress> emAddr = net.getInetAddresses();
					while(emAddr.hasMoreElements()){
						InetAddress addr = emAddr.nextElement();
						String ip = addr.getHostAddress();
						boolean isIpv4 = DFIpUtil.isIPv4(ip);
						if(!isIpv4){ //not IPv4
							continue ;
						}
						if(DFIpUtil.isLanIP(ip)){ //bingo
							_selfLanIP = ip;
							_arrLanIPSeg = _selfLanIP.split("\\.");
							log.info("find local lanIP: "+ip);
							return true;
						}
					}
				}
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		log.error("no valid lanIP detected");
		return false;
	}
	
	private void _doShutdown(){
		DFActorManager.get().shutdown();
	}
	
	
}
