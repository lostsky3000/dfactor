package com.funtag.util.system;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class DFSysUtil {
	
	public static final int OS_UNKNOWN = 0;
	public static final int OS_LINUX = 1;
	public static final int OS_WINDOWS = 2;
	public static final int OS_MAC = 3;
	
	private static int _osType = OS_UNKNOWN;
	static{
		final String osName = System.getProperty("os.name");
		if(osName != null){
			final String osNameLow = osName.toLowerCase();
			if(osNameLow.contains("linux")){
				_osType = OS_LINUX;
			}else if(osNameLow.contains("windows")){
				_osType = OS_WINDOWS;
			}
		}
	}
	
	public static int getOSType(){
		return _osType;
	}
	
	public static boolean isLinux(){
		return _osType==OS_LINUX?true:false;
	}
	
	
	public static int execShellCmd(final List<String> cmds, final List<String> out, final int timeoutSec){
		if(!DFSysUtil.isLinux()){
			return -4;
		}
		
		File dirBin = new File("/bin");
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("/bin/bash", null, dirBin);
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		if(p != null){
			BufferedReader br = null;
			if(out != null){
				br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			}
			PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(p.getOutputStream())), true);
			//exec cmd
//			pw.println("pwd");
//			pw.println("cd ~/javatest");
			for(String cmd : cmds){
				pw.println(cmd);
			}
			pw.println("exit");
			//
			if(br != null){
				String line = null;
				try {
					while((line=br.readLine()) != null){
						if(out != null){
							out.add(line);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			try {
				boolean bExit = p.waitFor(timeoutSec, TimeUnit.SECONDS);
				if(bExit){
					return 0;
				}
				return 1;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return -2;
			}finally{
				if(br != null){
					try {
						br.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				pw.close();
				p.destroy();
			}
		}
		return -3;
	}
}
