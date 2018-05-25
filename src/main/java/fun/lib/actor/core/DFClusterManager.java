package fun.lib.actor.core;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.api.DFTcpChannel;
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
	
	protected int sendToNode(String srcActor, String dstNode, String dstActor, int userCmd, String userData){
		if(userData != null){
			byte[] buf = userData.getBytes(CharsetUtil.UTF_8);
			return _sendToNode(srcActor, dstNode, dstActor, userCmd, buf, 1);
		}else{
			return _sendToNode(srcActor, dstNode, dstActor, userCmd, null, 1);
		}
	}
	protected int sendToNode(String srcActor, String dstNode, String dstActor, int userCmd, byte[] userData){
		return _sendToNode(srcActor, dstNode, dstActor, userCmd, userData, 0);
	}
	protected int sendToNode(String srcActor, String dstNode, String dstActor, int userCmd, ByteBuf userData){
		return _sendToNodeByteBuf(srcActor, dstNode, dstActor, userCmd, userData, 0);
	}
	
	private int _sendToNode(String srcActor, String dstNode, String dstActor, int userCmd, byte[] userData, int userDataType){
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
			.setSrcActor(srcActor).setDstActor(dstActor);
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
	private int _sendToNodeByteBuf(String srcActor, String dstNode, String dstActor, int userCmd, ByteBuf userData, int userDataType){
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
			.setSrcActor(srcActor).setDstActor(dstActor);
			byte[] bufHead = bd.build().toByteArray();
			int headLen = bufHead.length;
			int dataLen = 0;
			if(userData != null){
				dataLen = userData.readableBytes();
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
		if(userData != null){
			userData.release();
		}
		return 1;
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
