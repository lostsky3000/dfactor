package fun.lib.actor.po;

import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorDefine;

public final class ActorProp {
/*
 * String entryName, 
			Class<? extends DFActor> entryClassz, Object entryParam, 
			int entryScheduleUnit, int entryConsumeType
 * */
	
	private String name = null;
	private Class<? extends DFActor> classz = null;
	private Object param = null;
	private int scheduleMilli = 0;
	private int consumeType = DFActorDefine.CONSUME_AUTO;
	private boolean isBlock = false;
	
	private ActorProp(){
		
	}
	//set
	public ActorProp name(String name){
		this.name = name;
		return this;
	}
	public ActorProp classz(Class<? extends DFActor> classz){
		this.classz = classz;
		return this;
	}
	public ActorProp param(Object param){
		this.param = param;
		return this;
	}
	public ActorProp scheduleMilli(int scheduleMilli){
		this.scheduleMilli = scheduleMilli;
		return this;
	}
	public ActorProp consumeType(int consumeType){
		this.consumeType = consumeType;
		return this;
	}
	public ActorProp blockActor(boolean isBlock){
		this.isBlock = isBlock;
		return this;
	}
	
	//get
	public String getName(){
		return name;
	}
	public Class<? extends DFActor> getClassz(){
		return classz;
	}
	public Object getParam(){
		return param;
	}
	public int getScheduleMilli(){
		return scheduleMilli;
	}
	public int getConsumeType(){
		return consumeType;
	}
	public boolean isBlock(){
		return isBlock;
	}
	//new
	public static ActorProp newProp(){
		return new ActorProp();
	}
}
