package fun.lib.actor.api.http;

import io.netty.handler.codec.http.HttpHeaderValues;

public final class DFHttpContentType {

	public static final String TEXT_HTML = "text/html";
	public static final String TEXT_PLAIN = HttpHeaderValues.TEXT_PLAIN.toString();// "text/plain";
	public static final String FORM = HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString();// "application/x-www-form-urlencoded";
	public static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString(); //"application/json";
	public static final String XML = "application/xml";
	public static final String OCTET_STREAM = HttpHeaderValues.APPLICATION_OCTET_STREAM.toString(); //"application/octet-stream";
//	public static final String UNKNOWN = "unknown";
}
