package fun.lib.actor.deprecated;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.funtag.util.log.DFLogFactory;
import com.funtag.util.log.DFLogger;
import fun.lib.actor.kcp.Kcp;
import fun.lib.actor.kcp.KcpChannel;
import fun.lib.actor.kcp.KcpConfig;
import fun.lib.actor.kcp.KcpConfigInner;
import fun.lib.actor.kcp.KcpListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public final class UdpTestClient {

static final DFLogger log = DFLogFactory.create(UdpTestClient.class);
	
	static volatile Channel channel = null;
	static final String host = "127.0.0.1"; // "106.15.37.38"; //    
	static final int port = 13500;
	
	public static void main(String[] args) {
		final EventLoopGroup ioGroup = new NioEventLoopGroup(1);
		//start listen
				Bootstrap boot = new Bootstrap();
				boot.group(ioGroup)
					.option(ChannelOption.SO_BROADCAST, false)
					.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
					.option(ChannelOption.SO_SNDBUF, 1024*10)
					.option(ChannelOption.SO_RCVBUF, 1024*10)
					.channel(NioDatagramChannel.class)
					.handler(new UdpHandlerTestClient());
				try{
					ChannelFuture future = boot.bind(0).sync(); 
					channel = future.channel();
					future.addListener(new GenericFutureListener<Future<? super Void>>() {
						@Override
						public void operationComplete(Future<? super Void> f) throws Exception {
							boolean isDone = f.isDone();
							boolean isSucc = f.isSuccess();
							boolean isCancel = f.isCancelled();
							if(isDone && isSucc){  //listen
								log.I("Init udp succ");
							}else{
								//shutdown io group
								ioGroup.shutdownGracefully();
							}
						}
					});
				}catch(Throwable e){
					e.printStackTrace();
				}
				//start loop
				ExecutorService thPool = Executors.newFixedThreadPool(1);
				thPool.submit(new UdpTestClientLoop());
	}
	static class UdpHandlerTestClient extends SimpleChannelInboundHandler<DatagramPacket>{
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			log.I("channelActive");
		}
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket pack) throws Exception {
			final ByteBuf buf = pack.content();
			final int size = buf.readableBytes();
			if(size > 0){
				int connId = 0;
				if(buf.readByte()==Kcp.FLAG && size > 1 + 4){ //valid kcp head
					connId = buf.getInt(buf.readerIndex());
				}
				if(connId > 0){ //valid kcp pack
					pack.retain();
					queueRecv.offer(pack);
//					log.I("Recv kcp pack, sender="+pack.sender().toString());
				}else{  //normal udp pack
					log.I("Recv udp pack, sender="+pack.sender().toString());
				}
			}else{
				log.E("Invalid pack, len=0, sender="+pack.sender().toString());
			}
		}
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			cause.printStackTrace();
		}
	}
	
	static LinkedBlockingQueue<DatagramPacket> queueRecv = new LinkedBlockingQueue<>();
	static class UdpTestClientLoop implements Runnable{
		@Override
		public void run() {
			int dataCount = 0;
			boolean onLoop = true;
			long tmNow = 0;
			long tmLastSend = 0;
			int sendCount = 0;
			while(onLoop){
				try{
					final DatagramPacket pack = queueRecv.poll(10, TimeUnit.MILLISECONDS);
					if(pack != null){
						final ByteBuf buf = pack.content();
						pack.release();
					}
					//
					tmNow = System.currentTimeMillis();
					if(tmNow - tmLastSend >= 1000 && ++sendCount < 2000000){
						tmLastSend = tmNow;
						//send
						if(channel != null){
							try {
								byte[] bufData = ("hehe_"+(++dataCount)).getBytes("utf-8");
								final ByteBuf bufRsp = PooledByteBufAllocator.DEFAULT.buffer(
//										8 + 
										bufData.length);
//								bufRsp.writeLong(tmNow);
								bufRsp.writeBytes(bufData);
								//
								InetSocketAddress addr = new InetSocketAddress(host, port);
								
								channel.writeAndFlush(new DatagramPacket(bufRsp, addr));
							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}catch(Throwable e){
					e.printStackTrace();
				}
				
			}
		}
		
	}

}
