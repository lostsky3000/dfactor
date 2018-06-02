package com.funtag.util.script;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public final class DFJsUtil {

	public static boolean isJsFunction(Object func){
		if(func != null && func instanceof ScriptObjectMirror){
			ScriptObjectMirror mir = (ScriptObjectMirror) func;
			return mir.isFunction();
		}
		return false;
	}
	
	public static boolean isJsArray(Object arr){
		if(arr != null && arr instanceof ScriptObjectMirror){
			ScriptObjectMirror mir = (ScriptObjectMirror) arr;
			boolean b = mir.isArray();
			b = mir.isExtensible();
			
			return mir.isArray();
		}
		return false;
	}
}
