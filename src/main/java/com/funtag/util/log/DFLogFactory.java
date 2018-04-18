package com.funtag.util.log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class DFLogFactory {

	public static final int LEVEL_VERB = 1;
	public static final int LEVEL_DEBUG = 2;
	public static final int LEVEL_INFO = 3;
	public static final int LEVEL_WARN = 4;
	public static final int LEVEL_ERROR = 5;
	public static final int LEVEL_FATAL = 6;
	
	public static DFLogger create(final Class c){
		return new DFLogger() {
			private SimpleDateFormat _sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			private final String className = c.getSimpleName();
			private final StringBuffer sbLog = new StringBuffer();
			private void getTimePfx(final StringBuffer sb){
				sb.setLength(0);
				sb.append(_sdf.format(Calendar.getInstance().getTime()));
				sb.append("[");
				sb.append(className);
				sb.append("]");
			}
			@Override
			public void V(String msg) {
				if(DFLogFactory._logLevel <= LEVEL_VERB){
					sbLog.setLength(0);
					getTimePfx(sbLog);
					sbLog.append("[V]");
					sbLog.append(msg);
					System.out.println(sbLog.toString());
				}
			}
			@Override
			public void D(String msg) {
				if(DFLogFactory._logLevel <= LEVEL_DEBUG){
					sbLog.setLength(0);
					getTimePfx(sbLog);
					sbLog.append("[D]");
					sbLog.append(msg);
					System.out.println(sbLog.toString());
				}
			}
			@Override
			public void I(String msg) {
				if(DFLogFactory._logLevel <= LEVEL_INFO){
					sbLog.setLength(0);
					getTimePfx(sbLog);
					sbLog.append("[I]");
					sbLog.append(msg);
					System.out.println(sbLog.toString());
				}
			}
			@Override
			public void W(String msg) {
				if(DFLogFactory._logLevel <= LEVEL_WARN){
					sbLog.setLength(0);
					getTimePfx(sbLog);
					sbLog.append("[W]");
					sbLog.append(msg);
					System.out.println(sbLog.toString());
				}
			}
			@Override
			public void E(String msg) {
				if(DFLogFactory._logLevel <= LEVEL_ERROR){
					sbLog.setLength(0);
					getTimePfx(sbLog);
					sbLog.append("[E]");
					sbLog.append(msg);
					System.out.println(sbLog.toString());
				}
			}
			@Override
			public void F(String msg) {
				sbLog.setLength(0);
				getTimePfx(sbLog);
				sbLog.append("[F]");
				sbLog.append(msg);
				System.out.println(sbLog.toString());
			}
		};
	}
	
	private static volatile int _logLevel = LEVEL_INFO;
	
	public static boolean setLogLevel(int level){
		if(level <= LEVEL_FATAL && level >= LEVEL_VERB){
			_logLevel = level;
			return true;
		}
		return false;
	}
	
}



