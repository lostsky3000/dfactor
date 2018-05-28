package fun.lib.actor.api;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.GeneratedMessageV3;

import fun.lib.actor.api.cb.CbActorRsp;
import fun.lib.actor.api.cb.CbActorRspAsync;
import fun.lib.actor.api.cb.CbCallHereBlock;
import fun.lib.actor.api.cb.CbRpc;
import fun.lib.actor.api.cb.RpcFuture;
import fun.lib.actor.api.cb.CbCallHere;
import fun.lib.actor.core.DFActor;
import fun.lib.actor.po.ActorProp;
import io.netty.buffer.ByteBuf;

public interface DFActorSystem {
	
	/**
	 * 创建actor
	 * @param classz actor类的class
	 * @return >0 创建成功,actor的id; <0创建失败，错误码
	 */
	public int createActor(Class<? extends DFActor> classz);
	/**
	 * 创建actor
	 * @param classz actor类的class
	 * @param param 传递的参数
	 * @return >0 创建成功,actor的id; <0创建失败，错误码
	 */
	public int createActor(Class<? extends DFActor> classz, Object param);
	/**
	 * 创建actor
	 * @param name actor名称，要求全局唯一
	 * @param classz actor类的class
	 * @return >0 创建成功,actor的id; <0创建失败，错误码
	 */
	public int createActor(String name, Class<? extends DFActor> classz);
	/**
	 * 创建actor
	 * @param name actor名称，要求全局唯一
	 * @param classz actor类的class
	 * @param param 传递的参数
	 * @return >0 创建成功,actor的id; <0创建失败，错误码
	 */
	public int createActor(String name, Class<? extends DFActor> classz, Object param);
	/**
	 * 创建actor
	 * @param prop actor配置
	 * @return >0 创建成功,actor的id; <0创建失败，错误码
	 */
	public int createActor(ActorProp prop);
	
	/**
	 * 调用其它actor，由其它actor的调用线程回调cb中的方法
	 * @param dstId 目标actorId
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @param cb 回调
	 * @return
	 */
	public int callHere(int dstId, int cmd, Object payload, CbCallHere cb);
	/**
	 * 调用其它actor，由其它actor的调用线程回调cb中的方法
	 * @param dstId 目标actorName
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @param cb 回调
	 * @return
	 */
	public int callHere(String dstName, int cmd, Object payload, CbCallHere cb);
	
	/**
	 * 调用框架自带的blockActor，由调用blockActor的线程回调cb中的方法
	 * @param shardId shardId%block线程数量，算出消息由哪个blockActor处理
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @param cb 回调
	 * @return
	 */
	public int callHereBlock(int shardId, int cmd, Object payload, CbCallHereBlock cb);
	
	/**
	 * 向指定id的actor发消息，带有回调
	 * @param dstId 目标actorId
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @param cb 回调
	 * @return 0成功  非0失败
	 */
	public int call(int dstId, int cmd, Object payload, CbActorRsp cb);
	
	/**
	 * 向指定name的actor发消息，带有回调
	 * @param dstName 目标actorName
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @param cb 回调
	 * @return 0成功  非0失败
	 */
	public int call(String dstName, int cmd, Object payload, CbActorRsp cb);
	
