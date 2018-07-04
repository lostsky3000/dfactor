package com.funtag.util.script;

import java.util.HashMap;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import fun.lib.actor.core.IVirtualHost;

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
//	private StringBuilder sbJs = null;
	private HashMap<Integer, String> mapTxt = null;
	private int version = 0;
	
	//
	
	public DFJsPageModel(String name, String dir, String file) {
		this.name = name;
		this.dir = dir;
		this.file = file;
//		sbJs = new StringBuilder();
		mapTxt = new HashMap<>();
	}
	
	
	public boolean analyze(StringBuilder sb, Compilable compiler) throws Throwable{
		boolean bRet = false;
		do {
			//reset
			
			StringBuilder sbJs = new StringBuilder();
			int txtCount = 0;
			mapTxt.clear();
			try{
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
							if(_isValidEndTag(sb, subBegin + TAG_BEGIN_LEN, tmp)){
								curSec = sb.substring(subBegin + TAG_BEGIN_LEN, tmp);
								idxSearch = tmp + TAG_END_LEN;
								subBegin = idxSearch;
								subEnd = -2;
								isJs = true;
								jsStart = false;
							}else{  //invalid endTag
								subEnd = -2;
								idxSearch = tmp + TAG_END_LEN;
							}
							
//							try{
//								compiler.compile(curSec);
//								//compile succ
//								idxSearch = tmp + TAG_END_LEN;
//								subBegin = idxSearch;
//								subEnd = -2;
//								isJs = true;
//								jsStart = false;
//							}catch(Throwable e){ //compile failed
//								e.printStackTrace();
//								curSec = null;
//								idxSearch = tmp + TAG_END_LEN;
//								if(idxSearch > srcLen - TAG_END_LEN){ //end
//									subEnd = srcLen;
//									idxSearch = srcLen;
//								}
//							}
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
				cs = null;
				cs = compiler.compile(sbJs.toString());
			}catch(Throwable e){
				e.printStackTrace();
				throw e;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	
	private boolean _isValidEndTag(StringBuilder sb, int startPos, int endPos){
		int stat = STAT_FREE;
		char ch = 0;
		boolean quotIsSingle = false;
		endPos = endPos - 1;
		for(int i=startPos; i<=endPos; ++i){
			ch = sb.charAt(i);
			if(stat == STAT_FREE){
				if(ch=='/' && i<endPos && sb.charAt(i+1)=='*'){  //comment
					stat = STAT_COMMENT;
					++ i;
				}else if(ch == '"'){
					stat = STAT_QUOT; quotIsSingle = false;
				}else if(ch == '\''){
					stat = STAT_QUOT; quotIsSingle = true;
				}
			}else if(stat == STAT_COMMENT){
				if(ch=='*' && i<endPos && sb.charAt(i+1)=='/'){
					stat = STAT_FREE;
					++ i;
				}
			}else if(stat == STAT_QUOT){
				if(quotIsSingle){
					if(ch == '\''){
						stat = STAT_FREE;
					}
				}else{
					if(ch == '"'){
						stat = STAT_FREE;
					}
				}
			}
		}
		return stat==STAT_FREE?true:false;
	}
	private static final int STAT_FREE = 1;
	private static final int STAT_COMMENT = 2;  // 处于注释内
	private static final int STAT_QUOT = 3;   //处于引号内
	
	public boolean execute(Bindings bind, StringBuilder sbOut, DFJsPageModel parent) throws Throwable{
		if(cs != null){
			curSbOut = sbOut;
			try {
				cs.eval(bind);
				curSbOut = null;
				if(parent != null){ //has parent, reset
					bind.put(IVirtualHost.JS_INNER_ANALYZER, parent);
					bind.put(IVirtualHost.JS_DIR, parent.dir);
					bind.put(IVirtualHost.JS_FILE, parent.file);
					bind.put(IVirtualHost.JS_PAGE_ID, parent.name);
				}
				return true;
			} catch (Throwable e) {
				e.printStackTrace();
				throw e;
			}
		}
		return false;
	}
	
	@Override
	public void _scriptCall(Object content) {
		if(content instanceof Double){
			curSbOut.append(((Double)content).intValue());
		}else{
			curSbOut.append(content);
		}
	}
	@Override
	public void _textCall(Integer txtId) {
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



