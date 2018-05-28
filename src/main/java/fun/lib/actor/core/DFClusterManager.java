package fun.lib.actor.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.GeneratedMessageV3;

import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.api.DFSerializable;
import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.define.RpcParamType;
import fun.lib.actor.msg.DMCluster;
import fun.lib.actor.msg.DMCmd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.CharsetUtil;

public final class DFClusterManager {
	private static final DFClusterManager instance = new DFClusterManager();
	private DFClusterManager() {
		// TODO Auto-generated constructor stub
	}
	protected static DFClusterManager get(){
		return instance;
	}
	
	private final ReentrantReadWriteLock _lockNode = new ReentrantReadWriteLock();
	private final ReadLock _lockNodeRead = _lockNode.readLock();
	private final WriteLock _lockNodeWrite = _lockNode.writeLock();
	
	private String _selfNodeName = null;
	private String _selfNodeType = null;
	protected void setSelfNodeName(String nodeName, String nodeType){
		_lockNodeWrite.lock();
		try{
			_selfNodeName = nodeName;
			_selfNodeType = nodeType;
		}finally{
			_lockNodeWrite.unlock();
		}
	}
	
	private final HashMap<String, DFNode> _mapNode = new HashMap<>();
	private final HashMap<String, DFNodeList> _mapNodeType = new HashMap<>();
	private final DFNodeList _nodeListAll = new DFNodeList();
	protected void addNode(String name, String type, DFTcpChannel channel, DFActorLog log){
		DFNode node =  new DFNode(name, type, channel);
		boolean addSucc = false;
		_lockNodeWrite.lock();
		try{
			do {
				if(_mapNode.containsKey(name)){
					break;
				}
				_mapNode.put(name, node);
				//
				DFNodeList nodeLs = _mapNodeType.get(type);
				if(nodeLs == null){
					nodeLs = new DFNodeList();
					_mapNodeType.put(type, nodeLs);
				}
				nodeLs.addNode(node);
				//
				_nodeListAll.addNode(node);
				//
				addSucc = true;
			} while (false);
		}finally{
			_lockNodeWrite.unlock();
		}
		node = null;
		if(addSucc){
			log.info("NewNodeAdd confirmed: nodeType="+type+", nodeName="+name+", from " 
					+channel.getRemoteHost()+":"+channel.getRemotePort());
		}
	}
	protected void removeNode(String name, DFActorLog log){
		DFNode node = null;
		_lockNodeWrite.lock();
		try{
			node = _mapNode.remove(name);
			if(node != null){
				String type = node.type;
				DFNodeList nodeLs = _mapNodeType.get(type);
				if(nodeLs != null){ //in list
					nodeLs.removeNode(node);
					if(nodeLs.nodeNum <= 0){ //no one left, remove from map
						_mapNodeType.remove(type);
					}
				}
				_nodeListAll.removeNode(node);
			}
		}finally{
			_lockNodeWrite.unlock();
		}
		if(node != null){
			log.info("NewNodeRemove: nodeType="+node.type+", nodeName="+node.name+", from " 
					+node.channel.getRemoteHost()+":"+node.channel.getRemotePort());
		}
	}
	
