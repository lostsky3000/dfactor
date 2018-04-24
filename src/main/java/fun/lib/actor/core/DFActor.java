package fun.lib.actor.core;

import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.api.DFActorNet;
import fun.lib.actor.api.DFActorSystem;
import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.DFUdpChannel;
import fun.lib.actor.api.DFActorMsgCallback;
import io.netty.channel.socket.DatagramPacket;

public class DFActor {
	public static final long TIMER_UNIT_MILLI = 10;
	protected final int id;
	protected final String name;
	protected final int consumeType;
	private final DFActorManager _mgr;
	protected final DFActorLog log;
	protected final DFActorSystem sys;
	protected final DFActorNet net;
	protected final boolean isBlockActor;
	//
	protected int lastSrcId = 0;
	protected Object lastUserHandler = null;
	protected boolean curCanCb = false;
	
	public DFActor(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
		this.id = id;
		this.name = name;
		this.isBlockActor = isBlockActor;
		if(consumeType < DFActorDefine.CONSUME_AUTO || consumeType > DFActorDefine.CONSUME_ALL){ //consumeType invalid
			consumeType = DFActorDefine.CONSUME_AUTO;
		}
		this.consumeType = consumeType;
		_mgr = DFActorManager.get();
		//
		log = new DFActorLogWrapper(id, name);
		sys = new DFActorSystemWrapper();
		net = new DFActorNetWrapper(id);
	}
	
	public int getId(){
		return id;
	}
	public String getName(){
		return name;
	}
	public int getConsumeType(){
		return consumeType;
	}
	
	//event
	/**
	 * 接收其它actor发过来的消息
	 * @param srcId 发送者actor的id
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return
	 */
	public int onMessage(int srcId, int cmd, Object payload){return DFActorDefine.MSG_AUTO_RELEASE;}
	/**
	 * actor创建时调用一次
	 * @param param 创建actor的调用者传入的参数
	 */
	public void onStart(Object param){}
	/**
	 * 周期调用的回调函数
	 * @param dltMilli 距离上次回调的间隔，单位毫秒
	 */
	public void onSchedule(long dltMilli){}
	/**
	 * 定时器回调函数
	 * @param requestId 注册定时器时传入的id，用于多个定时器触发时区分
	 */
	public void onTimeout(int requestId){}
	//tcp common
	/**
	 * tcp连接建立时调用
	 * @param requestId 启动监听时传入的id
	 * @param channel tcp连接对象
	 */
	public void onTcpConnOpen(int requestId, DFTcpChannel channel){}
	
	
	/**
	 * tcp连接断开时调用 
	 * @param requestId 启动监听时传入的id
	 * @param channel tcp连接对象
	 */
	public void onTcpConnClose(int requestId, DFTcpChannel channel){}
	/**
	 * 收到网络消息时调用
	 * @param requestId 启动监听时传入的id
	 * @param channel tcp连接对象
	 * @param msg 消息内容
	 * @return 返回消息释放策略，见DFActorDefine.MSG_AUTO_RELEASE
	 */
	public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg){return DFActorDefine.MSG_AUTO_RELEASE;}
	
	//tcp server
	/**
	 * 启动tcp监听服务的结果回调
	 * @param requestId 启动监听时传入的id
	 * @param isSucc 监听是否成功
	 * @param errMsg 监听失败的错误描述
	 */
	public void onTcpServerListenResult(int requestId, boolean isSucc, String errMsg){}
	//tcp client
	/**
	 * 启动tcp连接的结果回调
	 * @param requestId 启动连接时传入的id
	 * @param isSucc 连接是否成功
	 * @param errMsg 连接失败的错误描述
	 */
	public void onTcpClientConnResult(int requestId, boolean isSucc, String errMsg){};
	
	//udp server
	public void onUdpServerListenResult(int requestId, boolean isSucc, String errMsg, DFUdpChannel channel){}
	public int onUdpServerRecvMsg(int requestId, DFUdpChannel channel, DatagramPacket pack){return DFActorDefine.MSG_AUTO_RELEASE;}
	
	//system api
	private class DFActorSystemWrapper implements DFActorSystem{
		public final int createActor(String name, Class<? extends DFActor> classz){
			return _mgr.createActor(name, classz, null, 0, DFActorDefine.CONSUME_AUTO, false);
		}
		public final int createActor(String name, Class<? extends DFActor> classz, Object param){
			return _mgr.createActor(name, classz, param, 0, DFActorDefine.CONSUME_AUTO, false);
		}
		public final int createActor(String name, Class<? extends DFActor> classz, Object param, 
				int scheduleUnit){
			return _mgr.createActor(name, classz, param, scheduleUnit, DFActorDefine.CONSUME_AUTO, false);
		}
		public final int createActor(String name, Class<? extends DFActor> classz, Object param, 
				int scheduleUnit, int consumeType){
			return _mgr.createActor(name, classz, param, scheduleUnit, consumeType, false);
		}
		public final int createActor(String name, Class<? extends DFActor> classz, Object param, 
				int scheduleUnit, int consumeType, boolean isBlockActor){
			return _mgr.createActor(name, classz, param, scheduleUnit, consumeType, isBlockActor);
		}
		//
		@Override
		public int send(int dstId, int cmd, Object payload) {
			return _mgr.send(id, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, false);
		}
//		@Override
//		public int send(int dstId, int requestId, int subject, int cmd, Object payload) {
//			return _mgr.send(id, dstId, requestId, subject, cmd, payload, true, null, null, false);
//		}
		@Override
		public int send(String dstName, int cmd, Object payload) {
			return _mgr.send(id, dstName, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null);
		}
		
		
		public final void exit(){
			_mgr.removeActor(id);
			log.verb("exit");
		}
		public final void timeout(int delay, int requestId){
			_mgr.addTimeout(id, delay, requestId);
		}
		@Override
		public long getTimeStart() {
			return _mgr.getTimerStartNano();
		}
		@Override
		public long getTimeNow() {
//			return System.currentTimeMillis();
			return _mgr.getTimerNowNano();
		}
		//
		@Override
		public int call(int dstId, int cmd, Object payload, DFActorMsgCallback cb) {
			return _mgr.send(id, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, cb, false);
		}
		@Override
		public int call(String dstName, int cmd, Object payload, DFActorMsgCallback cb) {
			return _mgr.send(id, dstName, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, cb);
		}
		@Override
		public int callback(int cmd, Object payload) {
			if(lastSrcId == 0){ //没有发送人可以调用
				return -1;
			}
			if(!curCanCb){ //当前不可回调
				return -2;
			}
			int ret = _mgr.sendCallback(id, lastSrcId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, lastUserHandler);
			//
			lastSrcId = 0;
			curCanCb = false;
			lastUserHandler = null;
			return ret;
		}
		
		
		
	}
	
	
	/**
	 * 将真实时长转换为actor计时器格式时长
	 * @param timeMilli 真实时长(毫秒)
	 * @return actor计时器格式时长
	 */
	public static int transTimeRealToTimer(long timeMilli){
		return (int) (timeMilli/TIMER_UNIT_MILLI);
	}
}
