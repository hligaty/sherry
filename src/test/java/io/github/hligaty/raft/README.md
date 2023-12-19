# 简介

实现了 Leader 选举和日志复制的 Raft Demo，包括预投票和读索引读（只是 Demo，有很多偷懒实现，所以和真正用于生产的是不完全一样的）

代码在 `io.github.hligaty.raft` 下，具体包作用：

|  包名   |              作用               |
| :-----: | :-----------------------------: |
| config  |       集群节点地址等配置        |
|  core   | raft 算法实现（投票、日志复制） |
|   rpc   |          集群节点通信           |
| storage |      raft 日志、元数据存储      |
|  util   |             工具类              |

# 测试

流程: 启动 5 个服务端节点, 启动 1 个客户端, 在客户端输入指令, 过程中随意停止和启动服务端节点, 判断指令执行结果是否与预期相同

## 选择实现

基于 Raft 实现了两个功能，一个是 KV 数据库 [KVStateMachine](stateMachine/KVStateMachine.java)，另一个是计数器 [CounterStateMachine](stateMachine/CounterStateMachine.java)，在类  [RaftLocalClusterTest](https://github.com/hligaty/sherry/blob/bd06fe8428efd9d9ef0d4b4ff0876583db201e2e/src/test/java/io/github/hligaty/raft/RaftLocalClusterTest.java#L43) 中修改 `rocksDBStateMachine` 变量来更改(更改后记得把 Raft 节点的所有数据删掉)。

## 启动服务端

1. 配置 5 个运行程序, 配置如下 VM 参数，-Dport 分别设置为 4869、4870、4871、4872 和 4873：

```
-ea
-Dport=4869
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.math=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=java.base/java.nio=ALL-UNNAMED
--add-opens=java.base/java.security=ALL-UNNAMED
--add-opens=java.base/java.text=ALL-UNNAMED
--add-opens=java.base/java.time=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/jdk.internal.access=ALL-UNNAMED
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
```

2. 运行配置的 5 个 `io.github.hligaty.raft.RaftLocalClusterTest.RaftServer#main` 程序

## 启动客户端

运行 `io.github.hligaty.raft.RaftLocalClusterTest.RaftCli#main` 启动客户端，客户端通过键盘接受命令

## 客户端使用

### KV 数据库

对于 KV 数据库的客户端命令：

- 保存：set ${key} ${value}
- 获取：get ${key}
- 删除：delete ${key}

样例：

```
Sofa-Middleware-Log SLF4J Warn : No log util is usable, Default app logger will be used.
set foo bar
2023/12/19 15:11:14, 827 [cc86a8f2-ada4-4aaf-a78a-15bd4b46c635] [main] [INFO]  Succeed to execute remote invoke. port[4869], leaderPort[4869], time[267]
Please enter the command(command execution time283):
get foo
2023/12/19 15:11:18, 650 [fddcca50-5d04-4c24-85a5-04915b372024] [main] [INFO]  Succeed to execute remote invoke. port[4871], leaderPort[4869], time[16]
bar
Please enter the command(command execution time17):
delete foo
2023/12/19 15:11:30, 793 [8cdc47ac-9c92-4d0c-8d53-396810ab53e7] [main] [INFO]  Succeed to execute remote invoke. port[4869], leaderPort[4869], time[132]
Please enter the command(command execution time132):
get foo
2023/12/19 15:11:33, 997 [48aaf0cb-4996-42fd-b8df-e6ee9f8a5747] [main] [INFO]  Succeed to execute remote invoke. port[4869], leaderPort[4869], time[6]
null
Please enter the command(command execution time6):
get foo
2023/12/19 15:11:46, 678 [92fcb1cc-d1ed-44c8-8c58-233b8cd62191] [main] [INFO]  Succeed to execute remote invoke. port[4872], leaderPort[4869], time[27]
null
Please enter the command(command execution time31):
```

### 计数器

对于计数器的客户端命令：

- 加一：increment
- 获取：get

样例：

```
Sofa-Middleware-Log SLF4J Warn : No log util is usable, Default app logger will be used.
increment
2023/12/19 14:05:03, 702 [6dfc5fc4-ae04-42fd-8fbc-b0b476e653c6] [main] [INFO]  Succeed to execute remote invoke. port[4870], leaderPort[4870], time[114]
1
Please enter the command(command execution time275):
get
2023/12/19 14:05:07, 335 [ca8ae651-23a7-4c8f-bbcd-5e75a4e774ba] [main] [INFO]  Succeed to execute remote invoke. port[4871], leaderPort[4870], time[15]
1
Please enter the command(command execution time21):
get
2023/12/19 14:05:24, 542 [8e26e222-8982-4850-9b02-d99484f87dcf] [main] [INFO]  Succeed to execute remote invoke. port[4870], leaderPort[4870], time[6]
1
Please enter the command(command execution time6):
increment
2023/12/19 14:05:35, 711 [3dc656f6-6b77-4562-a7bc-f5500736939a] [main] [INFO]  Succeed to execute remote invoke. port[4870], leaderPort[4870], time[83]
2
Please enter the command(command execution time84):
get
2023/12/19 14:05:37, 685 [b538a86b-bc08-43b7-b3b1-97ff4ca841b5] [main] [INFO]  Succeed to execute remote invoke. port[4871], leaderPort[4870], time[5]
2
Please enter the command(command execution time8):
```

### 其他

客户端出现 REPLICATION_FAIL 不代表执行失败，之后还是有可能执行成功的(集群重新选举后还可能复制成功)，可以再查一次，比如下面：

```
Sofa-Middleware-Log SLF4J Warn : No log util is usable, Default app logger will be used.
set hello world
2023/11/15 17:30:08, 068 [ERROR]  Failed to execute remote invoke, reason: replication fail, port[4869], data[Set{key='hello', value='world'}]
com.alipay.remoting.rpc.exception.InvokeServerException: Server exception! Please check the server log, the address is 127.0.0.1:4869, id=1, ServerErrorMsg:null
	at com.alipay.remoting.rpc.RpcResponseResolver.preProcess(RpcResponseResolver.java:124)
	at com.alipay.remoting.rpc.RpcResponseResolver.resolveResponseObject(RpcResponseResolver.java:54)
	at com.alipay.remoting.rpc.RpcRemoting.invokeSync(RpcRemoting.java:186)
	at com.alipay.remoting.rpc.RpcClientRemoting.invokeSync(RpcClientRemoting.java:72)
	at com.alipay.remoting.rpc.RpcRemoting.invokeSync(RpcRemoting.java:143)
	at com.alipay.remoting.rpc.RpcClient.invokeSync(RpcClient.java:219)
	at io.github.hligaty.raft.RaftLocalClusterTest.sendCommand(RaftLocalClusterTest.java:167)
	at io.github.hligaty.raft.RaftLocalClusterTest.executeKVCommand(RaftLocalClusterTest.java:115)
	at io.github.hligaty.raft.RaftLocalClusterTest$RaftCli.main(RaftLocalClusterTest.java:67)
Caused by: com.alipay.remoting.rpc.exception.RpcServerException: [Server]OriginErrorMsg: io.github.hligaty.raft.core.ServerException: REPLICATION_FAIL. AdditionalErrorMsg: SYNC process rpc request failed in RpcRequestProcessor, id=1
	at io.github.hligaty.raft.core.DefaultNode.apply(DefaultNode.java:204)
	at io.github.hligaty.raft.rpc.SofaBoltService.handleRequest(SofaBoltService.java:64)
	at io.github.hligaty.raft.rpc.SofaBoltService$SingleThreadExecutorSyncUserProcessor.handleRequest(SofaBoltService.java:104)
	at com.alipay.remoting.rpc.protocol.RpcRequestProcessor.dispatchToUserProcessor(RpcRequestProcessor.java:252)
	at com.alipay.remoting.rpc.protocol.RpcRequestProcessor.doProcess(RpcRequestProcessor.java:146)
	at com.alipay.remoting.rpc.protocol.RpcRequestProcessor$ProcessTask.run(RpcRequestProcessor.java:393)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	at java.base/java.lang.Thread.run(Thread.java:1583)
Please enter the clientRequest:
get hello
Succeed to execute remote invoke. port[4871], leaderPort[4871], time[13]
world
Please enter the clientRequest:
```

