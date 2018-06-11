package fun.lib.actor.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.funtag.util.net.DFIpUtil;
import com.funtag.util.proto.DFProtoUtil;
import com.funtag.util.script.DFJsPageModel;
import com.funtag.util.system.DFSysUtil;
import com.google.common.io.Files;
import com.google.protobuf.GeneratedMessageV3;

import fun.lib.actor.api.cb.Cb;
import fun.lib.actor.po.ActorProp;
import fun.lib.actor.po.DFActorClusterConfig;
import fun.lib.actor.po.DFActorManagerConfig;
import fun.lib.actor.po.DFPageInfo;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public final class DFActorManagerJs{

	private static DFActorManagerJs instance = new DFActorManagerJs();
	
	public static DFActorManagerJs get(){
		return instance;
	}
	
	private final DFActorManager _mgrActor;
	private ScriptEngine _jsEngine = null;
	private ScriptObjectMirror _jsFnApi = null;
	
	private DFActorManagerJs() {
		_mgrActor = DFActorManager.get();
	}
	
	//
	protected int send(int srcId, Object dst, int cmd, Object payload){
		if(dst instanceof String){ //dst name
			return _mgrActor.send(srcId, (String) dst, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true, null, null, null, null);
		}else{ //dst id
			int dstId = 0;
			if(dst instanceof Double){
				dstId = ((Double)dst).intValue();
			}else{
				dstId = (int) dst;
			}
			return _mgrActor.send(srcId, dstId, 0, DFActorDefine.SUBJECT_USER, cmd, payload, true);
		}
	}
	
	protected int createActor(Object template, Object name, Object param, Object initCfg) {
		
		HashMap<String,Object> mapParam = new HashMap<>();
		mapParam.put("template", template);
		mapParam.put("fnApi", _jsFnApi);
		if(param != null && !ScriptObjectMirror.isUndefined(param)){
			mapParam.put("param", param);
		}
		ActorProp prop = ActorProp.newProp()
				.classz(DFJsActor.class)
				.param(mapParam);
		if(name != null && name instanceof String){
			prop.name((String) name);
		}
		if(initCfg == null){ //check default cfg
			ScriptObjectMirror mirFunc = (ScriptObjectMirror) template;
			ScriptObjectMirror tmpInstance = (ScriptObjectMirror) mirFunc.newObject();
			initCfg = tmpInstance.getMember("initCfg");
			tmpInstance = null;
		}
		if(initCfg != null){ //has cfg
			try{
				if(!ScriptObjectMirror.isUndefined(initCfg)){
					ScriptObjectMirror mirCfg = (ScriptObjectMirror) initCfg;
					Object objTmp = mirCfg.get("block");
					if(objTmp != null) prop.blockActor((Boolean)objTmp);
					objTmp = mirCfg.get("schedule");
					if(objTmp != null) prop.scheduleMilli((Integer)objTmp);
				}
			}catch(Throwable e){e.printStackTrace();}
		}
		return _mgrActor.createActor(prop.getName(), prop.getClassz(), prop.getParam(),
				(int) (prop.getScheduleMilli()/DFActor.TIMER_UNIT_MILLI), prop.getConsumeType(), prop.isBlock());
	}
	
	protected GeneratedMessageV3 getProtoType(String className){
		return s_mapProto.get(className);
	}
	
	private final ConcurrentHashMap<String, DFPageInfo> _mapPageInfo = new ConcurrentHashMap<>();
	protected DFPageInfo getPageInfo(String absPath){
		return _mapPageInfo.get(absPath);
	}
	protected void addPageInfo(String absPath, DFPageInfo pageInfo){
		_mapPageInfo.put(absPath, pageInfo);
	}
	protected void removePageInfo(String absPath){
		_mapPageInfo.remove(absPath);
	}
	
	//
	
	private ScriptEngineManager _engineMgr = null;
	private DFActorManagerConfig _cfgMgr = null;
	private DFActorClusterConfig _cfgCluster = null;
	private ActorProp _propActorEntry = null;
	private String _entryActor = null;
	private String _entryActorName = null;
	private String _customDir = null;
	private String _protoDir = null;
	private String _extLibDir = null;
	private String _absRunDir = null;
	
	public boolean start(String runDir){
		boolean bRet = false;
		do {
			File dirRun = new File(runDir);
			if(!dirRun.exists() || !dirRun.isDirectory()){
				printError("runDir invalid: "+runDir);
				break;
			}
			File dirCfg = new File(dirRun.getAbsolutePath()+File.separator+"cfg");
			if(!dirCfg.exists()){
				printError("cfgDir not exist: "+dirCfg.getAbsolutePath());
				break;
			}
			if(!_initEngine()){
				printError("initJsEngine failed");
				break;
			}
			print("initJsEngine succ");
			if(!_initCfg(dirCfg, dirRun) || _cfgMgr==null || _propActorEntry==null){
				printError("initCfg failed");
				break;
			}
			print("initCfg succ");
			if(_protoDir != null){ //has protobuf define
				if(!_checkProtobuf(dirRun)){
					printError("parse proto failed");
					break;
				}
			}
			if(_extLibDir != null){ //has ext lib
				print("start load extLib in dir: "+_extLibDir);
				if(!_loadExtLib(dirRun)){
					printError("load ext lib failed");
					break;
				}
				print("load extLib done");
			}
			//init js
			if(!_initScript(dirRun)){
				printError("initScript failed");
				break;
			}
			Bindings bind = _jsEngine.getBindings(ScriptContext.ENGINE_SCOPE);
			_jsFnApi = (ScriptObjectMirror) bind.get("g_fn_df");
			//
			_absRunDir = dirRun.getAbsolutePath();
			//start dfactor
			bRet = _mgrActor.start(_cfgMgr, _propActorEntry);
		} while (false);
		return bRet;
	}
	
	protected String getAbsRunDir(){
		return _absRunDir;
	}
	private boolean _initScript(File dirRun){
		boolean bRet = false;
		do {
			File dirSys = new File(dirRun.getAbsolutePath() + File.separator + "script");
			if(!dirSys.exists() || !dirSys.isDirectory()){
				printError("invalid script dir: "+dirSys.getAbsolutePath());
				break;
			}
			if(!_initSysScript(dirSys)){
				printError("init sys script failed");
				break;
			}
			File dirCustom = new File(dirRun.getAbsolutePath() + File.separator + _customDir);
			if(!dirCustom.exists() || !dirCustom.isDirectory()){
				printError("invalid custom dir: "+dirCustom.getAbsolutePath());
				break;
			}
			if(!_initCustomScript(dirCustom)){
				printError("init customScript failed");
				break;
			}
			_mapScriptKeyFile.clear();
			bRet = true;
		} while (false);
		return bRet;
	}
	
	private boolean _loadExtLib(File dirRun){
		boolean bRet = false;
		do {
			File dir = new File(dirRun.getAbsolutePath()+File.separator+_extLibDir);
			if(!dir.exists() || !dir.isDirectory()){
				printError("invalid extLibDir: "+dir.getAbsolutePath()); 
				break;
			}
			if(!_mgrActor.loadJars(dir.getAbsolutePath(), _mapExtClz)){
				printError("load extLib failed in dir: "+dir.getAbsolutePath());
				break;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	private ConcurrentHashMap<String, Class<?>> _mapExtClz = new ConcurrentHashMap<>();
	protected Class<?> getExtLibClass(String clzName){
		return _mapExtClz.get(clzName);
	}
	
	private boolean _initCustomScript(File dir){
		boolean bRet = false;
		do {
			LinkedList<File> lsFile = new LinkedList<>();
			_iteratorCustomScript(dir, lsFile);
			Iterator<File> it = lsFile.iterator();
			while(it.hasNext()){
				File f = it.next();
				if(!_checkScriptFileValid(f)){
					return false;
				}
				try {
					_jsEngine.eval(new FileReader(f));
				} catch (FileNotFoundException | ScriptException e) {
					e.printStackTrace();
					return false;
				}
			}
			Bindings bind = _jsEngine.getBindings(ScriptContext.ENGINE_SCOPE);
			Object objEntry = bind.get(_entryActor);
			if(objEntry == null){
				printError("EntryActor not found: "+_entryActor);
				break;
			}
			if(!(objEntry instanceof ScriptObjectMirror)){
				printError("invalid EntryActor class: "+objEntry.getClass());
				break;
			}
			ScriptObjectMirror mir = (ScriptObjectMirror) objEntry;
			if(!mir.isFunction()){
				printError("invalid EntryActor type: not function");
				break;
			}
			
			bRet = true;
		} while (false);
		return bRet;
	}
	
	private void _iteratorCustomScript(File dir, LinkedList<File> ls){
		File[] arrFile = dir.listFiles();
		int len = arrFile.length;
		for(int i=0; i<len; ++i){
			if(arrFile[i].isFile()){
				if(_isValidJsFile(arrFile[i])){
					ls.offer(arrFile[i]);
				}
			}else{ //dir
				_iteratorCustomScript(arrFile[i], ls);
			}
		}
	}
	private boolean _isValidJsFile(File f){
		if(f.getName().endsWith(".js")){
			return true;
		}
		return false;
	}
	
	private boolean _initSysScript(File dir){
		boolean bRet = false;
		do {
			try {
				File f = new File(dir.getAbsolutePath()+File.separator+"Init.js");
				if(!f.exists()){
					printError("invalid sys script: "+f.getAbsolutePath());
					break;
				}
				//
				if(!_checkScriptFileValid(f)){
					printError("add "+f.getAbsolutePath()+" failed");
					break;
				}
				_jsEngine.eval(new FileReader(f));
			} catch (FileNotFoundException | ScriptException e) {
				e.printStackTrace();
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	private HashMap<String,String> _mapScriptKeyFile = new HashMap<>();
	private boolean _checkScriptFileValid(File f){
		boolean bRet = false;
		do {
			ScriptEngine engineTmp = _engineMgr.getEngineByName("JavaScript");
			try {
				engineTmp.eval(new FileReader(f));
				Set<String> setCur = _jsEngine.getBindings(ScriptContext.ENGINE_SCOPE).keySet();
				Bindings bindTmp = engineTmp.getBindings(ScriptContext.ENGINE_SCOPE);
				Iterator<String> it = bindTmp.keySet().iterator();
				while(it.hasNext()){
					String key = it.next();
					if(setCur.contains(key)){
						printError("global var duplicated: "+key+" in '"+f.getAbsolutePath()+"' & '"+_mapScriptKeyFile.get(key)+"'");
						return false;
					}else{
						_mapScriptKeyFile.put(key, f.getAbsolutePath());
					}
				}
			} catch (FileNotFoundException | ScriptException e) {
				e.printStackTrace();
				break;
			}finally{
				engineTmp = null;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	
	private boolean _initEngine(){
		boolean bRet = false;
		do {
			try{
				if(_engineMgr == null){
					_engineMgr = new ScriptEngineManager();
				}
				_jsEngine = _engineMgr.getEngineByName("JavaScript");
				if(_jsEngine == null){
					printError("getJsEngine failed");
					break;
				}
			}catch(Throwable e){
				e.printStackTrace();
				break;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	private boolean _initCfg(File dirCfg, File dirRun){
		boolean bRet = false;
		do {
			try{
				Properties propBase = new Properties();
				propBase.load(new FileReader(dirCfg.getAbsolutePath()+File.separator+"dfactor.conf"));
				_cfgMgr = new DFActorManagerConfig();
				//
				_customDir = propBase.getProperty("custom_dir");
				if(_customDir == null){
					printError("no custom dir conf found!");break;
				}
				_customDir = _customDir.trim();
				//
				_protoDir = propBase.getProperty("proto_dir");
				if(_protoDir != null){
					_protoDir = _protoDir.trim();
					if(_protoDir.equals("")) _protoDir = null;
				}
				//
				_extLibDir = propBase.getProperty("ext_lib_dir");
				if(_extLibDir != null){
					_extLibDir = _extLibDir.trim();
					if(_extLibDir.equals("")) _extLibDir = null;
				}
				//
				_entryActor = propBase.getProperty("entry_actor");
				if(_entryActor != null){
					_entryActor = _entryActor.trim();
				}
				if(_entryActor == null || _entryActor.equals("")){
					printError("invalid entryActor: "+_entryActor);
					break;
				}
				_entryActorName = propBase.getProperty("entry_actor_name");
				if(_entryActorName != null){
					_entryActorName = _entryActorName.trim();
				}
				if(_entryActorName!=null && _entryActorName.equals("")){
					_entryActorName = null;
				}
				String val = propBase.getProperty("logic_thread");
				if(val != null){
					int num = Integer.parseInt(val.trim());
					if(num > 0) _cfgMgr.setLogicWorkerThreadNum(num);
				}
				//
				val = propBase.getProperty("block_thread");
				if(val != null){
					int num = Integer.parseInt(val.trim());
					if(num > 0) _cfgMgr.setBlockWorkerThreadNum(num);
				}
				//
				val = propBase.getProperty("client_io_thread");
				if(val != null){
					int num = Integer.parseInt(val.trim());
					if(num > 0) _cfgMgr.setClientIoThreadNum(num);
				}
				//
				val = propBase.getProperty("timer_thread");
				if(val != null){
					int num = Integer.parseInt(val.trim());
					if(num > 0) _cfgMgr.setTimerThreadNum(num);
				}
				//
				val = propBase.getProperty("log_level");
				if(val != null){
					int num = Integer.parseInt(val.trim());
					if(num > 0) _cfgMgr.setLogLevel(num);
				}
				//
				val = propBase.getProperty("use_sys_log");
				if(val != null){
					int num = Integer.parseInt(val.trim());
					_cfgMgr.setUseSysLog(num==0?false:true);
				}
				//
				val = propBase.getProperty("sys_log_consume");
				if(val != null){
					int num = Integer.parseInt(val.trim());
					if(num > 0){
						_cfgMgr.setSysLogConsumeType(num);
					}
				}
				//
				val = propBase.getProperty("enable_cluster");
				if(val != null){
					int num = Integer.parseInt(val.trim());
					if(num == 1){  //enable cluster
						_loadClusterCfg(dirCfg);
					}
				}
				_cfgMgr.setClusterConfig(_cfgCluster);
				//
				HashMap<String,Object> mapParam = new HashMap<>();
				mapParam.put("entryActor", _entryActor);
				if(_entryActorName != null){
					mapParam.put("entryActorName", _entryActorName);
				}
				mapParam.put("engine", _jsEngine);
				_propActorEntry = ActorProp.newProp()
						.classz(DFJsLaunchActor.class)
						.param(mapParam);
			}catch(Throwable e){
				e.printStackTrace();
				break;
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	private void _loadClusterCfg(File dirCfg){
		File fCfg = new File(dirCfg.getAbsolutePath()+File.separator+"cluster.conf");
		String nodeName = null;
		do {
			try{
				if(!fCfg.exists()){  
					break;
				}
				Properties prop = new Properties();
				prop.load(new FileReader(fCfg));
				String val = prop.getProperty("node_name");
				if(val != null){
					val = val.trim();
					if(!val.equals("")) nodeName = val;
				}
				_cfgCluster = DFActorClusterConfig.newCfg(nodeName);
				//
				val = prop.getProperty("cluster_name");
				if(val != null){
					val = val.trim();
					if(!val.equals("")) _cfgCluster.setClusterName(val);
				}
				//
				val = prop.getProperty("node_type");
				if(val != null){
					val = val.trim();
					if(!val.equals("")) _cfgCluster.setNodeType(val);
				}
				//
				val = prop.getProperty("secret_key");
				if(val != null){
					val = val.trim();
					if(!val.equals("")) _cfgCluster.setSecretKey(val);
				}
				//
				val = prop.getProperty("base_port");
				if(val != null){
					int port = 0;
					try{
						port = Integer.parseInt(val.trim());
					}catch(Throwable e){}
					if(port > 0 && port < 65536) _cfgCluster.setBasePort(port);
				}
				//
				val = prop.getProperty("io_thread");
				if(val != null){
					int num = 0;
					try{
						num = Integer.parseInt(val.trim());
					}catch(Throwable e){}
					if(num > 0) _cfgCluster.setIoThreadNum(num);
				}
				//
				val = prop.getProperty("host_range");
				if(val != null){
					val = val.trim();
					String[] arr = val.split("-");
					String begin = null, end = null;
					for(int i=0; i<arr.length; ++i){
						String tmp = arr[i].trim();
						try{
							if(!tmp.equals("") && DFIpUtil.isLanIP(tmp)){
								if(begin == null){
									begin = tmp;
								}else if(end == null){
									end = tmp;
									break;
								}
							}
						}catch(Throwable e){continue;}
					}
					if(begin != null && end != null){
						_cfgCluster.setIPRange(begin, end);
					}
				}
				//
				val = prop.getProperty("host_specify");
				if(val != null){
					val = val.trim();
					String[] arr = val.split(",");
					for(int i=0; i<arr.length; ++i){
						String tmp = arr[i].trim();
						try{
							if(!tmp.equals("") && DFIpUtil.isLanIP(tmp)){
								_cfgCluster.addSpecifyIP(tmp);
							}
						}catch(Throwable e){continue;}
					}
				}
			}catch(Throwable e){
				e.printStackTrace();
			}finally{
				if(_cfgCluster == null){
					_cfgCluster = DFActorClusterConfig.newCfg(nodeName);
				}
			}
		} while (false);
	}
	
	private boolean _checkProtobuf(File dirRun){
		boolean bRet = false;
		do {
			File dirProto = new File(dirRun.getAbsolutePath() + File.separator + _protoDir);
			LinkedList<File> lsFile = new LinkedList<>();
			if(dirProto.exists() && dirProto.isDirectory()){  //遍历proto
				_iteratorProto(dirProto, lsFile);
			}
			if(!lsFile.isEmpty()){ //有proto
				String srcDir = dirProto.getAbsolutePath();
				String outDir = dirRun.getAbsolutePath() + File.separator + "temp/src";
				String binPath = dirRun.getAbsolutePath() + File.separator +"bin/";
				if(DFSysUtil.getOSType() == DFSysUtil.OS_WINDOWS){
					binPath += "protoc.exe";
				}else{
					binPath = "protoc";
				}
				File dirOut = new File(outDir);
				if(!dirOut.exists()){
					printError("proto out dir not exist: "+dirOut.getAbsolutePath());
					break;
				}
				LinkedList<File> lsJava = new LinkedList<>();
				_iteratorJavaFile(dirOut, lsJava, true);  //delete all exist javaFile
				Iterator<File> it = lsFile.iterator();
				print("start compile proto files ...");
				try{
					while(it.hasNext()){
						File f = it.next();
						int ret = DFProtoUtil.execProtoc(binPath, srcDir, outDir, f.getAbsolutePath());
						if(ret != 0){
							printError("gen proto failed: "+f.getAbsolutePath());
							return false;
						}
					}
				}catch(Throwable e){
					e.printStackTrace();
					break;
				}
				print("compile proto files done");
				//compile all java
				lsJava.clear();
				_iteratorJavaFile(dirOut, lsJava, false);
				if(!lsJava.isEmpty()){
					//获取系统Java编译器
				    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
				    if(compiler == null){  //未获取到编译器
				    	//检测java编译环境
						String javaHome = System.getProperty("java.home");
				    	if(javaHome == null){
				    		printError("JAVA_HOME not found"); break;
				    	}
				    	print("JAVA_HOME="+javaHome);
				    	File dirJavaHome = new File(javaHome);
				    	if(!dirJavaHome.exists() || !dirJavaHome.isDirectory()){
				    		printError("invalid JAVA_HOME: "+dirJavaHome.getAbsolutePath()); break;
				    	}
				    	File fToolsJar = new File(dirJavaHome.getAbsolutePath()+File.separator+"lib"+File.separator+"tools.jar");
				    	printError("you need copy jdk/lib/tools.jar to "+fToolsJar.getAbsolutePath()+" manually");
//				    	if(!fToolsJar.exists()){  //tools.jar not exist, copy
//				    		print("tools.jar not found, start copy");
//				    		File fSrcJar = new File(dirRun.getAbsolutePath()+File.separator+"lib"+File.separator+"tools.jar");
//				    		try{
//				    			Files.copy(fSrcJar, fToolsJar);
//				    		}catch(Throwable e){
//				    			e.printStackTrace();
//				    			printError("you should copy "+fSrcJar.getAbsolutePath()+" to "+fToolsJar.getAbsolutePath()+" manually");
//				    			break;
//				    		}
//				    	}
				    	break;
				    }
					File[] arrJava = new File[lsJava.size()];
					Iterator<File> itJava = lsJava.iterator();
					int idx = 0;
					while(itJava.hasNext()){
						arrJava[idx++] = itJava.next();
					}
					print("start compile proto java, hold on ...");
					//start compile
				    //获取Java文件管理器
				    final HashMap<String,byte[]> bytes = new HashMap<>();
				    final LinkedList<String> lsClzName = new LinkedList<>();
				    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
				    JavaFileManager jfm = new ForwardingJavaFileManager(fileManager) {
				        public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location,
				                                                   final String className,
				                                                   JavaFileObject.Kind kind,
				                                                   FileObject sibling) throws IOException {
				            if(kind == JavaFileObject.Kind.CLASS) {
				                return new SimpleJavaFileObject(URI.create(""), JavaFileObject.Kind.CLASS) {
				                    public OutputStream openOutputStream() {
				                        return new FilterOutputStream(new ByteArrayOutputStream()) {
				                            public void close() throws IOException{
				                                out.close();
				                                ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
				                                bytes.put(className, bos.toByteArray());
				                                lsClzName.offer(className);
				                            }
				                        };
				                    }
				                };
				            }else{
				                return super.getJavaFileForOutput(location, className, kind, sibling);
				            }
				        }
				    };
				    //通过源文件获取到要编译的Java类源码迭代器，包括所有内部类，其中每个类都是一个 JavaFileObject，也被称为一个汇编单元
				    Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(arrJava);
				    //生成编译任务
				    JavaCompiler.CompilationTask task = compiler.getTask(null, jfm, null, null, null, compilationUnits);
				    //执行编译任务
				    boolean retCompile = task.call();
				    if(!retCompile){
				    	printError("compile proto java error !!!");
				    }
				    //load class
				    MemClassLoader clzLoader = new MemClassLoader(bytes);
				    Iterator<String> itClzName = lsClzName.iterator();
			    	while(itClzName.hasNext()){
						try {
							String tmpName = itClzName.next();
				    		Class<?> clz = clzLoader.loadClass(tmpName);
							Constructor<?> ctor = clz.getDeclaredConstructor();
							ctor.setAccessible(true);
							Object obj = ctor.newInstance();
							GeneratedMessageV3 m = (GeneratedMessageV3) obj;
//							tmpName = tmpName.replaceAll("\\$", ".");
							synchronized ("asdfasdfasdfasdfasdffg2e") {
								s_mapProto.put(tmpName, m);
							}
						} catch (Throwable e) {
//							e.printStackTrace();
						} 
			    	}
			    	print("compile proto java done");
			    	try {
						clzLoader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			bRet = true;
		} while (false);
		return bRet;
	}
	private void _iteratorProto(File dir, LinkedList<File> ls){
		File[] arr = dir.listFiles();
		int len = arr.length;
		for(int i=0; i<len; ++i){
			File f = arr[i];
			if(f.isFile()){
				if(_isProtoFile(f)){
					ls.offer(f);
				}
			}else{
				_iteratorProto(f, ls);
			}
		}
	}
	private void _iteratorJavaFile(File dir, LinkedList<File> lsFile, boolean delete){
		File[] arr = dir.listFiles();
		int len = arr.length;
		for(int i=0; i<len; ++i){
			File f = arr[i];
			if(f.isFile()){
				if(_isJavaFile(f)){
					if(delete){
						f.delete();
					}
					lsFile.offer(f);
				}
			}else{
				_iteratorJavaFile(f, lsFile, delete);
			}
		}
	}
	private boolean _isJavaFile(File f){
		if(f.isFile() && f.getName().endsWith(".java")){
			return true;
		}
		return false;
	}
	private boolean _isProtoFile(File f){
		if(f.isFile() && f.getName().endsWith(".proto")){
			return true;
		}
		return false;
	}
	private static HashMap<String, GeneratedMessageV3> s_mapProto = new HashMap<>();
	private static class MemClassLoader extends URLClassLoader{
		private final HashMap<String, byte[]> mapClzByte;
		public MemClassLoader(HashMap<String, byte[]> mapClzByte) {
			super(new URL[0], MemClassLoader.class.getClassLoader());
			this.mapClzByte = mapClzByte;
		}
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] bufClz = mapClzByte.remove(name);
			if(bufClz == null){
				return super.findClass(name);
			}
			return defineClass(name, bufClz, 0, bufClz.length);
		}
	}
	
	private static void print(Object msg){
		System.out.println(msg);
	}
	private static void printError(Object msg){
		System.err.println(msg);
	}
	
}
