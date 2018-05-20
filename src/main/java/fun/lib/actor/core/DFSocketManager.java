package fun.lib.actor.core;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.concurrent.locks.StampedLock;

import com.funtag.util.system.DFSysUtil;
import fun.lib.actor.api.DFActorTcpDispatcher;
import fun.lib.actor.api.DFTcpDecoder;
import fun.lib.actor.api.DFTcpEncoder;
import fun.lib.actor.api.DFUdpDecoder;
import fun.lib.actor.api.cb.CbHttpClient;
import fun.lib.actor.api.cb.CbHttpServer;
import fun.lib.actor.api.http.DFHttpDispatcher;
import fun.lib.actor.api.DFActorUdpDispatcher;
import fun.lib.actor.define.DFActorErrorCode;
import fun.lib.actor.po.DFActorEvent;
import fun.lib.actor.po.DFTcpClientCfg;
import fun.lib.actor.po.DFTcpServerCfg;
import fun.lib.actor.po.DFUdpServerCfg;
import fun.lib.actor.po.SslConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public final class DFSocketManager {

	private static DFSocketManager instance = new DFSocketManager();
	private final DFActorManager actorMgr;
	private DFSocketManager(){
		actorMgr = DFActorManager.get();
	}
	protected static DFSocketManager get(){
		return instance;
	}
	
	//udp
	private final HashMap<Integer, DFUdpIoGroup> mapUdpGroup = new HashMap<>();
	private final ReentrantReadWriteLock lockUdpSvr = new ReentrantReadWriteLock();
	private final ReadLock readLockUdpSvr = lockUdpSvr.readLock();
	private final WriteLock writeLockUdpSvr = lockUdpSvr.writeLock();
	
	protected void doUdpListen(final DFUdpServerCfg cfg, final int defaultActorId, DFActorUdpDispatcher dispatcher,
			final int requestId){
		//start listen
		final DFUdpChannelWrap channelWrapper = new DFUdpChannelWrap();
		final DFUdpIoGroup group = new DFUdpIoGroup(cfg, defaultActorId);
		//
		Bootstrap boot = new Bootstrap();
		boot.group(group.ioGroup)
			.option(ChannelOption.SO_BROADCAST, cfg.isBroadcast)
			.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
			.option(ChannelOption.SO_SNDBUF, cfg.getSoSendBuf())
			.option(ChannelOption.SO_RCVBUF, cfg.getSoRecvBuf())
			.channel(NioDatagramChannel.class)
			.handler(new UdpHandler(defaultActorId, dispatcher, cfg.getDecoder(), cfg.port, requestId, channelWrapper));
		try{
			ChannelFuture future = boot.bind(cfg.port); 
//			final DFUdpChannelWrapper channelWrapper = new DFUdpChannelWrapper(future.channel());
			channelWrapper.onChannelActive(future.channel());
			future.addListener(new GenericFutureListener<Future<? super Void>>() {
				@Override
				public void operationComplete(Future<? super Void> f) throws Exception {
					boolean isDone = f.isDone();
					boolean isSucc = f.isSuccess();
					boolean isCancel = f.isCancelled();
					if(isDone && isSucc){  //listen
						writeLockUdpSvr.lock();
						try{
							mapUdpGroup.put(cfg.port, group);
						}finally{
							writeLockUdpSvr.unlock();
						}
						//notify eventActor & msgActor
						final DFActorEvent event = new DFActorEvent(DFActorErrorCode.SUCC)
								.setExtInt1(cfg.port).setExtObj1(channelWrapper);
						actorMgr.send(0, defaultActorId, requestId, DFActorDefine.SUBJECT_NET, 
								DFActorDefine.NET_UDP_LISTEN_RESULT, event, true);
					}else{
						//notify actor
						final String errMsg = f.cause().getMessage();
						final DFActorEvent event = new DFActorEvent(DFActorErrorCode.FAILURE, errMsg)
								.setExtInt1(cfg.port);
						actorMgr.send(0, defaultActorId, requestId, DFActorDefine.SUBJECT_NET, 
								DFActorDefine.NET_UDP_LISTEN_RESULT, event, true);
						//shutdown io group
						group.shutdownIoGroup();
					}
				}
			});
		}catch(Throwable e){
			//notify actor
			final String errMsg = e.getMessage();
			final DFActorEvent event = new DFActorEvent(DFActorErrorCode.FAILURE, errMsg)
					.setExtInt1(cfg.port);
			actorMgr.send(0, defaultActorId, 0, DFActorDefine.SUBJECT_NET, 
					DFActorDefine.NET_UDP_LISTEN_RESULT, event, true);
			//shutdown io group
			group.shutdownIoGroup();
		}
	}
	protected void doUdpListenClose(int port){
		DFUdpIoGroup group = null;
		readLockUdpSvr.lock();
		try{
			group = mapUdpGroup.get(port);
		}finally{
			readLockUdpSvr.unlock();
		}
		int retClose = -1;
		if(group != null){
			writeLockUdpSvr.lock();
			try{
				retClose = group.shutdownIoGroup();
				mapUdpGroup.remove(port);
			}finally{
				writeLockUdpSvr.unlock();
			}
			if(retClose == 0){ //shutdown succ
				// notify actor
				final DFActorEvent event = new DFActorEvent(DFActorErrorCode.SUCC)
						.setExtInt1(group.cfg.port);
				actorMgr.send(0, group.defaultActorId, 0, DFActorDefine.SUBJECT_NET, 
						DFActorDefine.NET_UDP_LISTEN_CLOSED, event, true);
			}
			group = null;
		}
	}
	class DFUdpIoGroup {
		protected final EventLoopGroup ioGroup;
		protected final DFUdpServerCfg cfg;
		protected final int defaultActorId;
		protected DFUdpIoGroup(DFUdpServerCfg cfg, int defaultActorId) {
			this.cfg = cfg;
			this.defaultActorId = defaultActorId;
			if(DFSysUtil.isLinux()){
				ioGroup = new EpollEventLoopGroup(cfg.ioThreadNum);
			}else{
				ioGroup = new NioEventLoopGroup(cfg.ioThreadNum);
			}
			
		}
		private volatile boolean _hasShutdown = false;
		protected synchronized int shutdownIoGroup(){
			if(_hasShutdown){
				return -2;
			}
			_hasShutdown = true;
			int ret = -1;
			try{
				if(ioGroup != null){
					if(ioGroup.isShutdown() || ioGroup.isShuttingDown() || ioGroup.isTerminated()){
					}else{
						ioGroup.shutdownGracefully();
						ret = 0;
					}
				}
			}catch(Throwable e){
				e.printStackTrace();
				ret = 1;
			}
			return ret;
		}
	}
	class UdpHandler extends SimpleChannelInboundHandler<DatagramPacket>{
		private final DFActorUdpDispatcher dispatcher;
		private final DFUdpDecoder decoder;
		private final int actorIdDef;
		private final int port;
		private final int requestId;
		private final DFUdpChannelWrap channel;
		protected UdpHandler(int actorIdDef, DFActorUdpDispatcher dispatcher, DFUdpDecoder decoder, int port, int requestId, DFUdpChannelWrap channel) {
			this.actorIdDef = actorIdDef;
			this.dispatcher = dispatcher;
			this.decoder = decoder;
			this.port = port;
			this.requestId = requestId;
			this.channel = channel;
		}
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket pack) throws Exception {
			try{
				Object msg = null;
				if(decoder != null){
					msg = decoder.onDecode(pack);
				}
				boolean isPack = false;
				if(msg == null){
					msg = pack;
					isPack = true;
				}
				int actorId = 0;
				if(dispatcher != null){
					actorId = dispatcher.onQueryMsgActorId(msg);
				}
				if(actorId == 0){
					actorId = actorIdDef;
				}
				if(actorId > 0){
					if(actorMgr.send(0, actorId, requestId, DFActorDefine.SUBJECT_NET, 
							DFActorDefine.NET_UDP_MESSAGE, msg, true, channel, null, false) == 0){ //send to queue succ
						if(isPack){
							pack.retain();
						}
					}
				}
			}catch(Throwable e){
				e.printStackTrace();
			}
		}
	}
	
	
	//tcp
	protected int doTcpConnect(final DFTcpClientCfg cfg, final int srcActorId,
			final EventLoopGroup ioGroup, final int requestId){
		return _doTcpConnect(cfg, srcActorId, null, ioGroup, requestId);
	}
	protected int doTcpConnect(final DFTcpClientCfg cfg, final int srcActorId, DFActorTcpDispatcher dispatcher,
			final EventLoopGroup ioGroup, final int requestId){
		return _doTcpConnect(cfg, srcActorId, dispatcher, ioGroup, requestId);
	}
	private int _doTcpConnect(final DFTcpClientCfg cfg, final int srcActorId, 
			DFActorTcpDispatcher dispatcher,
			final EventLoopGroup ioGroup, final int requestId){
		if(ioGroup == null){
			return 1;
		}
		Bootstrap boot = new Bootstrap();
		boot.group(ioGroup)
			.option(ChannelOption.ALLOCATOR, 
					PooledByteBufAllocator.DEFAULT)
			.option(ChannelOption.SO_KEEPALIVE, cfg.isKeepAlive())
			.option(ChannelOption.SO_RCVBUF, cfg.getSoRecvBufLen())
			.option(ChannelOption.SO_SNDBUF, cfg.getSoSendBufLen())
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int)cfg.getConnTimeout())
			.option(ChannelOption.TCP_NODELAY, cfg.isTcpNoDelay())
			.handler(new TcpHandlerInit(false, cfg.getTcpProtocol(), 
					cfg.getTcpMsgMaxLength(), srcActorId, requestId, null, dispatcher, 
					cfg.getDecoder(), cfg.getEncoder(), cfg.getUserHandler(), cfg.getSslCfg()
					, cfg.getReqData()));
		if(DFSysUtil.isLinux()){
			boot.channel(EpollSocketChannel.class);
		}else{
			boot.channel(NioSocketChannel.class);
		}
			ChannelFuture future = boot.connect(cfg.host, cfg.port);
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture f) throws Exception {
					if(f.isSuccess()){ 	//succ
						//notify actor
						final DFActorEvent event = new DFActorEvent(DFActorErrorCode.SUCC)
								.setExtString1(cfg.host).setExtInt1(cfg.port);
						actorMgr.send(0, srcActorId, requestId, DFActorDefine.SUBJECT_NET, 
								DFActorDefine.NET_TCP_CONNECT_RESULT, event, true, null, cfg.getUserHandler(), false);
					}else{		//failed
						//notify actor
						final String errMsg = f.cause().getMessage();
						final DFActorEvent event = new DFActorEvent(DFActorErrorCode.FAILURE, errMsg)
								.setExtString1(cfg.host).setExtInt1(cfg.port);
						actorMgr.send(0, srcActorId, requestId, DFActorDefine.SUBJECT_NET, 
								DFActorDefine.NET_TCP_CONNECT_RESULT, event, true, null, cfg.getUserHandler(), false);
					}
				}
			});
		return 0;
	}
	
	private final HashMap<Integer, DFTcpIoGroup> mapTcpGroup = new HashMap<>();
	private final ReentrantReadWriteLock lockTcpSvr = new ReentrantReadWriteLock();
	private final ReadLock lockTcpSvrRead = lockTcpSvr.readLock();
	private final WriteLock lockTcpSvrWrite = lockTcpSvr.writeLock();
