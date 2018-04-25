package fun.lib.actor.api.http;

public interface DFHttpClientHandler {

	/**
	 * http请求响应回调
	 * @param rsp 响应内容
	 * @param isSucc 请求是否成功
	 * @param errMsg 请求失败时的错误信息
	 * @return 响应消息是否由框架自动释放
	 */
	public int onHttpResponse(Object rsp, boolean isSucc, String errMsg);
}
