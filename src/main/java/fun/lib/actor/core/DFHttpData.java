package fun.lib.actor.core;

import java.io.File;
import java.io.IOException;

import io.netty.handler.codec.http.multipart.FileUpload;

public final class DFHttpData {

	private final FileUpload fileUp;
	
	protected DFHttpData(FileUpload fileUp) {
		this.fileUp = fileUp;
	}
	
	
	//api
	public String getContentType(){
		return fileUp.getContentType();
	}
	public String getField(){
		return fileUp.getName();
	}
	public String getFileName(){
		return fileUp.getFilename();
	}
	public boolean moveTo(String dstPath) throws IOException{
		return fileUp.renameTo(new File(dstPath));
	}
}
