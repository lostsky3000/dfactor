package fun.lib.actor.po;

public final class DFActorEvent {

	private final int what;
	private final String msg;
	
	private int extInt1 = 0;
	private String extString1 = null;
	private int extInt2 = 0;
	private String extString2 = null;
	
	private Object extObj1;
	private Object extObj2;
	
	private Object userHandler;
	
	public DFActorEvent(int what) {
		this.what = what;
		msg = null;
	}
	public DFActorEvent(int what, String msg) {
		this.what = what;
		this.msg = msg;
	}
	public DFActorEvent setExtInt1(int extInt1){
		this.extInt1 = extInt1;
		return this;
	}
	public DFActorEvent setExtInt2(int extInt2){
		this.extInt2 = extInt2;
		return this;
	}
	public DFActorEvent setExtString1(String extString1){
		this.extString1 = extString1;
		return this;
	}
	public DFActorEvent setExtString2(String extString2){
		this.extString2 = extString2;
		return this;
	}
	public DFActorEvent setExtObj1(Object extObj1){
		this.extObj1 = extObj1;
		return this;
	}
	public DFActorEvent setExtObj2(Object extObj2){
		this.extObj2 = extObj2;
		return this;
	}
	
	public int getWhat() {
		return what;
	}
	public String getMsg() {
		return msg;
	}
	public int getExtInt1() {
		return extInt1;
	}
	public String getExtString1() {
		return extString1;
	}
	public int getExtInt2() {
		return extInt2;
	}
	public String getExtString2() {
		return extString2;
	}
	public Object getExtObj1() {
		return extObj1;
	}
	public Object getExtObj2() {
		return extObj2;
	}
	public Object getUserHandler() {
		return userHandler;
	}
	public void setUserHandler(Object userHandler) {
		this.userHandler = userHandler;
	}
	
	
}
