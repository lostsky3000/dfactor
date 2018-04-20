package fun.lib.actor.api;

public interface DFTcpDecoder {

	/**
	 * 消息解码
	 * @param msgRaw 原始消息对象(解码后，将由系统自动释放)
	 * @return 解码后的消息对象
	 */
	public Object onDecode(Object msgRaw);
}
