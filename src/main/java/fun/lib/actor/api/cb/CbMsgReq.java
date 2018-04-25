package fun.lib.actor.api.cb;

public interface CbMsgReq {

	/**
	 * 向消息来源actor发送回复
	 * @param cmd 消息码
	 * @param payload 消息体
	 * @return 0成功，非0失败
	 */
	public int callback(int cmd, Object payload);
}
