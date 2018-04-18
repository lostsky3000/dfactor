package com.funtag.util.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DFSpinLock{
	
	private final AtomicBoolean _owner = new AtomicBoolean(false);
	
	public void lock(){
//		final long tmStart = System.nanoTime();
		while(!_owner.compareAndSet(false, true)){
			
		}
//		final int tmCost = (int) (System.nanoTime() - tmStart);
//		if(tmCost > 5000000){
//			int m = 1;
//			int n = m;
//		}
	}
	public boolean tryLock(int tryTimes){
		while(!_owner.compareAndSet(false, true)){
			if(--tryTimes < 1){
				return false;
			}
		}
		return true;
	}
	
	public void unlock(){
		_owner.compareAndSet(true, false);
	}

}
