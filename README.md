[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)

# dfactor

dfactor 是一个基于actor模型的消息处理框架

dfactor 使用java编写

dfactor 充分利用多核处理器，平衡业务负载

dfactor 提供易用的api，上手简单，示例丰富，编写少量代码即可快速构建从通信层到存储层的服务端程序


## dfactor 能做什么?

dfactor 本质是一个actor模型的消息处理框架，加上服务器开发(特别是游戏)常用的网络通信，定时任务等
功能，原则上说可以用于任何想充分利用多核性能的业务模型


## dfactor 有哪些特性?
- 充分利用多核处理器性能优势
- 内置服务器开发常用模块，如计时器，定时任务等
- 通信层使用netty，高性能网络io的保证
- io和业务分离模型，使业务逻辑计算不受io瓶颈制约，达到最大效能
- 开发接口简单易用，示例丰富，少量代码快速搭建模型


## 快速开始
```java
DFActorManager mgr = DFActorManager.get();
//启动配置参数
DFActorManagerConfig cfg = new DFActorManagerConfig()
				.setLogicWorkerThreadNum(2);  //设置逻辑线程数量
//启动入口actor，开始消息循环		
mgr.start(cfg, "EntryActor", EntryActor.class);
...
@Override
public void onStart(Object param) {
  //使用自带日志打印
  log.info("EntryActor start, curThread="+Thread.currentThread().getName());
}
...
```

几行代码，完成一个最简单dfactor的启动



## 示例

[Startup](src/test/java/fun/lib/actor/example/Startup.java)  快速启动一个dfactor示例

[Timeout](src/test/java/fun/lib/actor/example/Timeout.java)  计时器使用示例

[Schedule](src/test/java/fun/lib/actor/example/Schedule.java)  定时任务使用示例

[ExitActor](src/test/java/fun/lib/actor/example/ExitActor.java)  退出actor示例

[Shutdown](src/test/java/fun/lib/actor/example/Shutdown.java)  关闭dfactor示例

[TomAndJerry](src/test/java/fun/lib/actor/example/TomAndJerry.java)  多actor通信示例(猫捉老鼠游戏)

[BlockTest](src/test/java/fun/lib/actor/example/BlockTest.java)  block类型actor使用示例(适用数据库操作等io场景)

[TcpTest](src/test/java/fun/lib/actor/example/TcpTest.java)  tcp服务端客户端通信示例

[WebsocketServer](src/test/java/fun/lib/actor/example/WebsocketServer.java)  websocket服务端示例


## FAQ



## 问题

交流QQ群：726932841

