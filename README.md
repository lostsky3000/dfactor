[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)

# dfactor

dfactor 是一个基于actor模型的消息处理框架

dfactor 使用java编写，天生多平台支持，开发调试方便

dfactor 充分利用多核处理器，平衡业务负载

dfactor 提供易用的api，上手简单，编写少量代码即可快速构建从通信层到存储层的服务端程序

dfactor 示例丰富，持续增加各种类型的服务器开发示例，简单业务做些修改就可使用

dfactor 参考了 [skynet](https://github.com/cloudwu/skynet) 的设计(谢谢作者云风)，
依托java丰富的第三方资源，提供了更多服务端常用功能


## dfactor 能做什么?

dfactor 本质是一个actor模型的消息处理框架，加上服务器开发(特别是游戏)常用的网络通信，定时任务等
功能，原则上说可以用于任何想充分利用多核性能的业务模型，
常见的比如聊天室, mmorpg, moba, rts, slg, 卡牌棋牌等类型游戏


## dfactor 有哪些特性?

- 充分利用多核处理器性能优势
- TCP, HTTP, HTTPS, UDP, WebSocket多协议支持，快速搭建通信层
- 通信层使用netty，高性能网络io的保证
- io和业务分离模型，使业务逻辑计算不受io瓶颈制约，达到最大效能
- 内置服务器开发常用模块，如计时器，定时任务等
- 开发接口简单易用，示例丰富，少量代码快速搭建模型
- 内置redis, mysql客户端驱动，配合专门io线程使用，实现异步操作数据库及缓存



## 快速开始

启动一个简单的http-echo服务器
```java
net.doHttpServer(8080, new CbHttpServer() {
	@Override
	public int onHttpRequest(Object msg) {
		DFHttpSvrReq req = (DFHttpSvrReq) msg;
		//response
		req.response("echo from server").send();
		return MSG_AUTO_RELEASE;
	}
	...				
});
```
几行代码完成HTTP服务器启动，[本例代码](src/test/java/fun/lib/actor/example/SimpleHttpServer.java)



## 示例

[Startup](src/test/java/fun/lib/actor/example/Startup.java)  快速启动一个dfactor示例

[Timeout](src/test/java/fun/lib/actor/example/Timeout.java)  计时器使用示例

[Schedule](src/test/java/fun/lib/actor/example/Schedule.java)  定时任务使用示例

[ExitActor](src/test/java/fun/lib/actor/example/ExitActor.java)  退出actor示例

[Shutdown](src/test/java/fun/lib/actor/example/Shutdown.java)  关闭dfactor示例

[Sendback](src/test/java/fun/lib/actor/example/Sendback.java)  actor通信api sendback使用示例

[Callback](src/test/java/fun/lib/actor/example/Callback.java)  actor通信 异步回调使用示例

[TomAndJerry](src/test/java/fun/lib/actor/example/TomAndJerry.java)  多actor通信示例(猫捉老鼠游戏)

[BlockActor](src/test/java/fun/lib/actor/example/BlockActor.java)  block类型actor使用示例(适用数据库操作等io场景)

[TcpTest](src/test/java/fun/lib/actor/example/TcpTest.java)  TCP服务端客户端通信示例

[TcpCustomDecAndEnc](src/test/java/fun/lib/actor/example/TcpCustomDecAndEnc.java)  TCP自定义消息编解码器示例

[WebsocketServer](src/test/java/fun/lib/actor/example/WebsocketServer.java)  WebSocket服务端示例

[SimpleHttpServer](src/test/java/fun/lib/actor/example/SimpleHttpServer.java)  简单的HTTP服务示例

[HttpServerDispatcher](src/test/java/fun/lib/actor/example/HttpServerDispatcher.java)  充分利用多cpu处理HTTP业务的示例

[HttpsServer](src/test/java/fun/lib/actor/example/HttpsServer.java) HTTPS服务器示例

[HttpClient](src/test/java/fun/lib/actor/example/HttpClient.java) HTTP请求外部服务器示例

[RedisTest](src/test/java/fun/lib/actor/example/RedisTest.java) 使用io线程异步操作Redis示例


## FAQ


## 后续计划

- 增加 mysql,redis,mongodb 异步操作




## 支持

QQ群：726932841



## dfactor 开发哲学

- 尽量用dfactor实现所有需求
- 用，不要学

