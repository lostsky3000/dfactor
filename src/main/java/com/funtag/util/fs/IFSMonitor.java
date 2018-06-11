package com.funtag.util.fs;

import java.io.File;

public interface IFSMonitor {

	public void onCreate(File f);
	
	public void onModify(File f);
	
	public void onDelete(File f);
	
	public void onClose();
}
