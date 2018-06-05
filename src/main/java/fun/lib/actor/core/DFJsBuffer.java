package fun.lib.actor.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.CharsetUtil;

public final class DFJsBuffer implements IScriptBuffer{
	
	private final ByteBuf buf;
	private DFJsBuffer(int capacity) {
		buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer(capacity);
	}
	private DFJsBuffer(ByteBuf buf){
		this.buf = buf;
	}
	
	protected static DFJsBuffer newBuffer(int capacity){
		return new DFJsBuffer(capacity);
	}
	protected static DFJsBuffer newBuffer(ByteBuf buf){
		return new DFJsBuffer(buf);
	}
	
	protected ByteBuf getBuf(){
		return buf;
	}
	
	protected void readBytes(byte[] bytes){
		buf.readBytes(bytes);
	}

	@Override
	public int leftRead() {
		return buf.readableBytes();
	}
	@Override
	public boolean writeInt(int num) {
		if(buf.writableBytes() >= 4){
			buf.writeInt(num);
			return true;
		}
		return false;
	}
	@Override
	public boolean writeShort(int num) {
		if(buf.writableBytes() >= 2){
			buf.writeShort(num);
			return true;
		}
		return false;
	}
	@Override
	public boolean writeByte(int num) {
		if(buf.writableBytes() >= 1){
			buf.writeByte(num);
			return true;
		}
		return false;
	}
	@Override
	public boolean writeStr(String src, int len) {
		byte[] bufStr = src.getBytes(CharsetUtil.UTF_8);
		int lenStr = bufStr.length;
		buf.writeBytes(bufStr, 0, Math.min(lenStr, buf.writableBytes()));
		return true;
	}

	@Override
	public boolean writeBytes(IScriptBuffer src) {
		ByteBuf bufSrc = ((DFJsBuffer) src).getBuf();
		if(buf.writableBytes() >= bufSrc.readableBytes()){
			buf.writeBytes(bufSrc);
			return true;
		}
		return false;
	}

	@Override
	public int readInt() {
		return buf.readInt();
	}
	@Override
	public int readShort() {
		return buf.readShort();
	}
	@Override
	public int readByte() {
		return buf.readByte();
	}
	@Override
	public String readStr(int len) {
		return (String) buf.readCharSequence(len, CharsetUtil.UTF_8);
	}

	@Override
	public void release() {
		if(buf.refCnt() > 0){
			buf.release();
		}
	}

	@Override
	public int getInt(int idx) {
		return buf.getInt(idx);
	}
	@Override
	public int getShort(int idx) {
		return buf.getShort(idx);
	}
	@Override
	public int getByte(int idx) {
		return buf.getByte(idx);
	}
	
}
