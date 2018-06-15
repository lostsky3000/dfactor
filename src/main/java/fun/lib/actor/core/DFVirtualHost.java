package fun.lib.actor.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.funtag.util.cipher.DFCipherUtil;
import com.funtag.util.fs.DFFileMonitor;
import com.funtag.util.fs.IFSMonitor;
import com.funtag.util.script.DFJsPageModel;
import com.funtag.util.script.DFWebResModel;
import com.funtag.util.script.IScriptAnalyzer;
import com.funtag.util.system.DFSysUtil;

import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.api.http.DFHttpContentType;
import fun.lib.actor.api.http.DFHttpSvrReq;
import fun.lib.actor.api.http.DFHttpSvrRsp;
import fun.lib.actor.po.DFPageInfo;
import fun.lib.actor.po.DFWebResInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public final class DFVirtualHost {
	public static final String RSP_SERVER_NAME = "dfactor-js";
	private static final String SID_NAME = "JSSESSIONID";
	private static final int SID_NAME_LEN = SID_NAME.length();
	
	private final Object cfg;
	private final String runDir;
	//
	public final int port;
	private volatile File dirWebRoot = null;
	private volatile String webRoot = null;
	private volatile String[] _arrIndex = null;
	private volatile int _idxNum = 0;
	private String sfx = null;
	private String uriSfx = null;
	private final String strInitJs;
	private final DFVirtualHostManager _mgrHost;
	private final int expireTime;
	
	public DFVirtualHost(int port, Object cfg, String runDir, String initJs) {
		this.port = port;
		this.runDir = runDir;
		this.cfg = cfg;
		this.strInitJs = initJs;
		_mgrHost = DFVirtualHostManager.get();
		expireTime = 1000*60*20;
	}
	
	public void onMsg(DFHttpSvrReqWrap req, DFJsWebActor ctx){
		long tmNow = System.currentTimeMillis();
		//check expire session
		UserSession s = _lsUserSession.peek();
		if(s != null){
			if(tmNow - s.createTime >= expireTime){ //expire
				_lsUserSession.poll();
				if(!s.hasRemoved){  //remove from map
					_removeSession(s.sid);
				}
			}
		}		
		//
		Bindings bind = _getBind();
		bind.clear();
		bind.put(JS_DF_NAME, (IWebExtAPI)ctx);
		String uri = req.getUri();
		TmpResult ret = new TmpResult();
		int uriType = _parseUri(uri, ret);
		if(uriType == URI_CODE){
			_procCode(ret.strRet, req, ctx.log, tmNow);
		}else if(uriType == URI_INDEX){
//			_procRes("/index.html", req, ctx.log);
			boolean bingo = false;
			int count = 0;
			while(count < _idxNum){
				String idxName = _arrIndex[count++];
				String absPath = webRoot + File.separator + idxName;
				File f = new File(absPath);
				if(f.exists() && f.isFile()){
					bingo = true;
					req.response(302).header("Location", idxName).send();
					break;
				}
			}
			if(!bingo){
				req.response(404).send();
			}
		}else{  //res
			_procRes(ret.strRet, req, ctx.log);
		}
	}
	
	private int _parseUri(String uri, TmpResult ret){
		if(uri.equals("/")){ //index
			return URI_INDEX;
		}
		int tmp = uri.lastIndexOf(47);
		if(tmp >= 0){
			int tmp2 = uri.indexOf(this.uriSfx, tmp+1);
			if(tmp2 > 0){
				ret.strRet = uri.substring(0, tmp2);
				return URI_CODE;
			}
		}
		ret.strRet = uri;
		return URI_RES;
	}
	
	
	public static final String JS_INNER_ANALYZER = "df_kadajsapi";
	public static final String JS_DF_NAME = "df_uqO3k8aRl";
	public static final String JS_QUERY_IT = "df_Undk12ual";
	public static final String JS_HEADER_IT = "df_dHjq34tikp";
	public static final String JS_SESSION_IT = "df_mNE7rMpqk";
	public static final String JS_IS_GET = "df_k23sDf73op";
	public static final String JS_API_NAME = "df_Jk17DuRoqp";
	public static final String JS_DIR = "_DIR_";
	public static final String JS_FILE = "_FILE_";
	public static final String JS_PAGE_ID = "_PAGE_ID_";
	public static final String JS_INPUT = "_INPUT";
	
	private static final String JS_HEAD_CODE = 
			"var "+DFJsPageModel.TXT_ECHO_FUNC+"=function(id){"+JS_INNER_ANALYZER+".txtCall(id);};\n"
			//global api
			+"var _SESSION=null;\n"
			+"var session_start=function(){_SESSION="+JS_API_NAME+".onSessionStart({});};\n"
			+"var session_destroy=function(){"+JS_API_NAME+".onSessionDestroy(_SESSION);};\n"
			+"var session_id=function(){return "+JS_API_NAME+".onSessionId();};\n"
			+"var require=function(fileName){"+JS_API_NAME+".requireFile(_DIR_, fileName, _PAGE_ID_);};\n"
			+"var header=function(key,val){"+JS_API_NAME+".header(key,val);}\n"
			+"var echo=function(msg){"+JS_INNER_ANALYZER+".scriptCall(msg);};\n"
			+"var echoln=function(msg){"+JS_INNER_ANALYZER+".scriptCall(msg+'<br/>');};\n"
			+"var _HEADER=g_fn_wrap_query("+JS_HEADER_IT+");\n"
			+"var _GET={};var _POST={};\n"
			+"if("+JS_IS_GET+"){_GET=g_fn_wrap_query("+JS_QUERY_IT+");}else{_POST=g_fn_wrap_query("+JS_QUERY_IT+");}\n"
			+"var df="+JS_DF_NAME+";\n";
	private boolean _procCode(String relPath, DFHttpSvrReqWrap req, final DFActorLog log, final long tmNow){
		boolean bRet = false;
		do {
			String pageId = relPath; //relPath.replaceAll("/", "_");
			Map<String,DFJsPageModel> _mapPage = _getMapPage();
			DFJsPageModel tmpPage = _mapPage.get(pageId);
			TmpResult result = new TmpResult();
			result.obj = tmpPage;
			if(tmpPage == null){
				String absPath = webRoot + relPath + "." + DFJsPageModel.SFX;
				int ret = _doCompile(absPath, pageId, result, log);
				if(ret != 200){  //failed
					req.response(ret).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
					break;
				}
				DFPageInfo pageInfo = getPageInfo(absPath);
				if(pageInfo != null){
					((DFJsPageModel)result.obj).setVersion(pageInfo.getVersion());
				}
			}else{  //check version
				String absPath = webRoot + relPath + "." + DFJsPageModel.SFX;
				DFPageInfo pageInfo = getPageInfo(absPath);
				if(pageInfo == null){  //not exist
					_mapPage.remove(pageId);
					req.response(404).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
					break;
				}else{
					int verNew = pageInfo.getVersion();
					if(tmpPage.getVersion() != verNew){ //expire, re-compile
						int ret = _doCompile(absPath, pageId, result, log);
						if(ret != 200){  //failed
							req.response(ret).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
							break;
						}else{ //succ
							((DFJsPageModel)result.obj).setVersion(verNew);
						}
					}
				}
			}
			//set reqdata
			final String reqCookie = req.getHeaderValue(HttpHeaderNames.COOKIE.toString());
			final String reqSid = _parseSessionId(reqCookie);
			Bindings bind = _getBind();
			bind.put(JS_HEADER_IT, req.getHeaderIterator());
			if(req.getMethod() == HttpMethod.POST){
				bind.put(JS_IS_GET, false);
				Object objContent = req.getApplicationData();
				if(objContent != null){  //has content
					if(req.contentIsStr()){ //string
						bind.put(JS_INPUT, (String)objContent);
					}else{ //buf
						DFJsBuffer bufWrap = DFJsBuffer.newBuffer((ByteBuf)objContent);
						bind.put(JS_INPUT, (IScriptBuffer)bufWrap);
					}
				}else{
					bind.put(JS_INPUT, null);
				}
			}else{
				bind.put(JS_INPUT, null);
				bind.put(JS_IS_GET, true);
			}
			bind.put(JS_QUERY_IT, req.getQueryDataIterator());
			//
			final ReqSession reqSession = _getReqSession();
			reqSession.reset();
			WebScriptAPIWrap webApi = _getWebApi();
			webApi.reqSession = reqSession; webApi.reqSid = reqSid;
			webApi.tmNow = tmNow; webApi.log = log;
			webApi.mapHeader.clear();
			//
			bind.put(JS_API_NAME, (IWebScriptAPI)webApi);
			DFJsPageModel curPage = (DFJsPageModel)result.obj;
			bind.put(JS_INNER_ANALYZER, (IScriptAnalyzer)curPage);
			bind.put(JS_DIR, curPage.dir);
			bind.put(JS_FILE, curPage.file);
			bind.put(JS_PAGE_ID, curPage.name);
			try {
				_getCsHead().eval(bind);
			} catch (ScriptException e) {
				e.printStackTrace();
			}
			StringBuilder sbOut = _getSbOut();
			sbOut.setLength(0);
			final boolean execRet = curPage.execute(bind, sbOut, null);
			//check session begin
			if(reqSession.sessionStart && reqSession.userSession != null && reqSession.jsSession != null){
				Map<String, Object> mir = reqSession.jsSession;
				synchronized (reqSession.userSession) {
					reqSession.userSession.mapAttr.putAll(mir);
				}
			}
			//check session end
			final String out;
			if(execRet){
				out = sbOut.toString();
			}else{
				out = null;
			}
			if(out != null){
				DFHttpSvrRsp rsp = req.response(out);
				if(webApi.mapHeader.isEmpty()){
					rsp.contentType("text/html;charset=utf-8");
				}else{
					Iterator<Entry<String,String>> itPageHeader = webApi.mapHeader.entrySet().iterator();
					while(itPageHeader.hasNext()){
						Entry<String,String> en = itPageHeader.next();
						rsp.header(en.getKey(), en.getValue());
					}
					if(!webApi.mapHeader.containsKey(HttpHeaderNames.CONTENT_TYPE.toString())){
						rsp.contentType("text/html;charset=utf-8");
					}	
				}
				if(reqSession.userSession != null){ //	
					rsp.header(HttpHeaderNames.SET_COOKIE.toString(), SID_NAME+"="+reqSession.userSession.sid+"; max-age="+expireTime);
				}
				rsp.header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME)
					.header(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString())
					.send();
			}else{ //err
				log.error("execute "+DFJsPageModel.SFX+" failed: "+relPath);
				req.response(500).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
				break;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	private String _createSessionId(long now){
		return DFCipherUtil.getMD5(now+"");
	}
	private String _parseSessionId(String cookie){
		if(cookie != null){
			int tmp = cookie.indexOf(SID_NAME);
			if(tmp >= 0){
				int tmp2 = cookie.indexOf(59, tmp + SID_NAME_LEN + 1);
				if(tmp2 > 0){
					return cookie.substring(tmp + SID_NAME_LEN + 1, tmp2);
				}else{
					return cookie.substring(tmp + SID_NAME_LEN + 1).trim();
				}
			}
		}
		return null;
	}
	private int _doCompile(String srcPath, String pageId, TmpResult result, DFActorLog log){
		StringBuilder sb = _getSb();
		File f = new File(srcPath);
		int ret = _loadFile(f, sb);
		if(ret != 0){
			log.error("load "+DFJsPageModel.SFX+" failed: "+srcPath);
			return ret==1?404:500;
		}
		boolean first = false;
		if(result.obj == null){
			result.obj = new DFJsPageModel(pageId, f.getParent(), f.getAbsolutePath());
			first = true;
		}
		if(!((DFJsPageModel)result.obj).analyze(sb, _getCompiler())){
			log.error("analyze "+DFJsPageModel.SFX+" failed: "+srcPath);
			return 500;
		}else{ //succ
			if(first){
				Map<String,DFJsPageModel> _mapPage = _getMapPage();
				_mapPage.put(pageId, (DFJsPageModel)result.obj);
			}
		}
		return 200;
	}
	
	private final ThreadLocal<StringBuilder> _thSbOut = new ThreadLocal<>();
	private StringBuilder _getSbOut(){
		StringBuilder sb = _thSbOut.get();
		if(sb == null){
			sb = new StringBuilder();
			_thSbOut.set(sb);
		}
		return sb;
	}
	
	private final ThreadLocal<StringBuilder> _thSb = new ThreadLocal<>();
	private StringBuilder _getSb(){
		StringBuilder sb = _thSb.get();
		if(sb == null){
			sb = new StringBuilder();
			_thSb.set(sb);
		}
		return sb;
	}
	
	private int _loadFile(File f, StringBuilder sb){
		BufferedReader br = null;
		try{
			if(!f.exists()){
				return 1;
			}
			br = new BufferedReader(new FileReader(f));
			String line = null;
			sb.setLength(0);
			while( (line=br.readLine()) != null ){
				sb.append(line).append("\n");
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
	private boolean _procRes(String relPath, DFHttpSvrReqWrap req, DFActorLog log){
		boolean bRet = false;
		do {
			if(req.getMethod() != HttpMethod.GET){
				req.response(500).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
				break;
			}
			//
			ByteBuf bufOut = null;
			String absPath = webRoot + relPath;
			String contentType = parseContentTypeByName(relPath, null);
			if(isWebResByName(relPath)){  //属于被缓存类型的资源
				String resId = relPath;
				Map<String,DFWebResModel> mapRes = _getMapRes();
				DFWebResModel resModel = mapRes.get(resId);
				if(resModel == null){  //不存在, 加载
					DFWebResModel res = new DFWebResModel(absPath);
					if(res.load(log)){  //加载成功
						mapRes.put(resId, res);
						DFWebResInfo info = _getResInfo(absPath);
						if(info != null){  //跟文件变动记录保持一致
							res.setVersion(info.getVersion());
						}
						res.setContentType(contentType);
						bufOut = PooledByteBufAllocator.DEFAULT.ioBuffer(res.getBufLen());
						bufOut.writeBytes(res.getBufOut());
					}else{  //加载失败
						req.response(404).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
						break;
					}
				}else{  //缓存存在，检查文件变动记录
					DFWebResInfo info = _getResInfo(absPath);
					if(info == null){  //文件已不存在，删除缓存
						mapRes.remove(resId);
						req.response(404).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
						break;
					}else{ 
						int newVersion = info.getVersion();
						if(resModel.getVersion() != newVersion){  //缓存和变动记录版本不一致，重新加载
							resModel.load(log);
							resModel.setVersion(newVersion); //保持版本一致
						}
						bufOut = PooledByteBufAllocator.DEFAULT.ioBuffer(resModel.getBufLen());
						bufOut.writeBytes(resModel.getBufOut());
					}
				}
			}else{  //不属于缓存资源，直接io
				File f = new File(absPath);
				if(!f.exists()){
					req.response(404).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
					break;
				}
				BufferedInputStream bis = null;
				try{
					byte[] tmpBuf = _getTmpBytes();
					bis = new BufferedInputStream(new FileInputStream(f));
					int total = bis.available();
					bufOut = PooledByteBufAllocator.DEFAULT.ioBuffer(total);
					int read = 0;
					while( (read=bis.read(tmpBuf)) > 0 ){
						bufOut.writeBytes(tmpBuf, 0, read);
					}
				}catch(Throwable e){
					e.printStackTrace();
					req.response(500).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
					break;
				}finally{
					if(bis != null){
						try {
							bis.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			if(bufOut == null){
				req.response(500).header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME).send();
				break;
			}
			//rsp
			req.response(bufOut)
				.contentType(contentType)
				.header(HttpHeaderNames.CONTENT_LENGTH.toString(), bufOut.readableBytes()+"")
//				.header(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString())
				.header(HttpHeaderNames.SERVER.toString(), RSP_SERVER_NAME)
				.send();
			bRet = true;
		} while (false);
		return bRet;
	}
	
	
	
	private final ThreadLocal<byte[]> _thTmpBytes = new ThreadLocal<>();
	private byte[] _getTmpBytes(){
		byte[] bytes = _thTmpBytes.get();
		if(bytes == null){
			bytes = new byte[1024];
			_thTmpBytes.set(bytes);
		}
		return bytes;
	}
	
	public boolean init(DFActorLog log){
		boolean bRet = false;
		do {
			try{
				ScriptObjectMirror mir = (ScriptObjectMirror) cfg;
				String webRoot = ((String)mir.get("root")).trim();
				if(!webRoot.startsWith("/") && !webRoot.matches("[a-zA-Z]{1}:")){ //relative path
					webRoot = runDir + File.separator + webRoot;
				}
				dirWebRoot = new File(webRoot);
				this.webRoot = dirWebRoot.getAbsolutePath();
				this.webRoot = this.webRoot.replaceAll("\\\\", "/");
				if(!dirWebRoot.exists() || !dirWebRoot.isDirectory()){
					log.error("invalid webRoot: "+dirWebRoot.getAbsolutePath()); 
					break;
				}
				sfx = mir.containsKey("sfx")?((String)mir.get("sfx")).trim():DFJsPageModel.SFX;
				uriSfx = "."+sfx;
				if(mir.containsKey("index")){
					ScriptObjectMirror arr = (ScriptObjectMirror) mir.get("index");
					int size = arr.values().size();
					if(size > 0){
						_arrIndex = new String[size];
						_idxNum = 0;
						for(Object str : arr.values()){
							_arrIndex[_idxNum++] = ((String) str).trim();
						}		
					}
				}
				if(_idxNum <= 0){  //没有指定默认index
					_idxNum = 2;
					_arrIndex = new String[]{"index.html", "index.jssp"};
				}
			}catch(Throwable e){
				e.printStackTrace(); 
				break;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	//
	private final ConcurrentHashMap<String, DFWebResInfo> _mapResInfo = new ConcurrentHashMap<>();
	private void _addResInfo(String key, DFWebResInfo info){
		if(DFSysUtil.getOSType() == DFSysUtil.OS_WINDOWS){
			key = key.replaceAll("\\\\", "/");
		}
		_mapResInfo.put(key, info);
	}
	private void _removeResInfo(String key){
		if(DFSysUtil.getOSType() == DFSysUtil.OS_WINDOWS){
			key = key.replaceAll("\\\\", "/");
		}
		_mapResInfo.remove(key);
	}
	private DFWebResInfo _getResInfo(String key){
		if(DFSysUtil.getOSType() == DFSysUtil.OS_WINDOWS){
			key = key.replaceAll("\\\\", "/");
		}
		return _mapResInfo.get(key);
	}
	//
	private final ConcurrentHashMap<String, DFPageInfo> _mapPageInfo = new ConcurrentHashMap<>();
	private void _addPageInfo(String key, DFPageInfo p){
		if(DFSysUtil.getOSType() == DFSysUtil.OS_WINDOWS){
			key = key.replaceAll("\\\\", "/");
		}
		_mapPageInfo.put(key, p);
	}
	private void _removePageInfo(String key){
		if(DFSysUtil.getOSType() == DFSysUtil.OS_WINDOWS){
			key = key.replaceAll("\\\\", "/");
		}
		_mapPageInfo.remove(key);
	}
	public DFPageInfo getPageInfo(String key){
		if(DFSysUtil.getOSType() == DFSysUtil.OS_WINDOWS){
			key = key.replaceAll("\\\\", "/");
		}
		return _mapPageInfo.get(key);
	}
	
	public void start(){
		//record exist pages & res
		LinkedList<File> lsWebDir = new LinkedList<>();
		lsWebDir.add(dirWebRoot);
		while(!lsWebDir.isEmpty()){
			File d = lsWebDir.removeFirst();
			File[] arrF = d.listFiles();
			if(arrF == null || arrF.length == 0){
				continue;
			}
			for(File f : arrF){
				if(f.isDirectory()){
					lsWebDir.offer(f);
				}else{
					String absPath = f.getAbsolutePath();
					if(absPath.endsWith("."+DFJsPageModel.SFX)){
						DFPageInfo p = new DFPageInfo(absPath);
						p.increaseVersion();
						_addPageInfo(absPath, p);
					}else{
						if(isWebResByName(absPath)){  //监控js和css
							DFWebResInfo info = new DFWebResInfo(absPath);
							info.increaseVersion();
							_addResInfo(absPath, info);
						}
					}
				}
			}
		}
		//start page&res monitor
		final DFFileMonitor monitor = new DFFileMonitor(dirWebRoot, new IFSMonitor() {
			@Override
			public void onModify(File f) {
				if(f.isFile()){
					String absPath = f.getAbsolutePath();
					if(absPath.endsWith("."+DFJsPageModel.SFX)){
						if(!f.exists()){
							_removePageInfo(absPath);
						}else{
							DFPageInfo p = getPageInfo(absPath);
							if(p == null){
								p = new DFPageInfo(absPath);
								_addPageInfo(absPath, p);
							}
							p.increaseVersion();
						}
					}else{
						if(isWebResByName(absPath)){
							if(!f.exists()){
								_removeResInfo(absPath);
							}else{
								DFWebResInfo info = _getResInfo(absPath);
								if(info == null){
									info = new DFWebResInfo(absPath);
									_addResInfo(absPath, info);
								}
								info.increaseVersion();
							}
						}
					}
				}
			}
			@Override
			public void onDelete(File f) {
				if(f.isFile()){
					String absPath = f.getAbsolutePath();
					if(absPath.endsWith("."+DFJsPageModel.SFX)){
						_removePageInfo(absPath);
					}else{ 
						if(isWebResByName(absPath)){
							_removeResInfo(absPath);
						}
					}
				}
			}
			@Override
			public void onCreate(File f) {
				if(f.isFile()){
					String absPath = f.getAbsolutePath();
					if(absPath.endsWith("."+DFJsPageModel.SFX)){
						DFPageInfo p = new DFPageInfo(absPath);
						p.increaseVersion();
						_addPageInfo(absPath, p);
					}else{
						if(isWebResByName(absPath)){
							DFWebResInfo info = new DFWebResInfo(absPath);
							info.increaseVersion();
							_addResInfo(absPath, info);
						}
					}
				}
			}
			@Override
			public void onClose() {
				
			}
		});
		monitor.start();
	}
	
	public int getPort(){
		return this.port;
	}
	public String getSfx(){
		return this.sfx;
	}
	public String getWebRoot(){
		return this.webRoot;
	}
	
	private final ThreadLocal<CompiledScript>_thCsHead = new ThreadLocal<>();
	private CompiledScript _getCsHead(){
		CompiledScript cs = _thCsHead.get();
		if(cs == null){
			Compilable c = _getCompiler();
			try {
				cs = c.compile(strInitJs + JS_HEAD_CODE);
				_thCsHead.set(cs);
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
		return cs;
	}
	
	private final ThreadLocal<ScriptEngine> _thEng = new ThreadLocal<>();
	private ScriptEngine _getEngine(){
		ScriptEngine eng = _thEng.get();
		if(eng == null){
			eng = new ScriptEngineManager().getEngineByName("JavaScript");
			_thEng.set(eng);
		}
		return eng;
	}
	private final ThreadLocal<Compilable> _thCompiler = new ThreadLocal<>();
	private Compilable _getCompiler(){
		Compilable c = _thCompiler.get();
		if(c == null){
			ScriptEngine eng = _getEngine();
			c = (Compilable) eng;
			_thCompiler.set(c);
			try {
				CompiledScript cs = c.compile(strInitJs);
				cs.eval();
				Bindings bind = _getBind();
				bind.put("g_fn_wrap_query", eng.get("g_fn_wrap_query"));
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
		return c;
	}
	//
	private final ThreadLocal<Map<String,DFWebResModel>> _thMapRes = new ThreadLocal<>();
	private Map<String,DFWebResModel> _getMapRes(){
		Map<String,DFWebResModel> map = _thMapRes.get();
		if(map == null){
			map = new HashMap<>();
			_thMapRes.set(map);
		}
		return map;
	}
	//
	private final ThreadLocal<Map<String,DFJsPageModel>> _thMapPage = new ThreadLocal<>();
	private Map<String,DFJsPageModel> _getMapPage(){
		Map<String,DFJsPageModel> map = _thMapPage.get();
		if(map == null){
			map = new HashMap<>();
			_thMapPage.set(map);
		}
		return map;
	}
	//
	private final ThreadLocal<Bindings> _thBind = new ThreadLocal<>();
	private Bindings _getBind(){
		Bindings bind = _thBind.get();
		if(bind == null){
			bind = new SimpleBindings();
			_thBind.set(bind);
		}
		return bind;
	}
	
	private final ThreadLocal<ReqSession> _thReqSession = new ThreadLocal<>();
	private ReqSession _getReqSession(){
		ReqSession s = _thReqSession.get();
		if(s == null){
			s = new ReqSession();
			_thReqSession.set(s);
		}
		return s;
	}
	class ReqSession{
		private boolean sessionStart=false;
		private boolean sessionDestroy=false;
		private UserSession userSession = null;
		private ScriptObjectMirror jsSession = null;
		private ReqSession() {
			// TODO Auto-generated constructor stub
		}
		private void reset(){
			sessionStart = false;
			sessionDestroy = false;
			userSession = null;
			jsSession = null;
		}
	}
	
	private final ConcurrentLinkedQueue<UserSession> _lsUserSession = new ConcurrentLinkedQueue<>();
	//
	private final ReentrantReadWriteLock _lockSession = new ReentrantReadWriteLock();
	private final ReadLock _lockSessionRead = _lockSession.readLock();
	private final WriteLock _lockSessionWrite = _lockSession.writeLock();
	private final HashMap<String, UserSession> _mapSession = new HashMap<>();
	private void _addSesion(UserSession s){
		_lockSessionWrite.lock();
		try{
			_mapSession.put(s.sid, s);
		}finally{
			_lockSessionWrite.unlock();
		}
		_lsUserSession.offer(s);
	}
	private UserSession _getSession(String sid){
		_lockSessionRead.lock();
		try{
			return _mapSession.get(sid);
		}finally{
			_lockSessionRead.unlock();
		}
	}
	private void _removeSession(String sid){
		_lockSessionWrite.lock();
		try{
			UserSession s = _mapSession.remove(sid);
			if(s != null){
				s.hasRemoved = true;
			}
		}finally{
			_lockSessionWrite.unlock();
		}
	}
	
	class UserSession{
		private final String sid;
		private final long createTime;
		private Map<String,Object> mapAttr = new HashMap<>();
		private volatile boolean hasRemoved = false;
		private UserSession(String sid, long time) {
			this.sid = sid;
			this.createTime = time;
		}
	}
	
	private static final int URI_INDEX = 0;
	private static final int URI_CODE = 1;
	private static final int URI_RES = 2;
	
	class TmpResult{
		private String strRet=null;
		private Object obj = null;
		public TmpResult() {
			// TODO Auto-generated constructor stub
		}
	}
	
	private final ThreadLocal<WebScriptAPIWrap> _thWebApi = new ThreadLocal<>();
	private WebScriptAPIWrap _getWebApi(){
		WebScriptAPIWrap api = _thWebApi.get();
		if(api == null){
			api = new WebScriptAPIWrap();
			_thWebApi.set(api);
		}
		return api;
	}
	class WebScriptAPIWrap implements IWebScriptAPI{
		private String reqSid = null;
		private ReqSession reqSession = null;
		private long tmNow = 0;
		private DFActorLog log;
		private Map<String,String> mapHeader = new HashMap<>();
		private WebScriptAPIWrap() {
			// TODO Auto-generated constructor stub
		}
		@Override
		public Object onSessionStart(Object sessionJs) {
			if(reqSession.sessionStart){
				return reqSession.jsSession;
			}
			reqSession.sessionStart = true;
			ScriptObjectMirror mir = (ScriptObjectMirror) sessionJs;
			if(reqSid == null){  //no session, create
				String newSid = _createSessionId(tmNow);
				UserSession s = new UserSession(newSid, tmNow);
				_addSesion(s);
				reqSession.userSession = s;
			}else{  //has sessionId, check
				UserSession s = _getSession(reqSid);
				if(s != null){
					if(tmNow - s.createTime < expireTime){ //not expire
						synchronized (s) {
							mir.putAll(s.mapAttr);  //fill data
						}
						reqSession.userSession = s;
					}else{ //expire
						_removeSession(s.sid);
						s = null;
					}
				}
				if(s == null){
					String newSid = _createSessionId(tmNow);
					s = new UserSession(newSid, tmNow);
					_addSesion(s);
					reqSession.userSession = s;
				}
			}
			reqSession.jsSession = mir;
			return mir;
		}      
		@Override
		public boolean onSessionDestroy(Object sessionJs) { 
			if(reqSession.sessionDestroy){
				return false;
			}
			if(sessionJs != null){
				reqSession.sessionDestroy = true;
				ScriptObjectMirror mir = (ScriptObjectMirror) sessionJs;
				mir.clear();
				if(reqSid != null){
					_removeSession(reqSid);
				}
				if(reqSession.userSession != null){
					_removeSession(reqSession.userSession.sid);
					reqSession.userSession = null;
				}
			}
			reqSession.jsSession = null;
			return true;
		}
		@Override
		public String onSessionId() {
			return reqSession.sessionStart?reqSid:null;
		}
		@Override
		public void requireFile(String curDir, String fileName, String parentPageId) {	
			do {
				if(!fileName.endsWith(DFJsPageModel.SFX)){
					log.error("invalid require file: "+fileName);
					break;
				}
				if(DFSysUtil.getOSType() == DFSysUtil.OS_WINDOWS){
					curDir = curDir.replaceAll("\\\\", "/");
				}
				int tmp = -1;
				while( (tmp=fileName.indexOf("../")) >= 0 ){
					fileName = fileName.substring(tmp+3);
					tmp = curDir.lastIndexOf("/");
					if(tmp >= 0){
						curDir = curDir.substring(0, tmp);
					}
				}
				String absPath = curDir + "/" + fileName;
				String reqPageId = absPath.replaceAll(webRoot, "").replaceAll("."+DFJsPageModel.SFX, "");
				if(reqPageId.equalsIgnoreCase(parentPageId)){  //require self
					log.error("cycle require: "+absPath);
					break;
				}
				//
				Map<String,DFJsPageModel> mapPage = _getMapPage();
				DFJsPageModel tmpPage = mapPage.get(reqPageId);
				TmpResult result = new TmpResult();
				result.obj = tmpPage;
				if(tmpPage == null){
					int ret = _doCompile(absPath, reqPageId, result, log);
					if(ret != 200){  //failed
						log.error("parse page failed(1): "+absPath);
						break;
					}
					DFPageInfo pageInfo = getPageInfo(absPath);
					if(pageInfo != null){
						((DFJsPageModel)result.obj).setVersion(pageInfo.getVersion());
					}
				}else{  //check version
					DFPageInfo pageInfo = getPageInfo(absPath);
					if(pageInfo == null){  //not exist
						mapPage.remove(reqPageId);
						log.error("page not found: "+absPath);
						break;
					}else{
						int verNew = pageInfo.getVersion();
						if(tmpPage.getVersion() != verNew){ //expire, re-compile
							int ret = _doCompile(absPath, reqPageId, result, log);
							if(ret != 200){  //failed
								log.error("parse page failed(2): "+absPath);
								break;
							}else{ //succ
								((DFJsPageModel)result.obj).setVersion(verNew);
							}
						}
					}
				}
				//execute
				DFJsPageModel curPage = (DFJsPageModel)result.obj;
				Bindings bind = _getBind();
				DFJsPageModel parent = (DFJsPageModel) bind.get(JS_INNER_ANALYZER);
				bind.put(JS_INNER_ANALYZER, (IScriptAnalyzer)curPage);
				bind.put(JS_DIR, curPage.dir);
				bind.put(JS_FILE, curPage.file);
				bind.put(JS_PAGE_ID, curPage.name);
				curPage.execute(bind, _getSbOut(), parent);
				
			} while (false);
			
		}
		@Override
		public void header(String name, String val) {
			mapHeader.put(name, val);
		}
		
	}
	
	private static String parseContentTypeByName(String name, String defType){
		if(name.endsWith(".png")){
			return "image/png";
		}else if(name.endsWith(".jpg") || name.endsWith(".jpeg")){
			return "image/jpeg";
		}else if(name.endsWith(".js")){
			return "application/javascript";
		}else if(name.endsWith(".gif")){
			return "image/gif";
		}else if(name.endsWith(".html") || name.endsWith(".htm")){
			return "text/html; charset=UTF-8";
		}
		return defType==null?DFHttpContentType.OCTET_STREAM:defType;
	}
	private static boolean isWebResByName(String name){
		if(name.endsWith(".js") || name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".css")){
			return true;
		}
		return false;
	}
}
