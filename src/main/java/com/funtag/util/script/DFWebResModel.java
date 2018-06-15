package com.funtag.util.script;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import fun.lib.actor.api.DFActorLog;
import fun.lib.actor.api.http.DFHttpContentType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

public final class DFWebResModel {

	public final String path;
	
	private byte[] bufOut = null;
	private int bufLen = 0;
	private String contentType = DFHttpContentType.OCTET_STREAM;  //default
	
	private int version = 0;
	
	public DFWebResModel(String path) {
		this.path = path;
	}
	
	public boolean load(DFActorLog log){
		boolean bRet = false;
		do {
			File f = new File(path);
			if(!f.exists() || !f.isFile()){
				log.error("load res failed: "+path);
				break;
			}
			//
			BufferedInputStream bis = null;
			try{
				bis = new BufferedInputStream(new FileInputStream(f));
				int read = -1;
				int left = bis.available();
				bufLen = left;
				bufOut = null;
				bufOut = new byte[bufLen];
				int off = 0;
				while( (read=bis.read(bufOut, off, left)) > 0 ){
					off += read;
					left -= read;
				}
			}catch(Throwable e){
				e.printStackTrace();
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
			bRet = true;
		} while (false);
		return bRet;
	}
	
	public byte[] getBufOut(){
		return this.bufOut;
	}
	public int getBufLen(){
		return this.bufLen;
	}
	
	public String getContentType(){
		return this.contentType;
	}
	public void setContentType(String contentType){
		this.contentType = contentType;
	}
	
	public int getVersion(){
		return this.version;
	}
	public void setVersion(int version){
		this.version = version;
	}
}
