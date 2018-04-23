package fun.lib.actor.example;

import java.util.Random;

import fun.lib.actor.core.DFActor;
import fun.lib.actor.core.DFActorManager;

/**
 * 猫捉老鼠演示多actor通信
 * @author lostsky
 *
 */
public final class TomAndJerry {

	public static void main(String[] args) {
		final DFActorManager mgr = DFActorManager.get();
		//启动dfactor
		mgr.start("Director", Director.class);
	}
	//导演
	private static class Director extends DFActor{
		private final Random rand = new Random();
		public Director(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onStart(Object param) {
			log.info("Director start, curThread="+Thread.currentThread().getName());
			//release jerry
			int spd = 5 + rand.nextInt(3); //rand jerry speed
			//create jerry
			sys.createActor("Jerry", Jerry.class, new Integer(spd), DFActor.transTimeRealToTimer(1000));
			//start timeout
			int delay = DFActor.transTimeRealToTimer(5000);//delay 5 sec
			sys.timeout(delay, 1);
		}
		@Override
		public void onTimeout(int requestId) {
			//release tom
			int spd = 7 + rand.nextInt(3); //tom speed
			//create tom
			sys.createActor("Tom", Tom.class, new Integer(spd), DFActor.transTimeRealToTimer(1000));
		}
		private int posTom = 0;
		private int posJerry = 0;
		private boolean gameOver = false;
		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			if(gameOver){
				return 0;
			}
			if(cmd == 1001){  //tom report pos
				posTom = (Integer)payload;
				log.info("Tom curPos="+posTom+",    curThread="+Thread.currentThread().getName());
			}else if(cmd == 1002){ //jerry report pos
				posJerry = (Integer)payload;
				log.info("Jerry curPos="+posJerry+",    curThread="+Thread.currentThread().getName());
			}
			if(posJerry > 0 && posTom > 0){ //pos valid
				if(posTom >= posJerry){ //tom got jerry
					gameOver = true;
					sys.send("Tom", 0, 1003, new Boolean(true));
					sys.send("Jerry", 0, 1003, new Boolean(true));
				}else if(posJerry >= 150){ //jerry escaped
					gameOver = true;
					sys.send("Tom", 0, 1003, new Boolean(false));
					sys.send("Jerry", 0, 1003, new Boolean(false));
				}
			}
			return 0;
		}
	}
	//Tom
	private static class Tom extends DFActor{
		public Tom(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
		private int spd = 0;
		private int curPos = 0; 
		@Override
		public void onStart(Object param) {
			spd = (Integer) param;
			log.info("Tom start run, spd="+spd+",    curThread="+Thread.currentThread().getName());
		}
		@Override
		public void onSchedule(long dltMilli) {
			curPos = curPos + (int)(spd*dltMilli*1.0f/1000);
			//notify director
			sys.send("Director", 0, 1001, new Integer(curPos));
		}
		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			if(cmd == 1003){ //game over
				boolean got = (Boolean)payload;
				if(got){
					log.info("Tom got jerry, win!    curThread="+Thread.currentThread().getName());
				}else{
					log.info("Tom not got jerry, lose!    curThread="+Thread.currentThread().getName());
				}
				//tom exit
				sys.exit();
			}
			return 0;
		}
	}
	//Jerry
	private static class Jerry extends DFActor{
		public Jerry(Integer id, String name, Integer consumeType, Boolean isBlockActor) {
			super(id, name, consumeType, isBlockActor);
		}
		private int spd = 0;
		private int curPos = 0; 
		@Override
		public void onStart(Object param) {
			spd = (Integer) param;
			log.info("Jerry start run, spd="+spd+",    curThread="+Thread.currentThread().getName());
		}
		@Override
		public void onSchedule(long dltMilli) {
			curPos = curPos + (int)(spd*dltMilli*1.0f/1000);
			//notify director
			sys.send("Director", 0, 1002, new Integer(curPos));
		}
		@Override
		public int onMessage(int srcId, int requestId, int subject, int cmd, Object payload) {
			if(cmd == 1003){ //game over
				boolean got = (Boolean)payload;
				if(got){
					log.info("Jerry has be got! die!    curThread="+Thread.currentThread().getName());
				}else{
					log.info("Jerry escaped!    curThread="+Thread.currentThread().getName());
				}
				//delay exit
				int delay = DFActor.transTimeRealToTimer(2000);
				sys.timeout(delay, 0);
			}
			return 0;
		}
		@Override
		public void onTimeout(int requestId) {
			log.info("shutdown dfactor,    curThread="+Thread.currentThread().getName());
			DFActorManager.get().shutdown();
		}
	}
}
