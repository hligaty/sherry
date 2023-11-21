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

流程: 启动 3 个服务端节点, 启动 1 个客户端, 在客户端输入指令, 过程中随意停止和启动服务端节点, 判断指令执行结果是否与预期相同

## 选择实现

基于 Raft 实现了两个功能，一个是 KV 数据库 [KVStateMachine](KVStateMachine.java)，另一个是计数器 [CounterStateMachine](CounterStateMachine.java)，在类  [RaftLocalClusterTest](RaftLocalClusterTest.java) 中修改 `rocksDBStateMachine` 变量来更改(更改后记得把 Raft 节点的所有数据删掉)。

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
set hello world
Succeed to execute remote invoke. port[4871], leaderPort[4871], time[142]
Please enter the clientRequest:
get hello
Succeed to execute remote invoke. port[4869], leaderPort[4869], time[23]
world
Please enter the clientRequest:
delete hello
Succeed to execute remote invoke. port[4871], leaderPort[4871], time[112]
Please enter the clientRequest:
get hello
Succeed to execute remote invoke. port[4869], leaderPort[4869], time[6]
null
Please enter the clientRequest:
```

### 计数器

对于计数器的客户端命令：

- 加一：increment
- 获取：get

样例：

```
Sofa-Middleware-Log SLF4J Warn : No log util is usable, Default app logger will be used.
raft set 4870
Please enter the clientRequest:
Succeed to execute remote invoke. port[4869], leaderPort[4869], time[226]
increment
1
Please enter the clientRequest:
increment
Succeed to execute remote invoke. port[4869], leaderPort[4869], time[87]
2
Please enter the clientRequest:
get
Succeed to execute remote invoke. port[4869], leaderPort[4869], time[2]
2
Please enter the clientRequest:
get
Succeed to execute remote invoke. port[4870], leaderPort[4870], time[4]
2
Please enter the clientRequest:
```

### 其他

客户端出现 REPLICATION_FAIL 不代表执行失败，之后还是有可能执行成功的，可以再查一次，比如下面：

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

