package fun.lib.actor.po;

public final class DFNode {
	/**
	 * 结点名字
	 */
	public final String name;
	/**
	 * 结点类型
	 */
	public final String type;
	/**
	 * 结点主机地址
	 */
	public final String host;
	
	public DFNode(String name, String type, String host) {
		this.name = name;
		this.type = type;
		this.host = host;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return name+"("+type+") from "+host;
	}
}
