package com.funtag.util.timer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class DFHashWheelTimer {

	public final long tickDuration;
	public final int tickPerWheel;
	private final ArrayList<DFHashWheelSlot> _lsSlot;
	private int _curSlotIdx = 0;
	private long _tickCount = -1;
	private long _tmTotal = 0;
	private final int _scheduleTickInitRange;
	private long _scheduleTickCount = 0;
	private final ConcurrentLinkedQueue<DFTimerEvent> _queueEvent = new ConcurrentLinkedQueue<>();
	
	public DFHashWheelTimer(long tickDuration, int tickPerWheel, int scheduleInitRange) {
		this.tickDuration = tickDuration;
		this.tickPerWheel = tickPerWheel;
		_lsSlot = new ArrayList<>(tickPerWheel);
		for(int i=0; i<tickPerWheel; ++i){
			DFHashWheelSlot slot = new DFHashWheelSlot(i);
			_lsSlot.add(slot);
		}
		//
		_scheduleTickInitRange = scheduleInitRange; //(int) (1000*1000000/tickDuration);
	}
	
	public void start(long tmStart){
		if(_tickCount == -1){
			_curSlotIdx = 0;
			_tickCount = -1;
			_tmTotal = 0;
			//init tick
			this.onTick(tmStart, 0);
		}
	}
	public void addTimeout(long delay, DFTimeout timeout){
		final DFTimerEvent event = new DFTimerEvent(EVENT_ADD_TIMEOUT, delay, 0, timeout);
		_queueEvent.offer(event);
	}
	private void _addTimeout(long delay, DFTimeout timeout){
		final long curTickCount = Math.max(0, _tickCount);
		final long tmTrig = tickDuration*curTickCount + delay;
		final int tmpTickCount = (int) (tmTrig/tickDuration);
		final int idxSlot = tmpTickCount%tickPerWheel;
		
		int roundCount = (int) ((tmpTickCount - curTickCount)/tickPerWheel); // tmpTickCount/tickPerWheel;
		
		if(_tickCount > -1 && idxSlot == _tickCount%tickPerWheel){	//cur slot has ticked
			if(roundCount <= 0){ //tick now
				timeout.onTimeout();
				return ;
			}
			--roundCount;
		}
		final DFTimeoutWrapper wrapper = _createTimeoutWrapper(timeout, roundCount); //new DFTimeoutWrapper(timeout, roundCount);
		_lsSlot.get(idxSlot).addTimeoutWrapper(wrapper);
	}
	private final LinkedList<DFTimeoutWrapper> _timeoutWrapPool = new LinkedList<>();
	private int _timeoutPoolSize = 0;
	private DFTimeoutWrapper _createTimeoutWrapper(DFTimeout timeout, int roundCount){
		final DFTimeoutWrapper wrap = _timeoutWrapPool.poll();
		if(wrap == null){
			return new DFTimeoutWrapper(timeout, roundCount);
		}else{
			wrap.reset(timeout, roundCount);
			--_timeoutPoolSize;
			return wrap;
		}
	}
	private void _returnTimeoutWrapper(DFTimeoutWrapper wrap){
		if(_timeoutPoolSize < 1000){
			_timeoutWrapPool.offer(wrap);
			++_timeoutPoolSize;
		}
	}
	
	public void addSchedule(long tickDelay, DFScheduleTick scheduleTick, long tmNow){
		final DFTimerEvent event = new DFTimerEvent(EVENT_ADD_SCHEDULE, tickDelay, tmNow, scheduleTick);
		_queueEvent.offer(event);
	}
	private void _addSchedule(long tickDelay, DFScheduleTick scheduleTick, long tmNow){
		final long tickCountBegin = Math.max(0, _tickCount) + 1;// + _scheduleTickInitRange;
		final long tmpTickCount = tickCountBegin + _scheduleTickCount;
		_scheduleTickCount = ++_scheduleTickCount%_scheduleTickInitRange;
		
		final int idxSlot = (int) (tmpTickCount%tickPerWheel);
		final int delayTick = (int) (tickDelay/tickDuration);
		//
		final DFScheduleWrapper wrapper = new DFScheduleWrapper(scheduleTick, delayTick, tmNow);
		_lsSlot.get(idxSlot).addScheduleWrapper(wrapper);
	}
	
	private long _tmpTickCount = 0;
	private int _tmpOff = 0;
	private int _tmpTickCountBegin = 0;
	private int _eventCount = 0;
	private DFTimerEvent _event = null;
	public void onTick(final long tmNow, final long dlt){
		//check event
		if(!_queueEvent.isEmpty()){
			_eventCount = 0;
			_event = null;
			while( (_event = _queueEvent.poll()) != null){
				if(_event.event == EVENT_ADD_TIMEOUT){
					this._addTimeout(_event.extLong1, (DFTimeout) _event.extObj);
				}else if(_event.event == EVENT_ADD_SCHEDULE){
					this._addSchedule(_event.extLong1, (DFScheduleTick) _event.extObj, _event.extLong2);
				}
				if(++_eventCount > 1000){
					break;
				}
			}
		}
		//
		_tmTotal += dlt;
		if(_tmTotal > 0){
			_tmpTickCount = _tmTotal/tickDuration;
			_tmpOff = (int) (_tmpTickCount - _tickCount);
			if(_tmpOff > 0){
				_tmpTickCountBegin = (int) (_tickCount + 1);
				_tickCount = _tmpTickCount;
				for(int i=0; i<_tmpOff; ++i){
					_curSlotIdx = (int) ((_tmpTickCountBegin + i)%tickPerWheel);
					_lsSlot.get(_curSlotIdx).onTick(tmNow, dlt);
				}
			}
		}else if(_tickCount < 0){  //init tick
			_curSlotIdx = 0;
			_tickCount = 0;
			_lsSlot.get(_curSlotIdx).onTick(tmNow, dlt);
		}
	}
	
	class DFHashWheelSlot{
		protected final int idx;
		private int timeoutNum = 0;
		private int scheduleNum = 0;
		protected DFHashWheelSlot(final int idx) {
			this.idx = idx;
		}
		
		private LinkedList<DFTimeoutWrapper> lsTimeoutWrapper = new LinkedList<>();
		private LinkedList<DFScheduleWrapper> lsScheduleWrapper = new LinkedList<>();
		
		public void addTimeoutWrapper(DFTimeoutWrapper wrapper){
			lsTimeoutWrapper.add(wrapper);
			++timeoutNum;
		}
		public void addScheduleWrapper(DFScheduleWrapper wrapper){
			lsScheduleWrapper.add(wrapper);
			++scheduleNum;
		}
		
		public void onTick(final long tmNow, final long dlt){
			//tick timeout
			if(!lsTimeoutWrapper.isEmpty()){
				final Iterator<DFTimeoutWrapper> itTimeout = lsTimeoutWrapper.iterator();
				while(itTimeout.hasNext()){
					final DFTimeoutWrapper wrapper = itTimeout.next();
					if(wrapper.roundCount-- <= 0){
						itTimeout.remove();
						--timeoutNum;
						wrapper.timeout.onTimeout();
						//back to pool
						_returnTimeoutWrapper(wrapper);
					}
				}
			}
			//tick schedule
			if(!lsScheduleWrapper.isEmpty()){
				final Iterator<DFScheduleWrapper> itSchedule = lsScheduleWrapper.iterator();
				while(itSchedule.hasNext()){
					final DFScheduleWrapper wrapper = itSchedule.next();
					if(wrapper.onTick(tmNow, dlt) == 0){ //continue schedule
						//add to next tick slot
						final int nextTick = (int) (_tickCount + wrapper.delayTick);
						int tmpIdx = nextTick%tickPerWheel;
						if(tmpIdx != this.idx){
							itSchedule.remove();
							_lsSlot.get(tmpIdx).addScheduleWrapper(wrapper);
						}
					}else{
						itSchedule.remove();
						--scheduleNum;
					}
				}
			}
		}
	}
	
	class DFScheduleWrapper{
		protected final DFScheduleTick scheduleTick;
		protected final int delayTick;
		private long tmLastTick = 0;
		public DFScheduleWrapper(DFScheduleTick scheduleTick, int delayTick, long tmNow) {
			this.scheduleTick = scheduleTick;
			this.delayTick = delayTick;
			this.tmLastTick = tmNow;
		}
		
		protected final int onTick(final long tmNow, final long dlt){
			final int ret = scheduleTick.onScheduleTick(tmNow - tmLastTick);
			this.tmLastTick = tmNow;
			return ret;
		}
	}
	
	class DFTimeoutWrapper{
		protected DFTimeout timeout;
		protected int roundCount = 0;
		protected int roundCountBase;
		
		protected DFTimeoutWrapper(DFTimeout timeout, int roundCount) {
			this.timeout = timeout;
			this.roundCount = roundCount;
			this.roundCountBase = roundCount;
		}
		protected void reset(DFTimeout timeout, int roundCount){
			this.timeout = timeout;
			this.roundCount = roundCount;
			this.roundCountBase = roundCount;
		}
	}
	
	final class DFTimerEvent{
		protected final byte event;
		protected final long extLong1;
		protected final long extLong2;
		protected final Object extObj;
		
		protected DFTimerEvent(final byte event, final long extLong1,
				final long extLong2, final Object extObj) {
			this.event = event;
			this.extLong1 = extLong1;
			this.extLong2 = extLong2;
			this.extObj = extObj;
		}
	}
	
	private static final byte EVENT_ADD_TIMEOUT = 1;
	private static final byte EVENT_ADD_SCHEDULE = 2;
}

