package fun.lib.actor.api;

import fun.lib.actor.api.cb.CbTimeout;

public interface DFActorTimer {

	/**
	 * 注册计时器，仅回调一次
	 * @param delayMilli 延迟毫秒数
	 * @param requestId 请求标识
	 */
	public void timeout(int delayMilli, int requestId);
	
	/**
	 * 注册计时器，仅回调一次
	 * @param delayMilli 延迟毫秒数
	 * @param cb 超时回调函数
	 */
	public void timeout(int delayMilli, CbTimeout cb);
	
	/**
	 * 获取计时器启动时间，单位毫秒
	 * @return 时间
	 */
	public long getTimeStart();
	/**
	 * 获取计时器当前时间，单位毫秒
	 * @return 时间
	 */
	public long getTimeNow();
}
