[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)

# 最新

- 监听集群内结点事件api  [示例](src/test/java/fun/lib/actor/example/ClusterNodeEvent.java)

- 集群内广播功能 [示例](src/test/java/fun/lib/actor/example/ClusterBroadcast.java)

- 集群间通信(包括rpc)多种序列化格式支持,String,Json,byte[],ByteBuf,自定义序列化对象  [示例](src/test/java/fun/lib/actor/example/ClusterParam.java)

- 增加集群内结点间rpc调用 [示例](src/test/java/fun/lib/actor/example/RpcCluster.java)， 本地actor间rpc调用 [示例](src/test/java/fun/lib/actor/example/RpcLocal.java)

- 集群功能上线，去中心化自动组网，集群内结点互相发现互相通信  [入门示例](src/test/java/fun/lib/actor/example/ClusterSimple.java)

## 开发中

- js脚本开发支持


# dfactor

dfactor 是一个基于actor模型的消息处理框架，充分利用多核处理器，平衡业务负载，可参考 erlang

dfactor 使用java编写，天生多平台支持，开发调试方便

dfactor 提供丰富的组件支持，涵盖通信，编解码，rpc，集群组网等等

dfactor 提供易用的api，上手简单，编写少量代码即可快速构建从通信层到存储层的服务端程序

dfactor 示例丰富，持续增加各种类型的服务器开发示例，简单业务做些修改就可使用


## dfactor 能做什么?

dfactor 本质是一个actor模型的消息处理框架，加上服务器开发常用的网络通信，定时任务，rpc等
功能，原则上说可以用于任何想充分利用多核性能的业务模型，
常见的比如mmorpg, moba, rts, slg, 卡牌棋牌等类型游戏，或者作为服务端中间件开发的基础组件


## dfactor 有哪些特性?

- 充分利用多核处理器性能优势
- TCP, HTTP, HTTPS, UDP, WebSocket多协议支持，快速搭建通信层
- 通信层使用netty，高性能网络io的保证
- 集群功能支持，自动组网，结点自动发现，互相通信
- 支持集群内结点之前rpc通信，本地actor间rpc通信
- io和业务分离模型，使业务逻辑计算不受io瓶颈制约，达到最大效能
- 内置服务器开发常用模块，如计时器，定时任务等
- 开发接口简单易用，示例丰富，少量代码快速搭建模型
- 内置redis, mysql, mongoDb 客户端驱动，配合专门io线程使用，实现异步操作数据库及缓存
- 可 daemon 方式启动，spi模式运行，加载外部 jar 文件实现逻辑快速部署和模块解耦 [示例](src/test/java/fun/lib/actor/example/StartAsDaemon.java)



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


## 性能

[TreeTask](src/test/java/fun/lib/actor/benchmark/TreeTask.java)  百万级actor压力测试

树状actor压力测试，从根节点开始依次创建 6 层深的树，每一层的节点数量为 10

每次任务从根节点开始，向下请求，到达最底层节点时，向上返回结果消息，直到到达根节点

默认树深度为6，每层节点数为10，总共创建 1111111(N) 个actor

一次消息请求，所有子节点都会收到一次请求一次响应(最底层只收到一次请求)，
每个节点收到请求和收到响应都会调用_doTask，模拟业务逻辑的消耗(数组的随机打乱+排序)


使用 jvisualvm 观察程序运行时线程和gc情况，如果出现fullGC，需增加jvm分配内存


测试机配置： Intel i5-7500@3.4GHz

一次任务共200多万次消息调用,无逻辑负载(注释_doTask)情况下，平均耗时600ms左右，
有逻辑负载(每个actor每次收到消息对30个元素的队列随机打乱和排序)情况下，平均耗时1800ms左右

测试结果：

控制台输出 

![控制台输出](https://github.com/lostsky3000/dfactor/raw/master/assets/treetask_console.png)

线程使用情况 

![线程使用情况](https://github.com/lostsky3000/dfactor/raw/master/assets/treetask_thread.png)



可以做下试验：

将_doTask()内实现注释掉，观察框架任务调度本身的消耗，
调整启动参数的逻辑线程数，
观察只有任务调度消耗和包含了逻辑处理消耗时，不同逻辑线程数下的性能


## 示例

[Startup](src/test/java/fun/lib/actor/example/Startup.java)  快速启动一个dfactor示例

[Timeout](src/test/java/fun/lib/actor/example/Timeout.java)  计时器使用示例

[Schedule](src/test/java/fun/lib/actor/example/Schedule.java)  定时任务使用示例

[ExitActor](src/test/java/fun/lib/actor/example/ExitActor.java)  退出actor示例

[Shutdown](src/test/java/fun/lib/actor/example/Shutdown.java)  关闭dfactor示例

[ActorConn](src/test/java/fun/lib/actor/example/ActorConn.java) actor间通信示例

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

[MysqlTest](src/test/java/fun/lib/actor/example/MysqlTest.java) 使用io线程异步操作Mysql示例

[MongodbTest](src/test/java/fun/lib/actor/example/MongodbTest.java) 使用io线程异步操作Mongodb示例

[CallHere](src/test/java/fun/lib/actor/example/CallHere.java) 利用异步回调机制，在一个actor中编写另一个actor业务代码的示例

[CallHereBlock](src/test/java/fun/lib/actor/example/CallHereBlock.java) 利用异步回调机制+框架提供的BlockActor，简化io操作代码，在业务actor中编写io操作代码

[StartAsDaemon](src/test/java/fun/lib/actor/example/StartAsDaemon.java) daemon模式启动示例，加载外部jar执行逻辑的示例，方便部署和模块解耦
[测试外部jar文件下载](spi.jar)


参照[CallHere](src/test/java/fun/lib/actor/example/CallHere.java)和
[CallHereBlock](src/test/java/fun/lib/actor/example/CallHereBlock.java)，可改写 
[RedisTest](src/test/java/fun/lib/actor/example/RedisTest.java), 
[MysqlTest](src/test/java/fun/lib/actor/example/MysqlTest.java), 
[MongodbTest](src/test/java/fun/lib/actor/example/MongodbTest.java) 等例子，实现在业务actor里编写阻塞业务代码，增加代码可读性可维护性

[ClusterSimple](src/test/java/fun/lib/actor/example/ClusterSimple.java)  集群使用入门示例

[RpcLocal](src/test/java/fun/lib/actor/example/RpcLocal.java)  本地actor间rpc调用示例

[RpcCluster](src/test/java/fun/lib/actor/example/RpcCluster.java)  集群内结点间rpc调用示例

[ClusterParam](src/test/java/fun/lib/actor/example/ClusterParam.java)  集群间通信(包括rpc)支持多种消息格式的示例

[ClusterBroadcast](src/test/java/fun/lib/actor/example/ClusterBroadcast.java)  集群内广播示例

[ClusterNodeEvent](src/test/java/fun/lib/actor/example/ClusterNodeEvent.java)  监听集群内结点事件示例


## FAQ



## 后续计划

- 增加常用服务端业务示例，如房间类型游戏等等


## 社区&支持

QQ群：726932841



## dfactor 开发哲学

- 尽量用dfactor实现所有需求
- 用，不要学

