package com.funtag.util.fs;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DFFileMonitor {
	
	private final File dirRoot;
	private final IFSMonitor listener;
	private volatile WatchService watcher;
	private volatile ExecutorService pool = null;
	private volatile DFMonitorTh monitor = null;
	public DFFileMonitor(File dirRoot, IFSMonitor listener) {
		this.dirRoot = dirRoot;
		this.listener = listener;
	}
	
	public boolean start(){
		if(pool != null){
			return true;
		}
		boolean bRet = false;
		do {
			try {
				watcher = FileSystems.getDefault().newWatchService();
			} catch (IOException e) {
				e.printStackTrace(); break;
			}
			pool = Executors.newSingleThreadExecutor();
			monitor = new DFMonitorTh();
			pool.submit(monitor);
			bRet = true;
		} while (false);
		return bRet;
	}
	public void close(){
		if(pool == null){
			return;
		}
		monitor._close();
		pool.shutdown();
		pool = null;
	}
	
	private class DFMonitorTh implements Runnable{
		@Override
		public void run() {
			_onLoop = true;
			_registAll(Paths.get(dirRoot.getAbsolutePath()));
			WatchKey key = null;
			while(_onLoop){
				// 获取下一个文件改动事件
				try {
					key = watcher.poll(2000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace(); continue;
				}
				if(key == null){
					continue;
				}
	            Path dir = _mapKeyPath.get(key);
	            if(dir == null){
	            	continue;
	            }
	            for (WatchEvent<?> event : key.pollEvents()) {
	            	Kind<?> kind = event.kind();
	                // 事件可能丢失或遗弃
	                if (kind == StandardWatchEventKinds.OVERFLOW) {
	                    continue;
	                }
	                WatchEvent<Path> ev = (WatchEvent<Path>) event;
	                Path name = ev.context();
	                Path child = dir.resolve(name);
	                File file = child.toFile();
	                if(kind.name().equals(StandardWatchEventKinds.ENTRY_CREATE.name())){
//	                	print("create: " + file +" --> " + event.kind());
	                	if(file.isDirectory()){
	                		_regist(Paths.get(file.getAbsolutePath()), watcher);
	                	}
	                	listener.onCreate(file);
	                }else if(kind.name().equals(StandardWatchEventKinds.ENTRY_DELETE.name())){
//	                	print("delete: " + file +" --> " + event.kind());
	                	listener.onDelete(file);
	                }else if(kind.name().equals(StandardWatchEventKinds.ENTRY_MODIFY.name())){
//	                	print("modify: " + file +" --> " + event.kind());
	                	listener.onModify(file);
	                }
	            }
	            // 重设WatchKey
	            boolean valid = key.reset();
	            // 如果重设失败，退出监听
	            if (!valid) {
	            	// 移除不可访问的目录
	                // 因为有可能目录被移除，就会无法访问
	            	_mapKeyPath.remove(key);
	                // 如果待监控的目录都不存在了，就中断执行
	                if (_mapKeyPath.isEmpty()) {
	                    break;
	                }
	            }
			}
			listener.onClose();
		}
		private final HashMap<WatchKey, Path> _mapKeyPath = new HashMap<>();
		private void _regist(Path dir, WatchService watch){
			try {
				WatchKey key = Paths.get(dir.toFile().getAbsolutePath()).register(watcher, 
						StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.ENTRY_MODIFY,
						StandardWatchEventKinds.ENTRY_DELETE);
				_mapKeyPath.put(key, dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		private void _registAll(Path dir){
			LinkedList<File> lsDir = new LinkedList<>();
			_regist(dir, watcher);
			lsDir.add(dir.toFile());
			while(!lsDir.isEmpty()){
				File tmpDir = lsDir.removeFirst();
				File[] arr = tmpDir.listFiles();
				if(arr == null || arr.length == 0){
					continue;
				}
				for(File f:arr){
					if(f.isDirectory()){
						lsDir.addLast(f);
						_regist(Paths.get(f.getAbsolutePath()), watcher);
					}
				}
			}
		}
		private volatile boolean _onLoop = false;
		private void _close(){
			_onLoop = false;
		}
	}
}
