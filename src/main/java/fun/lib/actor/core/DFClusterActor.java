package fun.lib.actor.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import com.alibaba.fastjson.JSONObject;
import com.funtag.util.cipher.DFCipherUtil;
import com.funtag.util.net.DFIpUtil;
import com.funtag.util.system.DFSysUtil;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFActorUdpDispatcher;
import fun.lib.actor.api.DFSerializable;
import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.DFUdpChannel;
import fun.lib.actor.api.cb.CbActorReq;
import fun.lib.actor.api.cb.CbNode;
import fun.lib.actor.api.cb.CbTimeout;
import fun.lib.actor.define.RpcError;
import fun.lib.actor.define.RpcParamType;
import fun.lib.actor.msg.DMCluster;
import fun.lib.actor.msg.DMCluster.AskOtherConn;
import fun.lib.actor.msg.DMCluster.NewNodeAsk;
import fun.lib.actor.msg.DMCluster.NewNodeLogin;
import fun.lib.actor.msg.DMCluster.NewNodeRsp;
import fun.lib.actor.msg.DMCluster.NewNodeSucc;
import fun.lib.actor.msg.DMCmd;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFNode;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.DFUdpServerCfg;
import fun.lib.actor.po.IPRange;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

public final class DFClusterActor extends DFActor implements DFActorTcpDispatcher{
	
	private static final int PORT_RANGE = 20;
	private static final int MAX_PORT = 65536;
	
	public DFClusterActor(Integer id, String name, Boolean isBlockActor) {
		super(id, name, isBlockActor);
		// TODO Auto-generated constructor stub
	}
	
	private String _uniqueId = null;
	private long _tmStart = 0;
	private String _selfLanIP = null;
	private String _selfNodeName = null;
	private boolean _hasInCluster = false;
	private String[] _arrLanIPSeg = null;
	private int _curTcpPort = 0;
	private int _curUdpPort = 0;
	private InetSocketAddress _addrSelfUdp = null;
	private String _idAddrSelf = null;
	//
	private boolean _askNegative = false;
	//
	private final HashSet<String> _setLanHost = new HashSet<>();
	
