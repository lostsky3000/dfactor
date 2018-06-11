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

public final class DFJsPageModel implements IScriptAnalyzer{
	public static final String SFX = "jssp";
	private static final String TAG_BEGIN = "<?"+SFX;
	private static final int TAG_BEGIN_LEN = TAG_BEGIN.length();
	private static final String TAG_END = "?>";
	private static final int TAG_END_LEN = TAG_END.length();
	private static final String TXT_ECHO_FUNC = "asdw234SFfii";
	private static final String JS_INNER_API_NAME = "kakajsapi";
	public static final String JS_API_NAME = "uqOdapil";
	public static final String JS_QUERY_IT = "Undk12ual";
	public static final String JS_HEADER_IT = "dHjq34tikp";
	public static final String JS_IS_GET = "k23sDf73op";
	
	public final String name;
	private CompiledScript cs = null;
	private StringBuilder sbOut = null;
	private StringBuilder sbJs = null;
	private HashMap<Integer, String> mapTxt = null;
	private int version = 0;
	
	public DFJsPageModel(String name) {
		this.name = name;
		sbOut = new StringBuilder();
		sbJs = new StringBuilder();
		mapTxt = new HashMap<>();
	}
	private static final String JS_HEAD_CODE = 
			"var g_213ddx="+JS_INNER_API_NAME+";\n"
			+"var "+TXT_ECHO_FUNC+"=function(id){g_213ddx.txtCall(id);};\n"
			//global api
			+"var echo=function(msg){g_213ddx.scriptCall(msg);};\n"
			+"var g_kksQ4euHf="+JS_HEADER_IT+";\n"
			+"var _HEADER=g_fn_wrap_query(g_kksQ4euHf);\n"
			+"var g_lq0Ud3i="+JS_QUERY_IT+";\n"
			+"var _GET={};var _POST={};\n"
			+"var g_s0Uy3Uj="+JS_IS_GET+";\n"
			+"if(g_s0Uy3Uj){_GET=g_fn_wrap_query(g_lq0Ud3i);}else{_POST=g_fn_wrap_query(g_lq0Ud3i);}\n"
			+"var df="+JS_API_NAME+";\n";
	public boolean analyze(StringBuilder sb, Compilable compiler){
		boolean bRet = false;
		do {
			//reset
			sbJs.setLength(0);
			sbJs.append("var ").append(name).append("=function(){\n")
				.append(JS_HEAD_CODE);
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
			//call
			sbJs.append("};\n").append(name).append("();");
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
	
	public String execute(Bindings bind){
		if(cs != null){
			sbOut.setLength(0);
			try {
				bind.put(JS_INNER_API_NAME, (IScriptAnalyzer)this);
				cs.eval(bind);
				return sbOut.toString();
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public void scriptCall(Object content) {
		if(content instanceof Double){
			sbOut.append(((Double)content).intValue());
		}else{
			sbOut.append(content);
		}
	}
	@Override
	public void txtCall(Integer txtId) {
		final String str = mapTxt.get(txtId);
		if(str != null){
			sbOut.append(str);
		}else{ //error, impossible
			
		}
	}
	public int getVersion(){
		return version;
	}
	public void setVersion(int ver){
		this.version = ver;
	}
	
	public static void main(String[] args) {
		
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engJs = mgr.getEngineByName("JavaScript");
		Compilable compiler = (Compilable)engJs;
		
//		try {
//			CompiledScript cs1 = compiler.compile("var test1 = 1;");
//			CompiledScript cs2 = compiler.compile("print(test1);");
//			cs1.eval();
//			cs2.eval();
//		} catch (ScriptException e1) {
//			e1.printStackTrace();
//		}
		
		
		String srcPath = "F:/dev/guaji/svn/program/server/src/dfactorjs/runtime/example/webroot/test.html";
		
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(srcPath));
			String line = null;
			
			while( (line=br.readLine()) != null ){
				sb.append(line).append("\n");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//
		DFJsPageModel mod = new DFJsPageModel("index");
		mod.analyze(sb, compiler);
		Bindings bind = new SimpleBindings();
		bind.put("api", 1);
		mod.execute(bind);
		
//		_compareJsCompile(engJs, src);
	}
	
	
	private static void _compareJsCompile(ScriptEngine engJs, String src){
		Compilable cEng = (Compilable) engJs;
		CompiledScript cs = null;
		try {
			cs = cEng.compile(src);
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//
		long tmStart = 0;
		int cycleNum = 1000;
		for(int i=0; i<10; ++i){
			tmStart = System.currentTimeMillis();
			for(int j=0; j<cycleNum; ++j){
				try {
					cs = cEng.compile(src);
//					cs.eval();
				} catch (ScriptException e) {
					e.printStackTrace();
				}
			}
			print("cs tmCost = "+(System.currentTimeMillis() - tmStart));
			//
			tmStart = System.currentTimeMillis();
			for(int j=0; j<cycleNum; ++j){
				try {
					engJs.eval(src);
				} catch (ScriptException e) {
					e.printStackTrace();
				}
			}
			print("ori tmCost = "+(System.currentTimeMillis() - tmStart));
		}
	}
	
	
	private static void print(Object msg){
		System.out.println(msg);
	}

	
}



