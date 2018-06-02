package com.funtag.util.proto;

public final class DFProtoUtil {

	public static int execProtoc(String binPath, String srcDir, String outDir, String srcPath){
		int ret = -1;
		try{
			String cmd = binPath+" -I="+srcDir+" --java_out="+outDir+" "+srcPath;
			Process p = Runtime.getRuntime().exec(cmd);
			ret = p.waitFor();
		}catch(Throwable e){
			e.printStackTrace();
			return -2;
		}
		return ret;
	}
	
}
