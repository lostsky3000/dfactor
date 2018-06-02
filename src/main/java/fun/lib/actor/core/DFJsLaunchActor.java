package fun.lib.actor.core;

import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public final class DFJsLaunchActor extends DFActor{

	public DFJsLaunchActor(Integer id, String name, Boolean isBlockActor) {
		super(id, name, isBlockActor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onStart(Object param) {
		Map<String, Object> mapParam = (Map<String, Object>) param;
		ScriptEngine engine = (ScriptEngine) mapParam.get("engine");
		String entryActor = (String) mapParam.get("entryActor");
		String entryActorName = (String) mapParam.get("entryActorName");
		Bindings bind = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		//
		ScriptObjectMirror mirFunc = (ScriptObjectMirror) bind.get(entryActor);
		//
		DFActorManagerJs.get().createActor(mirFunc, entryActorName, null, null);
		mapParam.clear(); mapParam = null;
	}
}
