package fun.lib.actor.po;

public final class DFActorResult {

	public final int code;
	public final String msg;
	public final int extInt;
	public final String extString;
	public final Object extObj;
	
	public DFActorResult(int code, String msg) {
		this.code = code;
		this.msg = msg;
		this.extInt = 0;
		this.extString = null;
		this.extObj = null;
	}
	public DFActorResult(int code, String msg, int extInt, String extString) {
		this.code = code;
		this.msg = msg;
		this.extInt = extInt;
		this.extString = extString;
		this.extObj = null;
	}
	public DFActorResult(int code, String msg, int extInt, String extString, Object extObj) {
		this.code = code;
		this.msg = msg;
		this.extInt = extInt;
		this.extString = extString;
		this.extObj = extObj;
	}
}
