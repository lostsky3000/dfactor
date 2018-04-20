package fun.lib.actor.api;

public interface DFTcpEncoder {

	/**
	 * 消息编码
	 * @param msgRaw 应用层消息对象
	 * @return 编码后消息对象,由通信层发送出去
	 */
	public Object onEncode(Object msgRaw);
}