	/**
	 * 回复当前消息来源actor
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendback(int cmd, Object payload);
	/**
	 * 向指定id的actor发消息
	 * @param dstId 目标actor id
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0为发送成功  否则错误码
	 */
	public int send(int dstId, int cmd, Object payload);
	/**
	 * 向指定name的actor发消息
	 * @param dstName 目标actor 名称
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0为发送成功  否则错误码
	 */
	public int send(String dstName, int cmd, Object payload);
	//
	/**
	 * 向集群指定结点的actor发送消息
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToCluster(String dstNode, String dstActor, int cmd, String payload);
	/**
	 * 向集群指定结点的actor发送消息
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToCluster(String dstNode, String dstActor, int cmd, byte[] payload);
	/**
	 * 向集群指定结点的actor发送消息
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToCluster(String dstNode, String dstActor, int cmd, ByteBuf payload);
	/**
	 * 向集群指定结点的actor发送消息
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToCluster(String dstNode, String dstActor, int cmd, JSONObject payload);
	/**
	 * 向集群指定结点的actor发送消息
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToCluster(String dstNode, String dstActor, int cmd, DFSerializable payload);
	
	//sendToClusterByType
	/**
	 * 向集群指定类型结点的actor发送消息
	 * @param dstNodeType 目标结点类型
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, String payload);
	/**
	 * 向集群指定类型结点的actor发送消息
	 * @param dstNodeType 目标结点类型
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, JSONObject payload);
	/**
	 * 向集群指定类型结点的actor发送消息
	 * @param dstNodeType 目标结点类型
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, byte[] payload);
	/**
	 * 向集群指定类型结点的actor发送消息
	 * @param dstNodeType 目标结点类型
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, ByteBuf payload);
	/**
	 * 向集群指定类型结点的actor发送消息
	 * @param dstNodeType 目标结点类型
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterByType(String dstNodeType, String dstActor, int cmd, DFSerializable payload);
	
	//sendToClusterAll
	/**
	 * 向集群所有结点的actor发送消息
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterAll(String dstActor, int cmd, String payload);
	/**
	 * 向集群所有结点的actor发送消息
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterAll(String dstActor, int cmd, JSONObject payload);
	/**
	 * 向集群所有结点的actor发送消息
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterAll(String dstActor, int cmd, byte[] payload);
	/**
	 * 向集群所有结点的actor发送消息
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterAll(String dstActor, int cmd, ByteBuf payload);
	/**
	 * 向集群所有结点的actor发送消息
	 * @param dstActor 目标actor名字
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int sendToClusterAll(String dstActor, int cmd, DFSerializable payload);
	
	//
	/**
	 * 调用集群指定结点，指定actor，指定方法
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param dstMethod 方法名
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd, String payload);
	/**
	 * 调用集群指定结点，指定actor，指定方法
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param dstMethod 方法名
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd, byte[] payload);
	/**
	 * 调用集群指定结点，指定actor，指定方法
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param dstMethod 方法名
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd, ByteBuf payload);
	/**
	 * 调用集群指定结点，指定actor，指定方法
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param dstMethod 方法名
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd, DFSerializable payload);
	/**
	 * 调用集群指定结点，指定actor，指定方法
	 * @param dstNode 结点名字
	 * @param dstActor 目标actor名字
	 * @param dstMethod 方法名
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public RpcFuture callClusterMethod(String dstNode, String dstActor, String dstMethod, int cmd, JSONObject payload);
	
	/**
	 * 调用本地actor的指定方法
	 * @param dstId 目标actorId
	 * @param dstMethod 方法名
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public RpcFuture callMethod(int dstId, String dstMethod, int cmd, Object payload);
	/**
	 * 调用本地actor的指定方法
	 * @param dstId 目标actor名字
	 * @param dstMethod 方法名
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public RpcFuture callMethod(String dstName, String dstMethod, int cmd, Object payload);
	
	
	/**
	 * 检测指定结点是否在线
	 * @param nodeName
	 * @return
	 */
	public boolean isNodeOnline(String nodeName);
	
	/**
	 * 获取指定类型的结点在线数量
	 * @param nodeType 结点类型
	 * @return
	 */
	public int getNodeNumByType(String nodeType);
	/**
	 * 获取所有在线结点数量
	 * @return
	 */
	public int getAllNodeNum();
	
	/**
	 * 结束当前actor
	 */
	public void exit();
	
	/**
	 * 关闭整个dfactor
	 */
	public void shutdown();
	
	/**
	 * 注册计时器，仅回调一次，Use timer.timeout() instead
	 * @param delay 超时单位，见DFActor.TIMER_UNIT_MILLI，超时一秒为 delay=1000/DFActor.TIMER_UNIT_MILLI
	 * @param requestId 请求标识
	 */
	@Deprecated 
	public void timeout(int delay, int requestId);
	
	/**
	 * 获取计时器启动时间，单位毫秒，Use timer.getTimeStart() instead
	 * @return
	 */
	@Deprecated
	public long getTimeStart();
	/**
	 * 获取计时器当前时间，单位毫秒，Use timer.getTimeNow() instead
	 * @return
	 */
	@Deprecated
	public long getTimeNow();
}
