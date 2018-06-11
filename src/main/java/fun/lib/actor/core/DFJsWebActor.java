package fun.lib.actor.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import com.funtag.util.script.DFJsPageModel;
import fun.lib.actor.api.DFTcpChannel;
import fun.lib.actor.api.IWebScriptAPI;
import fun.lib.actor.po.DFPageInfo;
import io.netty.handler.codec.http.HttpMethod;

public final class DFJsWebActor extends DFActor implements IWebScriptAPI{
	
	public DFJsWebActor(Integer id, String name, Boolean isBlockActor) {
		super(id, name, isBlockActor);
		// TODO Auto-generated constructor stub
	}
	
	private DFActorManagerJs _mgrJs = null;
	private String _webRoot = null;
	private String _webRootAbs = null;
	private String _sfx = null;
	private String _curUrlRelPath = null;
	
	private Bindings _bindJs = null;
//	private Compilable _compiler = null;
//	private ScriptEngine _engJs = null;
	private String _strInitJs = null;
	
	@Override
	public void onStart(Object param) {
		_mgrJs = DFActorManagerJs.get();
		Map<String,String> mapParam = (Map<String, String>) param;
		_webRoot = mapParam.get("webRoot");
		_sfx = "."+mapParam.get("sfx");
		_strInitJs = mapParam.get("initJs");
		_webRootAbs = _mgrJs.getAbsRunDir() + File.separator+_webRoot;
		//
		_bindJs = new SimpleBindings();
		_bindJs.put(DFJsPageModel.JS_API_NAME, (IWebScriptAPI)this);
		//
	}
	
//	private final ThreadLocal<Compilable> _thCompiler = new ThreadLocal<>();
	private Compilable _compiler = null;
	private Compilable _getCompiler(){
//		Compilable c = _thCompiler.get();
//		if(c == null){
//			c = (Compilable) new ScriptEngineManager().getEngineByName("JavaScript");
//			_thCompiler.set(c);
//			try {
//				CompiledScript cs = c.compile(_strInitJs);
//				cs.eval();
//			} catch (ScriptException e) {
//				e.printStackTrace();
//			}
//		}
//		return c;
		if(_compiler == null){
			ScriptEngine eng = new ScriptEngineManager().getEngineByName("JavaScript");
			_compiler = (Compilable) eng;
			try {
				CompiledScript cs = _compiler.compile(_strInitJs);
				cs.eval();
				_bindJs.put("g_fn_wrap_query", eng.get("g_fn_wrap_query"));
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
		return _compiler;
	}
	
	@Override
	public int onTcpRecvMsg(int requestId, DFTcpChannel channel, Object msg) {
		DFHttpSvrReqWrap req = (DFHttpSvrReqWrap) msg;
		
		String uri = req.getUri();
		int uriType = _parseUri(uri);
		if(uriType == URI_CODE){
			_procCode(_curUrlRelPath, req);
		}else if(uriType == URI_INDEX){
			_procRes("/index.html", req);
		}else{  //res
			_procRes(_curUrlRelPath, req);
		}
		
//		String thName = Thread.currentThread().getName();
//		log.info("recv req, uri="+uri+", th="+thName);
//		req.response("rsp from svr, th="+thName).send();
		
		return 0;
	}
	
	private final HashMap<String,DFJsPageModel> _mapPage = new HashMap<>();
//	private final ThreadLocal<Map<String,DFJsPageModel>> _thMapPage = new ThreadLocal<>();
	private Map<String,DFJsPageModel> _getMapPage(){
//		Map<String,DFJsPageModel> map = _thMapPage.get();
//		if(map == null){
//			map = new HashMap<>();
//			_thMapPage.set(map);
//		}
//		return map;
		
		return _mapPage;
	}
	
	private DFJsPageModel _curReqPage = null;
	private boolean _procCode(String relPath, DFHttpSvrReqWrap req){
		boolean bRet = false;
		do {
			String pageId = relPath.replaceAll("/", "_");
			Map<String,DFJsPageModel> _mapPage = _getMapPage();
			_curReqPage = _mapPage.get(pageId);
			if(_curReqPage == null){
				File f = new File(_webRootAbs + relPath + "." + DFJsPageModel.SFX);
				String absPath = f.getAbsolutePath();
				int ret = _doCompile(absPath, pageId);
				if(ret != 200){  //failed
					req.response(ret).send();
					break;
				}
				DFPageInfo pageInfo = _mgrJs.getPageInfo(absPath);
				if(pageInfo != null){
					_curReqPage.setVersion(pageInfo.getVersion());
				}
			}else{  //check version
				File f = new File(_webRootAbs + relPath + "." + DFJsPageModel.SFX);
				String absPath = f.getAbsolutePath();
				DFPageInfo pageInfo = _mgrJs.getPageInfo(absPath);
				if(pageInfo == null){  //not exist
					_mapPage.remove(pageId);
					req.response(404).send();
					break;
				}else{
					int verNew = pageInfo.getVersion();
					if(_curReqPage.getVersion() != verNew){ //expire, re-compile
						int ret = _doCompile(absPath, pageId);
						if(ret != 200){  //failed
							req.response(ret).send();
							break;
						}else{ //succ
							_curReqPage.setVersion(verNew);
						}
					}
				}
			}
			
			//set reqdata
			_bindJs.put(DFJsPageModel.JS_HEADER_IT, req.getHeaderIterator());
			if(req.getMethod() == HttpMethod.POST){
				_bindJs.put(DFJsPageModel.JS_IS_GET, false);
			}else{
				_bindJs.put(DFJsPageModel.JS_IS_GET, true);
			}
			_bindJs.put(DFJsPageModel.JS_QUERY_IT, req.getQueryDataIterator());
			
			//	
			final String out = _curReqPage.execute(_bindJs);
			if(out != null){
				req.response(out)
					.contentType("text/html;charset=utf-8")
					.send();
			}else{ //err
				log.error("execute "+DFJsPageModel.SFX+" failed: "+relPath);
				req.response(505).send();
				break;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	
	private int _doCompile(String srcPath, String pageId){
		int ret = _loadFile(srcPath);
		if(ret != 0){
			log.error("load "+DFJsPageModel.SFX+" failed: "+srcPath);
			return ret==1?404:505;
		}
		boolean first = false;
		if(_curReqPage == null){
			_curReqPage = new DFJsPageModel(pageId);
			first = true;
		}
		if(!_curReqPage.analyze(_sb, _getCompiler())){
			log.error("analyze "+DFJsPageModel.SFX+" failed: "+srcPath);
			return 505;
		}else{ //succ
			if(first){
				Map<String,DFJsPageModel> _mapPage = _getMapPage();
				_mapPage.put(pageId, _curReqPage);
			}
		}
		return 200;
	}
	
	private boolean _procRes(String relPath, DFHttpSvrReqWrap req){
		boolean bRet = false;
		do {
			req.response(404).send();
			bRet = true;
		} while (false);
		return bRet;
	}
	
	private int _parseUri(String uri){
		if(uri.equals("/")){ //index
			return URI_INDEX;
		}
		int tmp = uri.lastIndexOf(47);
		if(tmp >= 0){
			int tmp2 = uri.indexOf(_sfx, tmp+1);
			if(tmp2 > 0){
				_curUrlRelPath = uri.substring(0, tmp2);
				return URI_CODE;
			}
		}
		_curUrlRelPath = uri;
		return URI_RES;
	}
	
	private StringBuilder _sb = new StringBuilder();
	private int _loadFile(String path){
		BufferedReader br = null;
		try{
			File f = new File(path);
			if(!f.exists()){
				return 1;
			}
			br = new BufferedReader(new FileReader(f));
			String line = null;
			_sb.setLength(0);
			while( (line=br.readLine()) != null ){
				_sb.append(line).append("\n");
			}
			return 0;
		}catch(Throwable e){
			e.printStackTrace();
			return 2;
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static final int URI_INDEX = 0;
	private static final int URI_CODE = 1;
	private static final int URI_RES = 2;
	
}








