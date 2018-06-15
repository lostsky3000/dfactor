package fun.lib.actor.core;

public interface IWebScriptAPI {

	public Object onSessionStart(Object sessionJs);
	public boolean onSessionDestroy(Object sessionJs);
	public String onSessionId();
	
	public void header(String name, String val);
	
	public void requireFile(String curDir, String fileName, String pageId);
}
