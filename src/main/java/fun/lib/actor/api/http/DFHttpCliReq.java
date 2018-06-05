package fun.lib.actor.api.http;


public interface DFHttpCliReq {

	public DFHttpCliReq end();
	
	public DFHttpCliReq uri(String uri);
	
	public DFHttpCliReq method(String method);
	
	public DFHttpCliReq header(String name, String val);
	
	public DFHttpCliReq content(Object data);
	
	public DFHttpCliReq contentType(String contentType);
	
	public DFHttpCliReq form(Boolean isForm);
	
	public DFHttpCliReq useDefaultHeader(boolean use);
}
