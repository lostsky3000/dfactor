package fun.lib.actor.kcp;

import io.netty.channel.socket.DatagramPacket;

public final class KcpServer {

	private final int id;
	private final int threadNum;
	private volatile KcpLoop[] arrLoop = null;
	private final KcpListener listener;
	private final KcpConfig kcpCfg;
	
	public KcpServer(int id, int threadNum, KcpListener listener, KcpConfig kcpCfg) {
		this.id = id;
		threadNum = Math.max(1, threadNum);
		this.threadNum = threadNum;
		this.listener = listener;
		this.kcpCfg = kcpCfg;
	}
	
	public int start(){
		int ret = -1;
		do {
			if(arrLoop != null){
				ret = 1; break;
			}
			//start loop thread
			arrLoop = new KcpLoop[this.threadNum];
			for(int i=0; i<this.threadNum; ++i){
				final KcpLoop loop = new KcpLoop(i, listener, kcpCfg);
				final Thread th = new Thread(loop);
				arrLoop[i] = loop;
				th.setName("df-kcp-loop-"+this.id+"-"+i);
				th.start();
			}
			ret = 0;
		} while (false);
		return ret;
	}
	
	public int onReceive(DatagramPacket pack, int connId){
		return arrLoop[connId%this.threadNum].onReceive(pack);
	}
	
	private volatile boolean stoped = false;
	public void stop(){
		if(stoped){
			return ;
		}
		stoped = true;
		if(arrLoop != null){
			for(int i=0; i<this.threadNum; ++i){
				arrLoop[i].stop();
			}
			arrLoop = null;
		}
	}
	
	
}