	protected boolean isNodeOnline(String nodeName){
		boolean exist = false;
		_lockNodeRead.lock();
		try{
			exist = _mapNode.containsKey(nodeName);
		}finally{
			_lockNodeRead.unlock();
		}
		return exist;
	}
	protected int getNodeNumByType(String nodeType){
		int nodeNum = 0;
		_lockNodeRead.lock();
		try{
			DFNodeList nodeLs = _mapNodeType.get(nodeType);
			if(nodeLs != null){
				nodeNum = nodeLs.nodeNum;
			}
		}finally{
			_lockNodeRead.unlock();
		}
		return nodeNum;
	}
	protected int getAllNodeNum(){
		int nodeNum = 0;
		_lockNodeRead.lock();
		try{
			nodeNum = _nodeListAll.nodeNum;
		}finally{
			_lockNodeRead.unlock();
		}
		return nodeNum;
	}
	//
	private DFNodeList _getNodeByTypeSafe(String nodeType){
		DFNodeList nodeLs = null;
		_lockNodeRead.lock();
		try{
			DFNodeList tmpLs = _mapNodeType.get(nodeType);
			if(tmpLs != null){
				nodeLs = tmpLs.copy();
			}
		}finally{
			_lockNodeRead.unlock();
		}
		return nodeLs;
	}
	private DFNodeList _getAllNodeSafe(){
		DFNodeList nodeLs = null;
		_lockNodeRead.lock();
		try{
			nodeLs = _nodeListAll.copy();
		}finally{
			_lockNodeRead.unlock();
		}
		return nodeLs;
	}
	protected int broadcast(String srcActor, String dstNodeType, String dstActor, int userCmd, DFSerializable userData){
		int failedNum = 0;
		DFNodeList nodeLs = null;
		if(dstNodeType == null){ //all node
			nodeLs = _getAllNodeSafe();
		}else{  //by type
			nodeLs = _getNodeByTypeSafe(dstNodeType);
		}
		if(nodeLs != null){
			ByteBuf bufUser = null;
			String clzName = null;
			if(userData != null){
				bufUser = PooledByteBufAllocator.DEFAULT.ioBuffer(userData.getSerializedSize());
				userData.onSerialize(bufUser);
				clzName = userData.getClass().getName();
			}
			//
			DFNode curNode = null;
			Iterator<DFNode> it = nodeLs.lsNode.iterator();
			int nodeCount = 0;
			while(it.hasNext()){
				curNode = it.next();
				if(_sendToNodeByteBuf(srcActor, curNode.name, dstActor, null, 0, 
						userCmd, bufUser, RpcParamType.CUSTOM, clzName) != 0){
					++failedNum;
				}
				if(++nodeCount < nodeLs.nodeNum) bufUser = bufUser.copy();
				else bufUser = null; //last one
			}
			if(bufUser!=null){
				bufUser.release(); bufUser = null;
			}
		}
		return failedNum;
	}
	protected int broadcast(String srcActor, String dstNodeType, String dstActor, int userCmd, String userData){
		int failedNum = 0;
		DFNodeList nodeLs = null;
		if(dstNodeType == null){ //all node
			nodeLs = _getAllNodeSafe();
		}else{  //by type
			nodeLs = _getNodeByTypeSafe(dstNodeType);
		}
		if(nodeLs != null){
			byte[] bufUser = null;
			if(userData != null){
				bufUser = userData.getBytes(CharsetUtil.UTF_8);
			}
			//
			DFNode curNode = null;
			Iterator<DFNode> it = nodeLs.lsNode.iterator();
			while(it.hasNext()){
				curNode = it.next();
				if(_sendToNode(srcActor, curNode.name, dstActor, null, 0, userCmd, bufUser, RpcParamType.STRING) != 0){
					++failedNum;
				}
			}
		}
		return failedNum;
	}
	protected int broadcast(String srcActor, String dstNodeType, String dstActor, int userCmd, JSONObject userData){
		int failedNum = 0;
		DFNodeList nodeLs = null;
		if(dstNodeType == null){ //all node
			nodeLs = _getAllNodeSafe();
		}else{  //by type
			nodeLs = _getNodeByTypeSafe(dstNodeType);
		}
		if(nodeLs != null){
			byte[] bufUser = null;
			if(userData != null){
				bufUser = userData.toJSONString().getBytes(CharsetUtil.UTF_8);
			}
			//
			DFNode curNode = null;
			Iterator<DFNode> it = nodeLs.lsNode.iterator();
			while(it.hasNext()){
				curNode = it.next();
				if(_sendToNode(srcActor, curNode.name, dstActor, null, 0, userCmd, bufUser, RpcParamType.JSON) != 0){
					++failedNum;
				}
			}
		}
		return failedNum;
	}
	protected int broadcast(String srcActor, String dstNodeType, String dstActor, int userCmd, byte[] userData){
		int failedNum = 0;
		DFNodeList nodeLs = null;
		if(dstNodeType == null){ //all node
			nodeLs = _getAllNodeSafe();
		}else{  //by type
			nodeLs = _getNodeByTypeSafe(dstNodeType);
		}
		if(nodeLs != null){
			DFNode curNode = null;
			Iterator<DFNode> it = nodeLs.lsNode.iterator();
			while(it.hasNext()){
				curNode = it.next();
				if(_sendToNode(srcActor, curNode.name, dstActor, null, 0, userCmd, userData, RpcParamType.BYTE_ARR) != 0){
					++failedNum;
				}
			}
		}
		return failedNum;
	}
	protected int broadcast(String srcActor, String dstNodeType, String dstActor, int userCmd, ByteBuf userData){
		int failedNum = 0;
		DFNodeList nodeLs = null;
		if(dstNodeType == null){ //all node
			nodeLs = _getAllNodeSafe();
		}else{  //by type
			nodeLs = _getNodeByTypeSafe(dstNodeType);
		}
		if(nodeLs != null){
			DFNode curNode = null;
			Iterator<DFNode> it = nodeLs.lsNode.iterator();
			if(userData != null){  //has payload
				int nodeCount = 0;
				while(it.hasNext()){
					curNode = it.next();
					if(_sendToNodeByteBuf(srcActor, curNode.name, dstActor, null, 0, userCmd, userData, RpcParamType.BYTE_BUF, null) != 0){
						++failedNum;
					}
					if(++nodeCount < nodeLs.nodeNum) userData = userData.copy();
					else userData = null;  //last one
				}
			}else{  //no payload
				while(it.hasNext()){
					curNode = it.next();
					if(_sendToNodeByteBuf(srcActor, curNode.name, dstActor, null, 0, userCmd, null, RpcParamType.BYTE_BUF, null) != 0){
						++failedNum;
					}
				}
			}
		}
		if(userData != null){
			userData.release();
		}
		return failedNum;
	}
	//
	protected int sendToNode(String srcActor, String dstNode, String dstActor, String dstMethod,
			int sessionId, int userCmd, DFSerializable userData){
		ByteBuf bufUser = null;
		String clzName = null;
		if(userData != null){
			bufUser = PooledByteBufAllocator.DEFAULT.ioBuffer(userData.getSerializedSize());
			userData.onSerialize(bufUser);
			clzName = userData.getClass().getName();
		}
		return _sendToNodeByteBuf(srcActor, dstNode, dstActor, dstMethod, sessionId, userCmd, bufUser, RpcParamType.CUSTOM, clzName);
	}
	protected int sendToNode(String srcActor, String dstNode, String dstActor, String dstMethod,
			int sessionId, int userCmd, JSONObject userData){
		byte[] bufUser = null;
		if(userData != null){
			bufUser = userData.toJSONString().getBytes(CharsetUtil.UTF_8);
		}
		return _sendToNode(srcActor, dstNode, dstActor, dstMethod, sessionId, userCmd, bufUser, RpcParamType.JSON);
	}
	protected int sendToNode(String srcActor, String dstNode, String dstActor, String dstMethod,
			int sessionId, int userCmd, String userData){
		if(userData != null){
			byte[] buf = userData.getBytes(CharsetUtil.UTF_8);
			return _sendToNode(srcActor, dstNode, dstActor, dstMethod, sessionId, userCmd, buf, RpcParamType.STRING);
		}else{
			return _sendToNode(srcActor, dstNode, dstActor, dstMethod, sessionId, userCmd, null, RpcParamType.STRING);
		}
	}
	protected int sendToNode(String srcActor, String dstNode, String dstActor, String dstMethod, 
				int sessionId, int userCmd, byte[] userData){
		return _sendToNode(srcActor, dstNode, dstActor, dstMethod, sessionId, userCmd, userData, RpcParamType.BYTE_ARR);
	}
	protected int sendToNode(String srcActor, String dstNode, String dstActor, String dstMethod,
				int sessionId, int userCmd, ByteBuf userData){
		return _sendToNodeByteBuf(srcActor, dstNode, dstActor, dstMethod, sessionId, userCmd, userData, RpcParamType.BYTE_BUF, null);
	}
	
