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
- 开发接口简单易用，少量代码快速搭建模型
