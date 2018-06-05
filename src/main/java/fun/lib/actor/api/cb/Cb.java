package fun.lib.actor.api.cb;

public interface Cb {

	/**
	 * rpc回调方法
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0
	 */
	public int onCallback(int cmd, Object payload);
	
	/**
	 * rpc调用失败的通知
	 * @param code 错误码，见RpcError.java
	 * @return 0
	 */
	public int onFailed(int code);
}
