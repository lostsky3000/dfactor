package fun.lib.actor.api.cb;

public interface RpcFuture {

	/**
	 * 添加结果回调监听
	 * @param cb 回调对象
	 * @param timeoutMilli 回调超时(毫秒)
	 */
	public boolean addListener(CbRpc cb, int timeoutMilli);
	
	/**
	 * 是否发送成功
	 * @return
	 */
	public boolean isSendSucc();
}