	private int _sendToNode(String srcActor, String dstNode, String dstActor, String dstMethod, 
				int sessionId, int userCmd, byte[] userData, int userDataType){
		DFNode node = null;
		_lockNodeRead.lock();
		try{
			node = _mapNode.get(dstNode);
		}finally{
			_lockNodeRead.unlock();
		}
		if(node != null){
			//
			DMCluster.UserMsgHead.Builder bd = DMCluster.UserMsgHead.newBuilder();
			bd.setSrcNode(_selfNodeName).setSrcType(_selfNodeType)
			.setSessionId(sessionId)
			.setSrcActor(srcActor).setDstActor(dstActor);
			if(dstMethod != null){
				bd.setDstMethod(dstMethod);
			}
			byte[] bufHead = bd.build().toByteArray();
			int headLen = bufHead.length;
			int dataLen = 0;
			if(userData != null){
				dataLen = userData.length;
			}
			//msgLen(2) + cmd(2) + headLen(2) + head(N) + userCmd(4) + userDataType(1) +  userData(N)
			int msgLen = 2 + 2 + headLen + 4 + 1 + dataLen;
			ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(2 + msgLen);  
			buf.writeShort(msgLen);
			buf.writeShort(DMCmd.UserMsg);
			buf.writeShort(headLen);
			buf.writeBytes(bufHead);
			buf.writeInt(userCmd);
			buf.writeByte(userDataType);
			if(userData != null){
				buf.writeBytes(userData);
			}
			node.channel.write(buf);
			return 0;
		}
		return 1;
	}
	private int _sendToNodeByteBuf(String srcActor, String dstNode, String dstActor, String dstMethod, 
				int sessionId, int userCmd, ByteBuf userData, int userDataType, String customClassName){
		DFNode node = null;
		_lockNodeRead.lock();
		try{
			node = _mapNode.get(dstNode);
		}finally{
			_lockNodeRead.unlock();
		}
		if(node != null){
			//
			DMCluster.UserMsgHead.Builder bd = DMCluster.UserMsgHead.newBuilder();
			bd.setSrcNode(_selfNodeName).setSrcType(_selfNodeType)
			.setSessionId(sessionId)
			.setSrcActor(srcActor).setDstActor(dstActor);
			if(dstMethod != null){
				bd.setDstMethod(dstMethod);
			}
			byte[] bufHead = bd.build().toByteArray();
			int headLen = bufHead.length;
			int dataLen = 0;
			byte[] bufClzName = null;
			int clzLen = 0;
			if(userData != null){
				dataLen = userData.readableBytes();
				if(RpcParamType.CUSTOM==userDataType && customClassName != null){ //custom class
					//nameLen(2) + name(N)
					bufClzName = customClassName.getBytes(CharsetUtil.UTF_8);
					clzLen = bufClzName.length;
					dataLen += 2 + clzLen;
				}
			}
			//msgLen(2) + cmd(2) + headLen(2) + head(N) + userCmd(4) + userDataType(1) +  userData(N)
			int msgLen = 2 + 2 + headLen + 4 + 1 + dataLen;
			ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(2 + msgLen);  
			buf.writeShort(msgLen);
			buf.writeShort(DMCmd.UserMsg);
			buf.writeShort(headLen);
			buf.writeBytes(bufHead);
			buf.writeInt(userCmd);
			buf.writeByte(userDataType);
			if(userData != null){
				if(bufClzName != null && clzLen > 0){ //custom userClass
					buf.writeShort(clzLen);
					buf.writeBytes(bufClzName);
				}
				buf.writeBytes(userData);
			}
			node.channel.write(buf);
			//
			if(userData != null){
				userData.release();
			}
			return 0;
		}
		if(userData != null){
			userData.release();
		}
		return 1;
	}
	
