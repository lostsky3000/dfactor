package fun.lib.actor.core;

public interface IScriptBuffer {

	public int leftRead();
	
	public boolean writeInt(int num);
	public boolean writeShort(int num);
	public boolean writeByte(int num);
	public boolean writeStr(String src, int len);
	public boolean writeBytes(IScriptBuffer src);
	
	public int readInt();
	public int readShort();
	public int readByte();
	public String readStr(int len);
	
	public int getInt(int idx);
	public int getShort(int idx);
	public int getByte(int idx);
	
	public void release();
	
}