//	private final StampedLock lockTcpSvr = new StampedLock();
	
	protected void doTcpListen(final DFTcpServerCfg cfg, final int srcActorId, final int requestId){
		_doTcpListen(cfg, srcActorId, null, requestId);
	}
	protected void doTcpListen(final DFTcpServerCfg cfg, final int srcActorId, Object dispatcher, final int requestId){
		_doTcpListen(cfg, srcActorId, dispatcher, requestId);
	}
	private void _doTcpListen(final DFTcpServerCfg cfg, final int srcActorId, Object dispatcher, final int requestId){
		//start listen
		final DFTcpIoGroup group = new DFTcpIoGroup(cfg, srcActorId, requestId);
		ServerBootstrap boot = new ServerBootstrap();
		boot.group(group.ioGroupBoss, group.ioGroupWorker)
			.option(ChannelOption.ALLOCATOR, 
					PooledByteBufAllocator.DEFAULT) //!!!!!
			.option(ChannelOption.SO_BACKLOG, cfg.getSoBackLog())
			.childOption(ChannelOption.ALLOCATOR, 
					PooledByteBufAllocator.DEFAULT) //!!!!!
			.childOption(ChannelOption.SO_KEEPALIVE, cfg.isKeepAlive())
			.childOption(ChannelOption.SO_RCVBUF, cfg.getSoRecvBufLen())
			.childOption(ChannelOption.SO_SNDBUF, cfg.getSoSendBufLen())
			.childOption(ChannelOption.TCP_NODELAY, cfg.isTcpNoDelay())
			.childHandler(new TcpHandlerInit(true, cfg.getTcpProtocol(), 
					cfg.getTcpMsgMaxLength(), srcActorId, requestId, cfg.getWsUri(), dispatcher, 
					cfg.getDecoder(), cfg.getEncoder(), cfg.getUserHandler(), cfg.getSslConfig(), null));
		if(DFSysUtil.isLinux()){
			boot.channel(EpollServerSocketChannel.class);
		}else{
			boot.channel(NioServerSocketChannel.class);
		}
		try{
			ChannelFuture future = boot.bind(cfg.port); 
			future.addListener(new GenericFutureListener<Future<? super Void>>() {
				@Override
				public void operationComplete(Future<? super Void> f) throws Exception {
					boolean isDone = f.isDone();
					boolean isSucc = f.isSuccess();
					boolean isCancel = f.isCancelled();
					if(isDone && isSucc){  //listen
						lockTcpSvrWrite.lock();
						try{
							mapTcpGroup.put(cfg.port, group);
						}finally{
							lockTcpSvrWrite.unlock();
						}
						//notify actor
						final DFActorEvent event = new DFActorEvent(DFActorErrorCode.SUCC)
								.setExtInt1(cfg.port);
						actorMgr.send(0, srcActorId, requestId, DFActorDefine.SUBJECT_NET, 
								DFActorDefine.NET_TCP_LISTEN_RESULT, event, true, null, cfg.getUserHandler(), false);
					}else{
						//notify actor
						final String errMsg = f.cause().getMessage();
						final DFActorEvent event = new DFActorEvent(DFActorErrorCode.FAILURE, errMsg)
								.setExtInt1(cfg.port);
						actorMgr.send(0, srcActorId, requestId, DFActorDefine.SUBJECT_NET, 
								DFActorDefine.NET_TCP_LISTEN_RESULT, event, true, null, cfg.getUserHandler(), false);
						//shutdown io group
						group.shutdownIoGroup();
					}
				}
			});
		}catch(Throwable e){
			//notify actor
			final String errMsg = e.getMessage();
			final DFActorEvent event = new DFActorEvent(DFActorErrorCode.FAILURE, errMsg)
					.setExtInt1(cfg.port);
			actorMgr.send(0, srcActorId, cfg.port, DFActorDefine.SUBJECT_NET, 
					DFActorDefine.NET_TCP_LISTEN_RESULT, event, true);
			//shutdown io group
			group.shutdownIoGroup();
		}
	}
	
	protected void doTcpListenClose(int port){
		DFTcpIoGroup group = null;
		lockTcpSvrRead.lock();
		try{
			group = mapTcpGroup.get(port);
		}finally{
			lockTcpSvrRead.unlock();
		}
		int retClose = -1;
		if(group != null){
			lockTcpSvrWrite.lock();
			try{
				retClose = group.shutdownIoGroup();
				mapTcpGroup.remove(port);
			}finally{
				lockTcpSvrWrite.unlock();
			}
			if(retClose == 0){ //shutdown succ
				// notify actor
				final DFActorEvent event = new DFActorEvent(DFActorErrorCode.SUCC)
						.setExtInt1(group.cfg.port);
				actorMgr.send(0, group.srcActorId, group.requestId, DFActorDefine.SUBJECT_NET, 
						DFActorDefine.NET_TCP_LISTEN_CLOSED, event, true);
			}
			group = null;
		}
	}
	
	protected void doTcpListenCloseAll(){
		lockTcpSvrWrite.lock();
		try{
			final Iterator<Entry<Integer, DFTcpIoGroup>> it = mapTcpGroup.entrySet().iterator();
			while(it.hasNext()){
				it.next().getValue().shutdownIoGroup();
			}
			mapTcpGroup.clear();
		}finally{
			lockTcpSvrWrite.unlock();
		}
	}
	
	class DFTcpIoGroup {
		protected final EventLoopGroup ioGroupBoss;
		protected final EventLoopGroup ioGroupWorker;
		protected final DFTcpServerCfg cfg;
		protected final int srcActorId;
		protected final int requestId;
		protected DFTcpIoGroup(DFTcpServerCfg cfg, int srcActorId, int requestId) {
			this.cfg = cfg;
			this.srcActorId = srcActorId;
			this.requestId = requestId;
			
			if(DFSysUtil.isLinux()){
				ioGroupWorker = new EpollEventLoopGroup(cfg.workerThreadNum);
			}else{
				ioGroupWorker = new NioEventLoopGroup(cfg.workerThreadNum);
			}
			
			
			if(cfg.bossThreadNum < 1){  //no boss thread
				ioGroupBoss = ioGroupWorker;
			}else{  //has boss group
				if(DFSysUtil.isLinux()){
					ioGroupBoss = new EpollEventLoopGroup(cfg.bossThreadNum);
				}else{
					ioGroupBoss = new NioEventLoopGroup(cfg.bossThreadNum);
				}
			}
		}
		private volatile boolean _hasShutdown = false;
		protected synchronized int shutdownIoGroup(){
			if(_hasShutdown){
				return -2;
			}
			_hasShutdown = true;
			int ret = -1;
			try{
				if(ioGroupBoss != null){
					if(ioGroupBoss.isShutdown() || ioGroupBoss.isShuttingDown() || ioGroupBoss.isTerminated()){
						
					}else{
						ioGroupBoss.shutdownGracefully();
						ret = 0;
					}
				}
				if(ioGroupWorker != null){
					if(ioGroupWorker.isShutdown() || ioGroupWorker.isShuttingDown() || ioGroupWorker.isTerminated()){
						
					}else{
						ioGroupWorker.shutdownGracefully();
						ret = 0;
					}
				}
			}catch(Throwable e){
				e.printStackTrace();
				ret = 1;
			}
			return ret;
		}
	}
	
	private class TcpHandler extends ChannelInboundHandlerAdapter{
		private final int _actorIdDef; //默认发送消息的actor
		private final int _requestId;
		private final int _decodeType;
		private final DFActorTcpDispatcher _dispatcher;
		private final DFTcpDecoder _decoder;
		private final DFTcpEncoder _encoder;
		private volatile DFTcpChannelWrap _session = null;
		private volatile int _sessionId = 0;
		private volatile InetSocketAddress _addrRemote = null;
		private TcpHandler(final int actorIdDef, final int requestId, final int decodeType, DFActorTcpDispatcher dispatcher,
				DFTcpDecoder decoder, DFTcpEncoder encoder) {
			_actorIdDef = actorIdDef;
			_requestId = requestId;
			_decodeType = decodeType;
			_dispatcher = dispatcher;
			_decoder = decoder;
			_encoder = encoder;
		}
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			_addrRemote = (InetSocketAddress) ctx.channel().remoteAddress();
			_session = new DFTcpChannelWrap(_addrRemote.getHostString(), _addrRemote.getPort(), 
					ctx.channel(), _decodeType, _encoder);
			_session.setOpenTime(System.currentTimeMillis());
			_sessionId = _session.getChannelId();
			//
			int actorId = 0;
			if(_dispatcher != null){
				actorId = _dispatcher.onConnActiveUnsafe(_requestId, _sessionId, _addrRemote);
			}else{ //没有notify指定
				actorId = _actorIdDef;
			}
			if(actorId != 0){ //通知actor
				_session.setStatusActor(actorId);
				_session.setMessageActor(actorId);
				//notify actor
				actorMgr.send(0, actorId, _requestId, 
						DFActorDefine.SUBJECT_NET, DFActorDefine.NET_TCP_CONNECT_OPEN, _session, true);
			}
			super.channelActive(ctx);
		}
		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			_session.onClose();
			int actorId = 0;
			if(_dispatcher != null){
				actorId = _dispatcher.onConnInactiveUnsafe(_requestId, _sessionId, _addrRemote);
			}else{   //没有notify指定
				actorId = _session.getStatusActor();
			}
			if(actorId != 0){ //actor 有效
				//notify actor
				actorMgr.send(0, actorId, _requestId, 
						DFActorDefine.SUBJECT_NET, DFActorDefine.NET_TCP_CONNECT_CLOSE, _session, true);
			}
			_session = null;
			//
			super.channelInactive(ctx);
		}
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msgRaw) throws Exception {
			boolean releaseRaw = true;
			try{
				boolean hasDecode = false;
				Object msg = null;
				if(_decoder != null){ //有自定义解码器
					msg = _decoder.onDecode(msgRaw);
					if(msg != null && msg != msgRaw){ //已经过解码转换
						hasDecode = true;
					}
				}else{
					msg = msgRaw;
				}
				int actorId = 0;
				if(_dispatcher == null){
					actorId = _session.getMsgActor();
				}else{ //没有notify指定
					actorId = _dispatcher.onQueryMsgActorId(_requestId, _sessionId, _addrRemote, msg);
				}
				if(actorId != 0){ //actor有效
					//notify actor
					if(actorMgr.send(_requestId, actorId, _sessionId, 
							DFActorDefine.SUBJECT_NET, 
							DFActorDefine.NET_TCP_MESSAGE, //hasDecode?DFActorDefine.NET_TCP_MESSAGE_CUSTOM:DFActorDefine.NET_TCP_MESSAGE, 
							msg, true, _session, null, false) == 0){ //send to queue succ
						if(!hasDecode){  //未解码，传递的原始消息，不释放
							releaseRaw = false;
						}
					}
				}
			}finally{
				if(releaseRaw){
					ReferenceCountUtil.release(msgRaw);
				}
			}
			
		}
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			
		}
	}
	private class TcpWsHandler extends SimpleChannelInboundHandler<WebSocketFrame>{
		private final int _actorIdDef;
		private final int _requestId;
		private final int _decodeType;
		private final DFActorTcpDispatcher _dispatcher;
		private final DFTcpDecoder _decoder;
		private final DFTcpEncoder _encoder;
		private volatile DFTcpChannelWrap _session = null;
		private volatile int _sessionId = 0;
		private volatile InetSocketAddress _addrRemote = null;
		public TcpWsHandler(int actorIdDef, int requestId, int decodeType, DFActorTcpDispatcher dispatcher, 
				DFTcpDecoder decoder, DFTcpEncoder encoder) {
			_actorIdDef = actorIdDef;
			_requestId = requestId;
			_decodeType = decodeType;
			_dispatcher = dispatcher;
			_decoder = decoder;
			_encoder = encoder;
		}
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			_addrRemote = (InetSocketAddress) ctx.channel().remoteAddress();
			_session = new DFTcpChannelWrap(_addrRemote.getHostString(), _addrRemote.getPort(), 
					ctx.channel(), _decodeType, _encoder);
			_session.setOpenTime(System.currentTimeMillis());
			_sessionId = _session.getChannelId();
			//
			int actorId = 0;
			if(_dispatcher != null){
				actorId = _dispatcher.onConnActiveUnsafe(_requestId, _sessionId, _addrRemote);
			}else{  //没有notify指定
				actorId = _actorIdDef;
			}
			if(actorId != 0){ //actor有效
				_session.setStatusActor(actorId);
				_session.setMessageActor(actorId);
				//notify actor
				actorMgr.send(0, actorId, _requestId, 
						DFActorDefine.SUBJECT_NET, DFActorDefine.NET_TCP_CONNECT_OPEN, _session, true);
			}
			super.channelActive(ctx);
		}
		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			_session.onClose();
			int actorId = 0;
			if(_dispatcher != null){
				actorId = _dispatcher.onConnInactiveUnsafe(_requestId, _sessionId, _addrRemote);
			}else{
				actorId = _session.getStatusActor();
			}
			if(actorId != 0){
				//notify actor
				actorMgr.send(0, actorId, _requestId, 
						DFActorDefine.SUBJECT_NET, DFActorDefine.NET_TCP_CONNECT_CLOSE, _session, true);
			}
			_session = null;
			super.channelInactive(ctx);
		}
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msgRaw) throws Exception {
			try{
				if(msgRaw instanceof PingWebSocketFrame){
					ctx.writeAndFlush(new PongWebSocketFrame(msgRaw.content().retain()));
					return ;
				}else{
					boolean msgIsBin = false;
					Object msgTmp = null;
					int msgType = 0;
					if(msgRaw instanceof BinaryWebSocketFrame){ 	//ByteBuf
						msgTmp = msgRaw.content().retain();
						msgType = DFActorDefine.NET_TCP_MESSAGE;
						msgIsBin = true;
					}else if(msgRaw instanceof TextWebSocketFrame){ //String
						msgTmp = ((TextWebSocketFrame) msgRaw).text();
						msgType = DFActorDefine.NET_TCP_MESSAGE_TEXT;
					}else{  //unknown frame
						return ;
					}
					//
					Object msg = null;
					if(_decoder != null){  //有自定义解码器
						msg = _decoder.onDecode(msgTmp);
						if(msg != null && msg != msgTmp){ //已经过解码
							msgType = DFActorDefine.NET_TCP_MESSAGE_CUSTOM;
							msgIsBin = false;
						}
					}else{
						msg = msgTmp;
					}
					int actorId = 0;
					if(_dispatcher == null){
						actorId = _session.getMsgActor();
					}else{
						actorId = _dispatcher.onQueryMsgActorId(_requestId, _sessionId, _addrRemote, msg);
					}
					if(actorId != 0){ //actor有效
						if(actorMgr.send(_requestId, actorId, _sessionId, 
								DFActorDefine.SUBJECT_NET, 
								DFActorDefine.NET_TCP_MESSAGE, //msgType, 
								msg, true, _session, null, false) != 0){ //send to queue failed
							if(msgIsBin){ //release
								ReferenceCountUtil.release(msg);
							}
						}
					}
				} 
			}finally{
				
			}
		}
		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
	            ctx.pipeline().remove(DFWSRequestHandler.class);  //remove http handle
	        } else {
	            super.userEventTriggered(ctx, evt);
	        }
		}
	}
	private class TcpHandlerInit extends ChannelInitializer<SocketChannel>{
		private final int _decodeType;
		private final int _maxLen;
		private final int _actorId;
		private final int _requestId;
		private final String _wsSfx;
		private final Object _dispatcher;
		private final DFTcpDecoder _decoder;
		private final DFTcpEncoder _encoder;
		private final Object _userHandler;
		private final boolean _isServer;
		private final SslConfig _sslCfg;
		private final Object _reqData;
		private TcpHandlerInit(boolean isServer, int decodeType, int maxLen, int actorId, int requestId, String wsSfx, 
				Object dispatcher, DFTcpDecoder decoder, DFTcpEncoder encoder,
				Object userHandler, SslConfig sslCfg, Object reqData) {
			_isServer = isServer;
			_decodeType = decodeType;
			_maxLen = maxLen;
			_actorId = actorId;
			_requestId = requestId;
			_wsSfx = wsSfx;
			_dispatcher = dispatcher;
			_decoder = decoder;
			_encoder = encoder;
			_userHandler = userHandler;
			_sslCfg = sslCfg;
			_reqData = reqData;
		}
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			final ChannelPipeline pipe = ch.pipeline();
			if(_sslCfg != null){ //ssl
				final SslContext sslCtx = SslContextBuilder.forServer(new File(_sslCfg.getCertPath()), 
						new File(_sslCfg.getPemPath())).build();
				SslHandler sslHandler = sslCtx.newHandler(ch.alloc());
				pipe.addLast(sslHandler);
			}
			//
			if(_decodeType == DFActorDefine.TCP_DECODE_WEBSOCKET){
				pipe.addLast(new HttpServerCodec());
				pipe.addLast(new HttpObjectAggregator(64*1024));
				pipe.addLast(new DFWSRequestHandler("/"+_wsSfx));
				pipe.addLast(new WebSocketServerProtocolHandler("/"+_wsSfx, null, true));
				pipe.addLast(new TcpWsHandler(_actorId, _requestId, _decodeType, (DFActorTcpDispatcher) _dispatcher, _decoder, _encoder));
			}
			else if(_decodeType == DFActorDefine.TCP_DECODE_HTTP){
				if(_isServer){
					pipe.addLast(new HttpServerCodec());
					pipe.addLast(new HttpObjectAggregator(64*1024));
//					pipe.addLast(new HttpServerExpectContinueHandler());
					pipe.addLast(new DFHttpSvrHandler(_actorId, _requestId, _decoder, (DFHttpDispatcher) _dispatcher, (CbHttpServer) _userHandler));
				}else{ //client
					pipe.addLast(new HttpClientCodec());
					pipe.addLast(new HttpObjectAggregator(64*1024));
					pipe.addLast(new DFHttpCliHandler(_actorId, _requestId, _decoder, (DFHttpDispatcher) _dispatcher, 
										(CbHttpClient) _userHandler, (DFHttpCliReqWrap) _reqData));
				}
			}
			else{
				if(_decodeType == DFActorDefine.TCP_DECODE_LENGTH){ //length base field
					pipe.addLast(new LengthFieldBasedFrameDecoder(_maxLen, 0, 2, 0, 2));
				}
				pipe.addLast(new TcpHandler(_actorId, _requestId, _decodeType, (DFActorTcpDispatcher) _dispatcher, _decoder, _encoder));
			}
			
		}
	}
}