	protected int sendToNodeInternal(String srcActor, String dstNode, String dstActor, int sessionId, int cmd){
		DFNode node = null;
		_lockNodeRead.lock();
		try{
			node = _mapNode.get(dstNode);
		}finally{
			_lockNodeRead.unlock();
		}
		if(node != null){
			//
			DMCluster.UserMsgHead.Builder bd = DMCluster.UserMsgHead.newBuilder();
			bd.setSrcNode(_selfNodeName).setSrcType(_selfNodeType)
			.setSessionId(sessionId)
			.setSrcActor(srcActor).setDstActor(dstActor);
			byte[] bufHead = bd.build().toByteArray();
			int headLen = bufHead.length;
			int dataLen = 0;
			
			//msgLen(2) + cmd(2) + headLen(2) + head(N) + userCmd(4) + userDataType(1) +  userData(N)
			int msgLen = 2 + 2 + headLen + 4 + 1 + dataLen;
			ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(2 + msgLen);  
			buf.writeShort(msgLen);
			buf.writeShort(cmd);
			buf.writeShort(headLen);
			buf.writeBytes(bufHead);
			buf.writeInt(0);
			buf.writeByte(0);
			node.channel.write(buf);
			//
			return 0;
		}
		return 1;
	}
	
	private class DFNodeList{
		private LinkedList<DFNode> lsNode = null;
		private int nodeNum = 0;
		private void addNode(DFNode node){
			if(lsNode == null){
				lsNode = new LinkedList<>();
			}
			lsNode.offer(node);
			++nodeNum;
		}
		private void removeNode(DFNode node){
			if(lsNode != null){
				if(lsNode.remove(node)){ //contains
					--nodeNum;
				}
				if(nodeNum <= 0){
					lsNode.clear(); lsNode = null;
				}
			}
		}
		private DFNodeList copy(){
			DFNodeList nodeList = new DFNodeList();
			nodeList.nodeNum = this.nodeNum;
			if(lsNode != null){
				nodeList.lsNode = new LinkedList<>();
				nodeList.lsNode.addAll(lsNode);
			}
			return nodeList;
		}
	}
	
	private class DFNode{
		private final String name;
		private final String type;
		private final DFTcpChannel channel;
		private DFNode(String name, String type, DFTcpChannel channel) {
			this.name = name;
			this.type = type;
			this.channel = channel;
		}
	}
}
