package fun.lib.actor.deprecated;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.funtag.util.log.DFLogFactory;
import com.funtag.util.log.DFLogger;

import fun.lib.actor.core.DFActorDefine;
import fun.lib.actor.core.DFUdpChannelWrapper;
import fun.lib.actor.define.DFActorErrorCode;
import fun.lib.actor.kcp.Kcp;
import fun.lib.actor.kcp.KcpChannel;
import fun.lib.actor.kcp.KcpConfig;
import fun.lib.actor.kcp.KcpListener;
import fun.lib.actor.kcp.KcpServer;
import fun.lib.actor.po.DFActorEvent;
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

public final class KcpTestServer {
	private static final DFLogger log = DFLogFactory.create(KcpTestServer.class);
	
	static volatile Channel channel = null;
	static volatile KcpServer kcpSys = null;
	public static void main(String[] args) {
		final int port = 13500;
		final EventLoopGroup ioGroup = new NioEventLoopGroup(1);
		//start listen
				Bootstrap boot = new Bootstrap();
				boot.group(ioGroup)
					.option(ChannelOption.SO_BROADCAST, false)
					.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
					.option(ChannelOption.SO_SNDBUF, 1024*10)
					.option(ChannelOption.SO_RCVBUF, 1024*10)
					.channel(NioDatagramChannel.class)
					.handler(new KcpTestHandlerServer());
				try{
					ChannelFuture future = boot.bind(port); 
					channel = future.channel();
					future.addListener(new GenericFutureListener<Future<? super Void>>() {
						@Override
						public void operationComplete(Future<? super Void> f) throws Exception {
							boolean isDone = f.isDone();
							boolean isSucc = f.isSuccess();
							boolean isCancel = f.isCancelled();
							if(isDone && isSucc){  //listen
								log.I("Listen udp on port "+port);
							}else{
								//shutdown io group
								ioGroup.shutdownGracefully();
							}
						}
					});
				}catch(Throwable e){
					e.printStackTrace();
				}
				//
				final KcpListener l = new KcpListener() {
					private int recvCount = 0;
					@Override
					public void onOutput(DatagramPacket pack) {
						channel.writeAndFlush(pack);
					}
					@Override
					public void onInput(DatagramPacket pack, KcpChannel kcpChannel) {
						final ByteBuf buf = pack.content();
						long tmSend = buf.readLong();
						final String data = (String) buf.readCharSequence(buf.readableBytes(), Charset.forName("utf-8"));
						pack.release();
						
						final Kcp kcp = (Kcp) kcpChannel;
//						log.I("Kcp recv data, connId="+kcpChannel.getConnId()+", data="+data
//								+", recvWndL="+kcp.getRecvWndL()
//								+", recvWndR="+kcp.getRecvWndR());
						//rsp
						try {
							byte[] bufData = (data+"_"+(recvCount++)).getBytes("utf-8");
							final ByteBuf bufRsp = PooledByteBufAllocator.DEFAULT.buffer(
									Kcp.KCP_HEAD_SIZE + 8 + bufData.length);
							bufRsp.writeZero(Kcp.KCP_HEAD_SIZE);
							bufRsp.writeLong(tmSend);
							bufRsp.writeBytes(bufData);
							kcpChannel.write(bufRsp);
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					@Override
					public void onChannelInactive(KcpChannel kcpChannel, int code) {
						log.I("Kcp onChannelInactive, connId="+kcpChannel.getConnId()+", code="+code);
					}
					@Override
					public void onChannelActive(KcpChannel kcpChannel, int connId) {
						log.I("Kcp onChannelActive, connId="+connId);
					}
				};
				KcpConfig kcpCfg = new KcpConfig();
				kcpSys = new KcpServer(1, 1, l, kcpCfg);
				kcpSys.start();
				log.I("Kcp server start");
				
	}
	
	static class KcpTestHandlerServer extends SimpleChannelInboundHandler<DatagramPacket>{
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
					kcpSys.onReceive(pack, connId);
//					log.I("Recv kcp pack, sender="+pack.sender().toString());
				}else{  //normal udp pack
					log.I("Recv udp pack, sender="+pack.sender().toString());
				}
			}else{
				log.E("Invalid pack, len=0, sender="+pack.sender().toString());
			}
		}
		
	}

}
