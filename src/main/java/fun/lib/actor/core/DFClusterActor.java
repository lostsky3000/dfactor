package fun.lib.actor.core;

import java.io.IOException;
import java.net.InetAddress;
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
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.DFUdpServerCfg;
import fun.lib.actor.po.IPRange;
import io.netty.channel.socket.DatagramPacket;

public final class DFClusterActor extends DFActor{
	
	private static final int PORT_RANGE = 30;
	
	public DFClusterActor(Integer id, String name, Boolean isBlockActor) {
		super(id, name, isBlockActor);
		// TODO Auto-generated constructor stub
	}
	
	private String _uniqueId = null;
	private String _lanIP = null;
	private String[] _arrLanIPSec = null;
	private int _curListenPort = 0;
	private int _curUdpPort = 0;
	
	private DFActorClusterConfig _cfgCluster = null;
	private ActorProp _propEntry = null;
	@Override
	public void onStart(Object param) {
		log.info("onStart");
		//
		HashMap<String,Object> mapParam = (HashMap<String, Object>) param;
		_cfgCluster = (DFActorClusterConfig) mapParam.get("cluster");
		_propEntry = (ActorProp) mapParam.get("entry");
		//get machine info
		_uniqueId = DFCipherUtil.getUUIDDigest();
		log.info("uniqueId = "+_uniqueId);
		if(!_checkMachine()){
			log.error("no valid lanIP detected");
			_doShutdown();
			return ;
		}
		//start tcp listen
		_curListenPort = _cfgCluster.getListenPort();
		_tryDoListen();
		
	}
	private void _tryDoListen(){
		DFTcpServerCfg cfg = new DFTcpServerCfg(_curListenPort, 1, 0)
			.setSoBackLog(1024)
			.setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
		net.doTcpServer(cfg);
	}
	@Override
	public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg) {
		if(isSucc){
			log.info("listen on port "+_curListenPort+" succ");
			//try udp listen
			_curUdpPort = _cfgCluster.getListenPort() + 1;
			_tryDoUdpListen();
		}else{
			int nextPort = _curListenPort + 1;
			log.error("listen on port "+_curListenPort+" failed, "+errMsg);
			if(_curListenPort <= _cfgCluster.getListenPort() + PORT_RANGE && nextPort < 35536){ //continue
				log.info("retry listen on port "+nextPort);
				_curListenPort = nextPort;
				_tryDoListen();
			}else{
				log.error("listen failed, curPort = "+_curListenPort);
				_doShutdown();
			}
		}
	}
	
	private void _tryDoUdpListen(){
		DFUdpServerCfg cfg = new DFUdpServerCfg(_curUdpPort, 1, false);
		net.doUdpServer(cfg, new DFActorUdpDispatcher() {
			@Override
			public int onQueryMsgActorId(Object msg) {
				// TODO Auto-generated method stub
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
				if(_curUdpPort <= _cfgCluster.getListenPort() + PORT_RANGE && nextPort < 35536){ //continue
					log.info("retry listen udp on port "+nextPort);
					_curUdpPort = nextPort;
					_tryDoUdpListen();
				}else{
					log.error("listen udp failed, curPort = "+_curUdpPort);
					_doShutdown();
				}
			}
		}
	}
	@Override
	public int onUdpServerRecvMsg(int requestId, DFUdpChannel channel, DatagramPacket pack) {
		log.info("recv udp msg: len = "+pack.content().readableBytes());
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
	
	private void _tryBroadcast(){
		LinkedList<String> lsWhiteIp = _cfgCluster.getIPWhiteList();
		IPRange ipRange = _cfgCluster.getIPRange();
		if(ipRange == null && lsWhiteIp == null){ //no ip range cfg
			String ipPfx = _arrLanIPSec[0]+"."+_arrLanIPSec[1]+"."+_arrLanIPSec[2];
			String ipBegin = ipPfx+".1";
			String ipEnd = ipPfx+".255";
			ipRange = new IPRange(ipBegin, ipEnd);
			log.info("no ipRange specify, use "+ipBegin+" to "+ipEnd);
		}
		LinkedList<InetAddress> lsAddr = new LinkedList<>();
		if(ipRange != null){
			log.info("start check ip range from "+ipRange.ipBegin+" to "+ipRange.ipEnd);
			long nBegin = DFIpUtil.ipToNumber(ipRange.ipBegin);
			long nEnd = DFIpUtil.ipToNumber(ipRange.ipEnd);
			for(long i=nBegin; i<=nEnd; ++i){
				String host = DFIpUtil.numberToIp(i);
				if(host.equals(_lanIP)){ //self
					continue;
				}
				try {
					InetAddress inetAddr = InetAddress.getByName(host);
					if(inetAddr.isReachable(20)){
						log.info("pingtest, got: "+host);
						lsAddr.offer(inetAddr);
					}else{
						log.warn("pingtest, can't reach: "+host);
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
					continue;
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		if(lsWhiteIp != null){
			log.info("start check ip white list");
			Iterator<String> it = lsWhiteIp.iterator();
			while(it.hasNext()){
				String host = it.next();
				if(host.equals(_lanIP)){ //self
					continue;
				}
				try {
					InetAddress inetAddr = InetAddress.getByName(host);
					if(inetAddr.isReachable(20)){
						log.info("pingtest, got: "+host);
						lsAddr.offer(inetAddr);
					}else{
						log.warn("pingtest, can't reach: "+host);
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
					continue;
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		//broadcast
		_doBroadcast(lsAddr);
	}
	private void _doBroadcast(LinkedList<InetAddress> lsAddr){
		Iterator<InetAddress> it = lsAddr.iterator();
		while(it.hasNext()){
			InetAddress addr = it.next();
			
		}
	}
	
	private boolean _checkMachine(){
		try {
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
							_lanIP = ip;
							_arrLanIPSec = _lanIP.split("\\.");
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
		return false;
	}
	
	private void _doShutdown(){
		DFActorManager.get().shutdown();
	}
}
