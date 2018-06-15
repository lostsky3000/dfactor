package com.funtag.util.script;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import fun.lib.actor.core.DFVirtualHost;
import fun.lib.actor.core.IWebScriptAPI;

public final class DFJsPageModel implements IScriptAnalyzer{
	public static final String SFX = "jssp";
	private static final String TAG_BEGIN = "<?"+SFX;
	private static final int TAG_BEGIN_LEN = TAG_BEGIN.length();
	private static final String TAG_END = "?>";
	private static final int TAG_END_LEN = TAG_END.length();
	//
	public static final String TXT_ECHO_FUNC = "df_aNdH1mfiQ";
	
	public final String name;
	public final String dir;
	public final String file;
	private CompiledScript cs = null;
	private StringBuilder curSbOut = null;
	private StringBuilder sbJs = null;
	private HashMap<Integer, String> mapTxt = null;
	private int version = 0;
	
	//
	
	public DFJsPageModel(String name, String dir, String file) {
		this.name = name;
		this.dir = dir;
		this.file = file;
		sbJs = new StringBuilder();
		mapTxt = new HashMap<>();
	}
	
	
	public boolean analyze(StringBuilder sb, Compilable compiler){
		boolean bRet = false;
		do {
			//reset
			sbJs.setLength(0);
			mapTxt.clear();
			int txtCount = 0;
			//
			boolean jsStart = false;
			boolean isJs = false;
			int srcLen = sb.length();
			int idxSearch = 0;
			int subBegin = idxSearch;
			int subEnd = 0;
			int tmp = 0;
			char tmpCh = ' ';
			String curSec = null;
			while(idxSearch < srcLen){
				curSec = null;
				isJs = false;
				if(jsStart){
					tmp = sb.indexOf(TAG_END, idxSearch);
					if(tmp < 0){ //no endTag found
						subEnd = srcLen;
						idxSearch = srcLen;
					}else{  //endTag found, check
						curSec = sb.substring(subBegin + TAG_BEGIN_LEN, tmp);
						idxSearch = tmp + TAG_END_LEN;
						subBegin = idxSearch;
						subEnd = -2;
						isJs = true;
						jsStart = false;
						
//						try{
//							compiler.compile(curSec);
//							//compile succ
//							idxSearch = tmp + TAG_END_LEN;
//							subBegin = idxSearch;
//							subEnd = -2;
//							isJs = true;
//							jsStart = false;
//						}catch(Throwable e){ //compile failed
//							e.printStackTrace();
//							curSec = null;
//							idxSearch = tmp + TAG_END_LEN;
//							if(idxSearch > srcLen - TAG_END_LEN){ //end
//								subEnd = srcLen;
//								idxSearch = srcLen;
//							}
//						}
					}
				}else{  //js not start, search startTag
					tmp = sb.indexOf(TAG_BEGIN, idxSearch);
					if(tmp < 0){  //no startTag
						subEnd = srcLen;
						idxSearch = srcLen;
					}else{  //has startTag
						if(tmp < srcLen - TAG_BEGIN_LEN - TAG_END_LEN){ //valid startTag
							tmpCh = sb.charAt(tmp +TAG_BEGIN_LEN);
							if(Character.isWhitespace(tmpCh)){
								curSec = sb.substring(subBegin, tmp);
								idxSearch = tmp;
								subBegin = idxSearch;
								subEnd = -2;
								jsStart = true;
							}else{ //bad startTag
								idxSearch = tmp + TAG_BEGIN_LEN;
								subEnd = -2;
							}
						}else{  //bad startTag
							idxSearch = tmp + TAG_BEGIN_LEN;
							subEnd = -2;
						}
					}
				}
				//
				if(subBegin < subEnd){  //valid
					curSec = sb.substring(subBegin, subEnd);
					subBegin = subEnd;
				}
				if(curSec != null){
					curSec = curSec.trim();
					if(!curSec.equals("")){
						if(isJs){  //js code
							sbJs.append(curSec).append('\n');
						}else{ //text
							sbJs.append(TXT_ECHO_FUNC).append("("+(++txtCount)+");").append('\n');
							mapTxt.put(txtCount, curSec);
						}
					}
				}
			}
			try {
				cs = null;
				cs = compiler.compile(sbJs.toString());
			} catch (ScriptException e) {
				e.printStackTrace();
				break;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	
	public boolean execute(Bindings bind, StringBuilder sbOut, DFJsPageModel parent){
		if(cs != null){
			curSbOut = sbOut;
			try {
				cs.eval(bind);
				curSbOut = null;
				if(parent != null){ //has parent, reset
					bind.put(DFVirtualHost.JS_INNER_ANALYZER, parent);
					bind.put(DFVirtualHost.JS_DIR, parent.dir);
					bind.put(DFVirtualHost.JS_FILE, parent.file);
					bind.put(DFVirtualHost.JS_PAGE_ID, parent.name);
				}
				return true;
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	@Override
	public void scriptCall(Object content) {
		if(content instanceof Double){
			curSbOut.append(((Double)content).intValue());
		}else{
			curSbOut.append(content);
		}
	}
	@Override
	public void txtCall(Integer txtId) {
		final String str = mapTxt.get(txtId);
		if(str != null){
			curSbOut.append(str);
		}else{ //error, impossible
			
		}
	}
	
	public int getVersion(){
		return version;
	}
	public void setVersion(int ver){
		this.version = ver;
	}
	
	
	
	private static void print(Object msg){
		System.out.println(msg);
	}
	private static void printError(Object msg){
		System.err.println(msg);
	}
	

	
}



