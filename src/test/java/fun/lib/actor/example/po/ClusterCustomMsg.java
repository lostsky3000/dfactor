package fun.lib.actor.example.po;

import fun.lib.actor.api.DFSerializable;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public final class ClusterCustomMsg implements DFSerializable{
	private int id = 0;
	private int age = 0;
	private String name = null;
	//
	private byte[] nameBuf = null;
	@Override
	public int getSerializedSize() {
		int len = 4 + 4; //id(4) + age(4)
		if(this.name != null){ //has name
			this.nameBuf = this.name.getBytes(CharsetUtil.UTF_8);
			len += this.nameBuf.length;
		}
		return len;
	}
	@Override
	public int onSerialize(ByteBuf buf) {
		buf.writeInt(this.id);
		buf.writeInt(this.age);
		if(this.nameBuf != null){
			buf.writeBytes(this.nameBuf);
		}
		return 0;
	}
	@Override
	public int onDeserialize(ByteBuf buf) {
		this.id = buf.readInt();
		this.age = buf.readInt();
		if(buf.readableBytes() > 0){ //has more
			this.name = (String) buf.readCharSequence(buf.readableBytes(), CharsetUtil.UTF_8);
		}
		return 0;
	}
	@Override
	public String toString() {
		return "id="+id+", age="+age+", name="+name;
	}
	public int getId() {
		return id;
	}
	public ClusterCustomMsg setId(int id) {
		this.id = id;
		return this;
	}
	public int getAge() {
		return age;
	}
	public ClusterCustomMsg setAge(int age) {
		this.age = age;
		return this;
	}
	public String getName() {
		return name;
	}
	public ClusterCustomMsg setName(String name) {
		this.name = name;
		return this;
	}
	
}
