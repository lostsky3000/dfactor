package fun.lib.actor.api;

import fun.lib.actor.core.DFActor;

public interface DFActorSystem {
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
	 * @param name actor名称，要求全局唯一
	 * @param classz actor类的class
	 * @param param 传递的参数
	 * @param scheduleUnit >0 开启schedule回调
	 * @return >0 创建成功,actor的id; <0创建失败，错误码
	 */
	public int createActor(String name, Class<? extends DFActor> classz, Object param, 
			int scheduleUnit);
	/**
	 * 创建actor
	 * @param name actor名称，要求全局唯一
	 * @param classz actor类的class
	 * @param param 传递的参数
	 * @param scheduleUnit >0 开启schedule回调
	 * @param consumeType 消息消费类型，见DFActorDefine.CONSUME_AUTO
	 * @return >0 创建成功,actor的id; <0创建失败，错误码
	 */
	public int createActor(String name, Class<? extends DFActor> classz, Object param, 
			int scheduleUnit, int consumeType);
	
	/**
	 * 创建actor
	 * @param name actor名称，要求全局唯一
	 * @param classz actor类的class
	 * @param param 传递的参数
	 * @param scheduleUnit >0 开启schedule回调
	 * @param consumeType 消息消费类型，见DFActorDefine.CONSUME_AUTO
	 * @param isBlockActor 是否为阻塞类型actor
	 * @return >0 创建成功,actor的id; <0创建失败，错误码
	 */
	public int createActor(String name, Class<? extends DFActor> classz, Object param, 
			int scheduleUnit, int consumeType, boolean isBlockActor);
	
	
	
	
	/**
	 * 发送actor间消息
	 * @param dstId 目标actor id
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0为发送成功  否则错误码
	 */
	public int send(int dstId, int cmd, Object payload);
	/**
	 * 发送actor间消息
	 * @param dstId 目标actor id
	 * @param requestId 消息标识
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0为发送成功  否则错误码
	 */
	public int send(int dstId, int requestId, int cmd, Object payload);
	/**
	 * 发送actor间消息
	 * @param dstId 目标actor id
	 * @param requestId 消息标识
	 * @param subject 消息分类
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0为发送成功  否则错误码
	 */
	public int send(int dstId, int requestId, int subject, int cmd, Object payload);
	/**
	 * 发送actor间消息
	 * @param dstName 目标actor 名称
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0为发送成功  否则错误码
	 */
	public int send(String dstName, int cmd, Object payload);
	/**
	 * 发送actor间消息
	 * @param dstName 目标actor 名称
	 * @param requestId 消息标识
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0为发送成功  否则错误码
	 */
	public int send(String dstName, int requestId, int cmd, Object payload);
	/**
	 * 发送actor间消息
	 * @param dstName 目标actor 名称
	 * @param requestId 消息标识
	 * @param subject 消息分类
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0为发送成功  否则错误码
	 */
	public int send(String dstName, int requestId, int subject, int cmd, Object payload);
	
	/**
	 * 结束当前actor
	 */
	public void exit();
	
	/**
	 * 注册计时器，仅回调一次
	 * @param delay 超时单位，见DFActor.TIMER_UNIT_MILLI，超时一秒为 delay=1000/DFActor.TIMER_UNIT_MILLI
	 * @param requestId 请求标识
	 */
	public void timeout(int delay, int requestId);
	
	/**
	 * 获取计时器启动时间，单位毫秒
	 * @return
	 */
	public long getTimeStart();
	/**
	 * 获取计时器当前时间，单位毫秒
	 * @return
	 */
	public long getTimeNow();
}