	private DFActorClusterConfig _cfgCluster = null;
	private ActorProp _propEntry = null;
	private DFActorManager _mgrActor = null;
	@Override
	public void onStart(Object param) {
		log.info("onStart, curThread="+Thread.currentThread().getName());
		_mgrActor = DFActorManager.get();
		//
		HashMap<String,Object> mapParam = (HashMap<String, Object>) param;
		_cfgCluster = (DFActorClusterConfig) mapParam.get("cluster");
		_propEntry = (ActorProp) mapParam.get("entry");
		//get machine info
		if(!_checkMachine()){
			_doShutdown();
			return ;
		}
		
		if(_cfgCluster.getNodeName() == null){ 
			_selfNodeName = _cfgCluster.getNodeType()+"_"+_uniqueId;
		}else{
			_selfNodeName = _cfgCluster.getNodeName();
		}
		DFClusterManager.get().setSelfNodeName(_selfNodeName, _cfgCluster.getNodeType());
		log.info("curNodeName = "+_selfNodeName);
		//
		_tmStart = System.currentTimeMillis();
		//start tcp listen
		_curTcpPort = _cfgCluster.getBasePort();
		_tryTcpListen();
	}
	private void _tryTcpListen(){
		
		DFTcpServerCfg cfg = new DFTcpServerCfg(_curTcpPort, DFActorManager.get().getClusterIoGroup())
			.setSoBackLog(1024)
			.setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
		net.doTcpServer(cfg, this);
	}
	@Override
	public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg) {
		if(isSucc){
			log.info("listen tcp on port "+_curTcpPort+" succ");
			_idAddrSelf = _selfLanIP+":"+_curTcpPort;
			//try udp listen
			int nextPort = _cfgCluster.getBasePort() + 1;
			if(nextPort <= _cfgCluster.getBasePort() + PORT_RANGE && nextPort < MAX_PORT){
				_curUdpPort = nextPort;
				_tryUdpListen();
			}else{
				log.error("listen udp on port "+nextPort+" failed");
				_doShutdown();
			}
		}else{
			int nextPort = _curTcpPort + 2;
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
		DFUdpServerCfg cfg = new DFUdpServerCfg(_curUdpPort, false, DFActorManager.get().getClusterIoGroup());
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
				_addrSelfUdp = new InetSocketAddress(_selfLanIP, _curUdpPort);
				//start broadcast
				_tryBroadcast();
			}else{ //listen udp failed
				int nextPort = _curUdpPort + 2;
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
		ByteBuf buf = pack.content();
		InetSocketAddress addrSender = pack.sender();
		String host = addrSender.getAddress().getHostAddress();
		int cmd = buf.readShort();
		do {
			if(_askNegative){
				break;
			}
			if(cmd == DMCmd.NewNodeAsk){  //new node req
				log.verb("udp recv newNodeReq, sender="+addrSender);
				DMCluster.NewNodeAsk msg = (NewNodeAsk) _parseMsg(cmd, buf);
				if(msg == null) break; //parse failed
				if(!_checkSign(msg.getSign(), msg.getSalt())) break;  //check sign failed
				if(!_checkClusterName(msg.getClusterName())) break;  //cluster name not match
				if(host.equals(_selfLanIP) && msg.getPort() == _curTcpPort) break; //self
				_checkSelfNodeName(msg, host);
			}else if(cmd == DMCmd.NewNodeRsp){
				log.verb("udp recv newNodeRsp, sender="+addrSender);
				if(!_hasInCluster){
					DMCluster.NewNodeRsp msg = (NewNodeRsp) _parseMsg(cmd, buf);
					if(!_checkSign(msg.getSign(), msg.getSalt())) break;  //check sign failed
					if(!_checkClusterName(msg.getClusterName())) break;  //cluster name not match
					if(msg.getResult() != 0 && msg.getUniqueId().equals(_uniqueId)){ //
						_askNegative = true;
						log.error("recv newNodeRsp, negative!");
						_doShutdown();
					}
				}
			}else if(DMCmd.NewNodeSucc == cmd){
				log.verb("udp recv newNodeSucc, sender="+addrSender);
				if(!_hasInCluster) break;
				DMCluster.NewNodeSucc msg = (NewNodeSucc) _parseMsg(cmd, buf);
				if(!_checkSign(msg.getSign(), msg.getSalt())) break;  //check sign failed
				if(!_checkClusterName(msg.getClusterName())) break;  //cluster name not match
				if(host.equals(_selfLanIP) && msg.getPort() == _curTcpPort) break; //self
				_checkNewNodeSuccNofity(msg);
			}else if(DMCmd.AskOtherConn == cmd){
				log.verb("udp recv askOtherConn, sender="+addrSender);
				if(!_hasInCluster) break;
				DMCluster.AskOtherConn msg =  (AskOtherConn) _parseMsg(cmd, buf);
				if(!_checkSign(msg.getSign(), msg.getSalt())) break;  //check sign failed
				if(!_checkClusterName(msg.getClusterName())) break;  //cluster name not match
				if(host.equals(_selfLanIP) && msg.getPort() == _curTcpPort) break; //self
				_doConnOther(msg.getHost(), msg.getPort(), msg.getNodeName());
			}
		} while (false);
		
		return 0;
	}
	@Override
	public void onTcpConnOpen(int requestId, DFTcpChannel channel) {
		NodeInfo node = null;
		if(requestId < 0){ //as client 
			log.debug("connOpen as client");
			node = _mapNodeCli.remove(requestId);
			node.channel = channel;
			node.isServer = true;
			_mapNodeChannel.put(channel.getChannelId(), node);
		}else if(requestId == _curTcpPort){ //as server
			log.debug("connOpen as server");
			node = new NodeInfo();
			node.channel = channel;
			node.isServer = false;
			_mapNodeChannel.put(channel.getChannelId(), node);
		}
		_doSendSelfInfo(node);
	}
	@Override
	public void onTcpConnClose(int requestId, DFTcpChannel channel) {
		NodeInfo node = _mapNodeChannel.remove(channel.getChannelId());
		boolean needReconn = false;
		if(requestId < 0){ //client 
			log.debug("connClose as client");
			node.channel = null;	
			needReconn = true;
		}else if(requestId == _curTcpPort){ //server
			log.debug("connClose as server");
		}
		if(node.nodeName != null){  //??
			_mapNode.remove(node.nodeName);
			if(node.hasAuth){
				DFClusterManager.get().removeNode(node.nodeName, log);
				_checkNodeRemoveCb(node);
			}
			if(needReconn){ //reconn
				_doConnOther(node.host, node.port, node.nodeName);
			}
		}
	}
	@Override
	public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
		ByteBuf buf = (ByteBuf) msg;
		int cmd = buf.readShort();
		NodeInfo node = _mapNodeChannel.get(channel.getChannelId());
//		log.verb("recvTcpMsg, cmd="+cmd);
		switch(cmd){
		case DMCmd.NewNodeLogin:
			_procNewNodeLogin(cmd, buf, node);
			break;
		default:
			break;
		}
		return 0;
	}
	@Override
	public int onQueryMsgActorId(int requestId, int channelId, InetSocketAddress addrRemote, Object msg) {
		ByteBuf buf = (ByteBuf) msg;
		buf.markReaderIndex();
		int cmd = buf.readShort();
		switch(cmd){
		case DMCmd.UserMsg:
			_procUserMsg(cmd, buf);
			break;
		case DMCmd.RpcFail:
			_procRpcCallFail(cmd, buf);
			break;
		default:   
			buf.resetReaderIndex();
			return id;
		}
		return 0;
	}
	private void _procUserMsg(int cmd, ByteBuf buf){
		//headLen(2) + head(N) + userCmd(4) + userDataType(1) +  userData(N)
		int headLen = buf.readShort();
		byte[] bufHead = new byte[headLen];
		buf.readBytes(bufHead);
		DMCluster.UserMsgHead head = null;
		try {
			head = DMCluster.UserMsgHead.parseFrom(bufHead);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return ;
		}
		//
		int userCmd = buf.readInt();
		Object payload = null;
		int userDataType = buf.readByte();
		int leftLen = buf.readableBytes();
		if(leftLen > 0){ //has payload
			if(userDataType == RpcParamType.STRING || userDataType==RpcParamType.JSON){ //string or json
				payload = buf.readCharSequence(leftLen, CharsetUtil.UTF_8);
				if(userDataType == RpcParamType.JSON){ //json
					payload = JSONObject.parse((String) payload);
				}
			}else if(userDataType == RpcParamType.BYTE_BUF){ //ByteBuf
				ByteBuf bufParam = UnpooledByteBufAllocator.DEFAULT.heapBuffer(leftLen);
				bufParam.writeBytes(buf);
				payload = bufParam;
			}else if(userDataType == RpcParamType.CUSTOM){
				int clzLen = buf.readShort();
				try {
					String clzName = (String) buf.readCharSequence(clzLen, CharsetUtil.UTF_8);
					Class<?> clz = Class.forName(clzName);
					DFSerializable obj = (DFSerializable) clz.newInstance();
					obj.onDeserialize(buf);
					payload = obj;
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{  //byteArray
				payload = new byte[leftLen];
				buf.readBytes((byte[])payload);
			}
		}
		//send to actor
		int ret = 0;
		if(head.getSessionId() == 0){  //not call method
			ret = _mgrActor.send(id, head.getDstActor(), 0, DFActorDefine.SUBJECT_CLUSTER, userCmd, payload, true, 
					head.getSrcNode(), head.getSrcActor(), head.getSrcType(), null);
		}else{  //req <-> rsp
			String dstMethod = head.getDstMethod();
			dstMethod = (dstMethod==null||dstMethod.equals(""))?null:dstMethod;
			if(dstMethod == null){   //response
//				ret = _mgrActor.send(id, head.getDstActor(), head.getSessionId(), DFActorDefine.SUBJECT_USER, userCmd, payload, true, 
//						null, null, null, null);
				ret = _mgrActor.send(id, head.getDstActor(), head.getSessionId(), DFActorDefine.SUBJECT_USER, 
						userCmd, payload, true, null, null, null, null, true);
			}else{  //req
				ret = _mgrActor.send(id, head.getDstActor(), head.getSessionId(), DFActorDefine.SUBJECT_RPC, userCmd, payload, true, 
						head.getSrcNode(), head.getSrcActor(), head.getSrcType(), dstMethod);
				if(ret != 0 && dstMethod != null){  //send failed, notify src
					DFClusterManager.get().sendToNodeInternal(this.name, head.getSrcNode(), 
							head.getSrcActor(), head.getSessionId(), DMCmd.RpcFail);
				}
			}
		}
	}
	private void _procRpcCallFail(int cmd, ByteBuf buf){
		//headLen(2) + head(N) + userCmd(4) + userDataType(1) +  userData(N)
		int headLen = buf.readShort();
		byte[] bufHead = new byte[headLen];
		buf.readBytes(bufHead);
		DMCluster.UserMsgHead head = null;
		try {
			head = DMCluster.UserMsgHead.parseFrom(bufHead);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return ;
		}
		_mgrActor.send(id, head.getDstActor(), head.getSessionId(), DFActorDefine.SUBJECT_CB_FAILED, RpcError.REMOTE_FAILED, null, true, null, null);
	}
	
	private void _procNewNodeLogin(int cmd, ByteBuf buf, NodeInfo node){
		DMCluster.NewNodeLogin msg = (NewNodeLogin) _parseMsg(cmd, buf);
		boolean bRet = false;
		do{
			if(!_checkSign(msg.getSign(), msg.getSalt())){ //sign invalid
				log.error("newNodeLogin err, sign invalid! from "+node);
				break;
			}
			if(!_checkClusterName(msg.getClusterName())){
				log.error("newNodeLogin err, clusterName invalid! "+msg.getClusterName()+", from "+node);
				break;
			}
			//
			node.nodeName = msg.getNodeName();
			node.nodeType = msg.getNodeType();
			node.host = msg.getHost();
			node.hasAuth = true;
			//
			DFClusterManager.get().addNode(node.nodeName, node.nodeType, node.channel, log);
			
			_checkNodeAddCb(node);
			
			bRet = true;
		}while(false);
		if(!bRet && node.channel != null){
			node.channel.close();
			node.channel = null;
		}
	}
	
	//
	private void _tryBroadcast(){
		boolean bRet = false;
		do{
			log.info("try to search lan host...");
			LinkedList<String> lsSpecifyIp = _cfgCluster.getSpecifyIPList();
			IPRange ipRange = _cfgCluster.getIPRange();
			if(lsSpecifyIp != null && !lsSpecifyIp.isEmpty()){ //use specify ip list
				log.info("use specify ipList for search");
				Iterator<String> it = lsSpecifyIp.iterator();
				while(it.hasNext()){
					String host = it.next();
					_setLanHost.add(host);
				}
			}else if(ipRange != null){ //use ip range
				log.info("use ipRange for search");
				long nBegin = DFIpUtil.ipToNumber(ipRange.ipBegin);
				long nEnd = DFIpUtil.ipToNumber(ipRange.ipEnd);
				for(long i=nBegin; i<=nEnd; ++i){
					String host = DFIpUtil.numberToIp(i);
					_setLanHost.add(host);
				}
			}else{ //broadcast to all host in lan
				log.info("search all lan host");
				String ipPfx = _arrLanIPSeg[0]+"."+_arrLanIPSeg[1]+"."+_arrLanIPSeg[2]+".";
				for(int i=1; i<=254; ++i){
					String host = ipPfx + i;
					_setLanHost.add(host);
				}
			}
			//add self
			_setLanHost.add(_selfLanIP);
			//
			bRet = true;
		}while(false);
		if(bRet){
			//broadcast
			_doBroadcast(1); // _cfgCluster.isPingTest()?false:true
		}else{
			log.error("try to broadcast failed");
			_doShutdown();
		}
	}
	private void _doBroadcast(int type){
		boolean ignorePingTest = true;
		log.debug("start broadcast to lanHost, hostNum=" + _setLanHost.size());
		if(type == 1){
			++_verifyAskCount;
		}else if(type == 2){
			++_notifySuccCount;
		}
		Iterator<String> it = _setLanHost.iterator();
		while(it.hasNext()){
			String host = it.next();
			InetAddress addr = null;
			try {
				addr = InetAddress.getByName(host);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				continue;
			} 
			if(addr == null){
				continue;
			}
			if(!ignorePingTest){
				try {
					if(!addr.isReachable(100)){
						log.info("ping unreachable, host="+addr);
						continue;
					}
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
			//
			String salt = _createSalt();
			String sign = _createSign(salt);
			int maxPort = Math.min(MAX_PORT, _cfgCluster.getBasePort()+PORT_RANGE);
			for(int i=_cfgCluster.getBasePort() + 1; i <= maxPort; i+=2){
				if(_curUdpPort == i && host.equals(_selfLanIP)){  //self, no broadcast
					log.debug("ignore broadcast to self");
				}else{
					if(type == 1){ //newNode ask
						_doSendSelfBroadcast(new InetSocketAddress(addr, i), salt, sign);
					}else if(type == 2){ //newNode succ notify
						_doSendEnterSuccBroarcast(new InetSocketAddress(addr, i), salt, sign);
					}
				}
			}
		}
		log.debug("broadcast done");
		//
		if(type == 1){
			timer.timeout(2500, 10001);
		}else if(type == 2){
			timer.timeout(2000, 10002);
		}
	}
	
	private int _verifyAskCount = 0;
	private int _notifySuccCount = 0;
	@Override
	public void onTimeout(int requestId) {
		if(10001 == requestId){
			if(!_askNegative && !_hasInCluster){
				if(_verifyAskCount < 3){
					_doBroadcast(1);
				}else{  
					_hasInCluster = true;
					log.info("enter cluster succ!");
					//create user actor
					sys.createActor(_propEntry);
					_doBroadcast(2);
				}
			}
		}else if(10002 == requestId){
			if(_notifySuccCount < 3){
				_doBroadcast(2);
			}else{
				
			}
		}
	}
	
	private void _doSendSelfBroadcast(InetSocketAddress addr, String salt, String sign){
//		log.debug("send selfBroadcast to "+addr);
		DMCluster.NewNodeAsk.Builder bd = DMCluster.NewNodeAsk.newBuilder();
		bd.setSalt(salt).setSign(sign)
			.setHost(_selfLanIP).setPort(_curTcpPort).setUdpPort(_curUdpPort)
			.setClusterName(_cfgCluster.getClusterName())
			.setUniqueId(_uniqueId)
			.setNodeName(_selfNodeName)
			.setNodeType(_cfgCluster.getNodeType())
			.setTmStart(_tmStart).setAskCount(_verifyAskCount);
		//
		_doSendUdp(DMCmd.NewNodeAsk, bd.build().toByteArray(), addr);
	}
	private void _doSendEnterSuccBroarcast(InetSocketAddress addr, String salt, String sign){
		DMCluster.NewNodeSucc.Builder bd = DMCluster.NewNodeSucc.newBuilder();
		bd.setSalt(salt).setSign(sign)
			.setHost(_selfLanIP).setPort(_curTcpPort).setUdpPort(_curUdpPort)
			.setClusterName(_cfgCluster.getClusterName())
			.setUniqueId(_uniqueId)
			.setNodeName(_selfNodeName)
			.setNodeType(_cfgCluster.getNodeType());
		//
		_doSendUdp(DMCmd.NewNodeSucc, bd.build().toByteArray(), addr);
	}
	
	private Object _parseMsg(int cmd, ByteBuf buf){
		try {
			GeneratedMessageV3 m = s_mapMsgParser.get(cmd);
			if(m != null){
				byte[] arrBuf = new byte[buf.readableBytes()];
				buf.readBytes(arrBuf);
				return m.getParserForType().parseFrom(arrBuf);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
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
	private void _checkSelfNodeName(NewNodeAsk msg, String remoteHost){
		String nameReq = msg.getNodeName();
		if(nameReq != null){
			if(nameReq.equals(_selfNodeName)){  //node name duplicated
				boolean reqFailed = false;
				if(_hasInCluster){
					reqFailed = true;
				}else{
					if(_tmStart < msg.getTmStart()){
						reqFailed = true;
					}else if(_tmStart == msg.getTmStart()){  //compare uniqueId
						if(_uniqueId.compareTo(msg.getUniqueId()) < 0){
							reqFailed = true;
						}
					}
				}
				//
				if(reqFailed){ //req node failed
					_doNewNodeRsp(1, "node name duplicated", remoteHost, msg.getUdpPort(), msg.getAskCount(), msg.getUniqueId());
				}else{
					log.error("nodeName duplicated, race failed, remote="+msg.getHost()+":"+msg.getPort());
					_doShutdown();
				}
			}
		}
	}
	
	private void _doNewNodeRsp(int ret, String errMsg, String hostRemote, int udpRemote, int askCount, String uniqueId){
		String salt = _createSalt();
		String sign = _createSign(salt);
		for(int i=0; i<2; ++i){
			DMCluster.NewNodeRsp.Builder bd = DMCluster.NewNodeRsp.newBuilder();
			bd.setClusterName(_cfgCluster.getClusterName())
			.setUniqueId(uniqueId)
			.setResult(ret).setErrMsg(errMsg).setAskCount(askCount)
			.setSalt(salt).setSign(sign);
			//
			_doSendUdp(DMCmd.NewNodeRsp, bd.build().toByteArray(), new InetSocketAddress(hostRemote, udpRemote));
		}
	}
	
	private void _checkNewNodeSuccNofity(NewNodeSucc msg){
//		String idReq = msg.getHost()+":"+msg.getPort();
		String idReq = msg.getNodeName();
		//_idAddrSelf
//		if(idReq.compareTo(_idAddrSelf) < 1){  //conn act
		if(idReq.compareTo(_selfNodeName) < 1){  //conn act
			_doConnOther(msg.getHost(), msg.getPort(), msg.getNodeName());
		}else{ //conn deact, ask other to conn
			DMCluster.AskOtherConn.Builder bd = DMCluster.AskOtherConn.newBuilder();
			String salt = _createSalt();
			bd.setSalt(salt).setSign(_createSign(salt))
			.setClusterName(_cfgCluster.getClusterName())
			.setHost(_selfLanIP).setPort(_curTcpPort)
			.setUniqueId(_uniqueId).setNodeName(_selfNodeName)
			.setNodeType(_cfgCluster.getNodeType());
			//
			_doSendUdp(DMCmd.AskOtherConn, bd.build().toByteArray(), new InetSocketAddress(msg.getHost(), msg.getUdpPort()));
		}
	}
	
	private final HashMap<String, NodeInfo> _mapNode = new HashMap<>();		//nodeName <-> node
	private final HashMap<Integer, NodeInfo> _mapNodeCli = new HashMap<>();   //connReqId <-> node
	private final HashMap<Integer, NodeInfo> _mapNodeChannel = new HashMap<>(); //channelId <-> node
	private void _doConnOther(String host, int port, String nodeName){
		do {
			if(_mapNode.containsKey(nodeName)){
				break;
			}
			NodeInfo node = new NodeInfo(nodeName, host, port, _genConnReqId());
			_mapNode.put(nodeName, node);
			_mapNodeCli.put(node.connReqId, node);
			//
			DFTcpClientCfg cfg = new DFTcpClientCfg(host, port)
					.setConnTimeout(5000).setTcpProtocol(DFActorDefine.TCP_DECODE_LENGTH);
			DFActorManager.get().doTcpConnectViaCluster(cfg, id, this, node.connReqId);
			
		} while (false);
	}
	@Override
	public void onTcpClientConnResult(int requestId, boolean isSucc, String errMsg) {
		final NodeInfo node = _mapNodeCli.get(requestId);
		if(isSucc){
			log.debug("conn succ, node="+node);
		}else{
			log.error("conn failed, node="+node);
			if(node != null){
				_mapNode.remove(node.nodeName);
				_mapNodeCli.remove(requestId);
				//reconn
				log.debug("will reconn "+node.nodeName+": "+node.host+":"+node.port);
				timer.timeout(5000, new CbTimeout() {
					@Override
					public void onTimeout() {
						_doConnOther(node.host, node.port, node.nodeName);
					}
				});
			}
		}
	}
	
	private void _doSendSelfInfo(NodeInfo node){
		DMCluster.NewNodeLogin.Builder bd = DMCluster.NewNodeLogin.newBuilder();
		String salt = _createSalt();
		bd.setSalt(salt).setSign(_createSign(salt))
		.setClusterName(_cfgCluster.getClusterName())
		.setUniqueId(_uniqueId).setHost(_selfLanIP)
		.setNodeName(_selfNodeName).setNodeType(_cfgCluster.getNodeType());
		_doSendTcp(DMCmd.NewNodeLogin, bd.build().toByteArray(), node.channel);
	}
	
	//msg map
	private static final HashMap<Integer, GeneratedMessageV3> s_mapMsgParser = new HashMap<>();
	static{
		s_mapMsgParser.put(DMCmd.NewNodeAsk, DMCluster.NewNodeAsk.getDefaultInstance());
		s_mapMsgParser.put(DMCmd.NewNodeRsp, DMCluster.NewNodeRsp.getDefaultInstance());
		s_mapMsgParser.put(DMCmd.NewNodeSucc, DMCluster.NewNodeSucc.getDefaultInstance());
		s_mapMsgParser.put(DMCmd.AskOtherConn, DMCluster.AskOtherConn.getDefaultInstance());
		s_mapMsgParser.put(DMCmd.NewNodeLogin, DMCluster.NewNodeLogin.getDefaultInstance());
	}	
	
	private static class NodeInfo{
		private String nodeName;
		private String nodeType;
		private String host;
		private int port;
		private int connReqId = 0;
		private DFTcpChannel channel = null;
		private boolean isServer = false;
		private boolean hasAuth = false;
		public NodeInfo(String nodeName, String host, int port, int connReqId) {
			this.nodeName = nodeName;
			this.host = host;
			this.port = port;
			this.connReqId = connReqId;
		}
		public NodeInfo() {
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return nodeName+", "+host+":"+port+", "+connReqId;
		}
	}
	private int _connReqIdCount = 0;
	private int _genConnReqId(){
		if(_connReqIdCount == Integer.MIN_VALUE){
			_connReqIdCount = 0;
		}
		return --_connReqIdCount;
	}
	private void _doSendUdp(int cmd, byte[] buf, InetSocketAddress addr){
		ByteBuf bufOut = PooledByteBufAllocator.DEFAULT.ioBuffer(2 + buf.length);
		bufOut.writeShort(cmd);
		bufOut.writeBytes(buf);
		DFActorManager.get().doUdpSendViaCluster(bufOut, addr);
	}
	private void _doSendTcp(int cmd, byte[]buf, DFTcpChannel channel){
		int dataLen = buf.length;
		ByteBuf bufOut = PooledByteBufAllocator.DEFAULT.ioBuffer(2 + dataLen);
		bufOut.writeShort(cmd);
		bufOut.writeBytes(buf);
		channel.write(bufOut);
	}
	private boolean _checkSign(String signReq, String saltReq){
		if(signReq != null && saltReq != null){
			String signCalc = _createSign(saltReq);
			if(signReq.equals(signCalc)){
				return true;
			}
		}
		return false;
	}
	private String _createSign(String salt){
		return DFCipherUtil.getMD5(_cfgCluster.getSecretKey()+ "_" + salt);
	}
	private String _createSalt(){
		return System.currentTimeMillis()+"_"+_uniqueId;
	}
	private boolean _checkClusterName(String nameReq){
		if(nameReq != null && nameReq.equals(_cfgCluster.getClusterName())){
			return true;
		}
		return false;
	}
	private void _doShutdown(){
		sys.shutdown();
	}
	@Override
	public int onConnActiveUnsafe(int requestId, int channelId, InetSocketAddress addrRemote) {
		// TODO Auto-generated method stub
		return id;
	}
	@Override
	public int onConnInactiveUnsafe(int requestId, int channelId, InetSocketAddress addrRemote) {
		// TODO Auto-generated method stub
		return id;
	}
	//
	@Override
	public int onMessage(int cmd, Object payload, int srcId) {
		if(CMD_REG_NODE_LISTENER == cmd){
			RegNodeReq req = (RegNodeReq) payload;
			if(_setNodeCb.contains(req.cb)){ //cb has used
				return 0;
			}
			_setNodeCb.add(req.cb);
			if(RegNodeReq.NODE_NAME == req.type){  //listen by nodeName
				_addNodeCbByName(req.value, req);
				Iterator<DFNode> it = _mapNodeUser.values().iterator();  //notify exist node
				while(it.hasNext()){
					DFNode n = it.next();
					if(n.name.equals(req.value)){
						_mgrActor.send(id, req.srcId, 0, DFActorDefine.SUBJECT_NODE_EVENT, 0, n, true, null, req.cb, true);
					}
				}
			}else if(RegNodeReq.NODE_TYPE == req.type){ //listen by nodeType
				_addNodeCbByType(req.value, req);
				Iterator<DFNode> it = _mapNodeUser.values().iterator();  //notify exist node
				while(it.hasNext()){
					DFNode n = it.next();
					if(n.type.equals(req.value)){
						_mgrActor.send(id, req.srcId, 0, DFActorDefine.SUBJECT_NODE_EVENT, 0, n, true, null, req.cb, true);
					}
				}
			}else{ //listen all
				_addNodeCbAll(req);
				Iterator<DFNode> it = _mapNodeUser.values().iterator();  //notify exist node
				while(it.hasNext()){
					DFNode n = it.next();
					_mgrActor.send(id, req.srcId, 0, DFActorDefine.SUBJECT_NODE_EVENT, 0, n, true, null, req.cb, true);
				}
			}
		}
		return 0;
	}
	private void _addNodeCbAll(RegNodeReq req){
		_cbLsAll.addCb(req);
	}
	private void _addNodeCbByType(String nodeType, RegNodeReq req){
		NodeCbList cbList = _mapCbType.get(nodeType);
		if(cbList == null){
			cbList = new NodeCbList();
			_mapCbType.put(nodeType, cbList);
		}
		cbList.addCb(req);
	}
	private void _addNodeCbByName(String nodeName, RegNodeReq req){
		NodeCbList cbList = _mapCbName.get(nodeName);
		if(cbList == null){
			cbList = new NodeCbList();
			_mapCbName.put(nodeName, cbList);
		}
		cbList.addCb(req);
	}
	private void _checkNodeAddCb(NodeInfo node){
		DFNode dfNode = new DFNode(node.nodeName, node.nodeType, node.host);
		_mapNodeUser.put(node.nodeName, dfNode);
		//
		NodeCbList cbLs = _mapCbName.get(node.nodeName);
		if(cbLs != null) _nofityNodeChange(false, cbLs, dfNode);	//reg by name
		cbLs = _mapCbType.get(node.nodeType);
		if(cbLs != null) _nofityNodeChange(false, cbLs, dfNode);	//reg by type
		if(!_cbLsAll.lsCb.isEmpty()) _nofityNodeChange(false, _cbLsAll, dfNode);	//all
	}
	private void _checkNodeRemoveCb(NodeInfo node){
		DFNode dfNode = _mapNodeUser.remove(node.nodeName);
		//
		if(dfNode != null){
			NodeCbList cbLs = _mapCbName.get(node.nodeName);
			if(cbLs != null) _nofityNodeChange(true, cbLs, dfNode);	//reg by name
			cbLs = _mapCbType.get(node.nodeType);
			if(cbLs != null) _nofityNodeChange(true, cbLs, dfNode);	//reg by type
			if(!_cbLsAll.lsCb.isEmpty()) _nofityNodeChange(true, _cbLsAll, dfNode);	//all
		}
		
	}
	private void _nofityNodeChange(boolean isRemove, NodeCbList cbLs, DFNode node){
		Iterator<RegNodeReq> it = cbLs.lsCb.iterator();
		while(it.hasNext()){
			RegNodeReq req = it.next();
			_mgrActor.send(id, req.srcId, 0, DFActorDefine.SUBJECT_NODE_EVENT, isRemove?1:0, node, true, null, req.cb, true);
		}
	}
	
	private HashMap<String, DFNode> _mapNodeUser = new HashMap<>();
	private HashSet<CbNode> _setNodeCb = new HashSet<>();
	private HashMap<String, NodeCbList> _mapCbName = new HashMap<>();
	private HashMap<String, NodeCbList> _mapCbType = new HashMap<>();
	private NodeCbList _cbLsAll = new NodeCbList();
	private class NodeCbList{
		private LinkedList<RegNodeReq> lsCb = new LinkedList<>();
		private void addCb(RegNodeReq req){
			lsCb.offer(req);
		}
	}
	
	public static final int CMD_REG_NODE_LISTENER = -1001;
	
	protected static final String NAME = "DFClusterActor";
}




