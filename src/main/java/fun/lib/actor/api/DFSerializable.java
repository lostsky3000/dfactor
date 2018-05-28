package fun.lib.actor.api;

import io.netty.buffer.ByteBuf;

public interface DFSerializable {

	/**
	 * 获取序列化后的大小，以分配序列化所需内存
	 * @return
	 */
	public int getSerializedSize();
	
	/**
	 * 序列化时调用，buf尺寸为getSerializedSize()返回的大小
	 * @param buf
	 * @return
	 */
	public int onSerialize(ByteBuf buf);
	
	/**
	 * 反序列化
	 * @param buf
	 * @return
	 */
	public int onDeserialize(ByteBuf buf);
}
