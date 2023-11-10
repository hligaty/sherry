# 简介

实现了 Leader 选举和日志复制，代码在 `io.github.hligaty.raft` 下，具体包作用：

|  包名   |              作用               |
| :-----: | :-----------------------------: |
| config  |       集群节点地址等配置        |
|  core   | raft 算法实现（投票、日志复制） |
|   rpc   |          集群节点通信           |
| storage |      raft 日志、元数据存储      |
|  util   |             工具类              |

# 测试

## 选择实现

基于 Raft 实现了两个功能，一个是 KV 数据库 [KVStateMachine](KVStateMachine.java)，另一个是计数器 [CounterStateMachine](CounterStateMachine.java)，在类  [RaftLocalClusterTest](RaftLocalClusterTest.java) 中修改 `rocksDBStateMachine` 变量来更改(更改后记得把 Raft 节点的所有数据删掉)。

## 启动服务端

1. 配置 3 个运行程序, 配置如下 VM 参数，-Dport 分别设置为 4869、4870 和 4871：

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

2. 运行配置的 3 个 `io.github.hligaty.raft.RaftLocalClusterTest.RaftServer#main` 程序

## 启动客户端

1. 运行 `io.github.hligaty.raft.RaftLocalClusterTest.RaftCli#main` 启动客户端，客户端通过键盘接受命令
2. 集群选举出 Leader 后就可以使用 `raft set ${port}` 命令来设置客户端以后向该领导者发送命令（集群 Leader 变更后也要重新设置）

## 客户端使用

### KV 数据库

对于 KV 数据库的客户端命令：

- 保存：set ${key} ${value}
- 获取：get ${key}
- 删除：delete ${key}

样例：

```
Sofa-Middleware-Log SLF4J Warn : No log util is usable, Default app logger will be used.
raft set 4870
Please enter the command:
set foo bar
Please enter the command:
get foo
bar
Please enter the command:
delete foo
Please enter the command:
```

### 计数器

对于计数器的客户端命令：

- 加一：increment
- 获取：get

样例：

```
Sofa-Middleware-Log SLF4J Warn : No log util is usable, Default app logger will be used.
raft set 4870
Please enter the command:
increment
1
Please enter the command:
increment
2
Please enter the command:
get
24
Please enter the command:
```

