package fun.lib.actor.api.cb;

import fun.lib.actor.po.DFNode;

public interface CbNode {
	/**
	 * 结点上线回调
	 * @param node 结点对象
	 */
	public void onNodeAdd(DFNode node);
	
	/**
	 * 结点下线回调
	 * @param node 结点对象
	 */
	public void onNodeRemove(DFNode node);
}
