# Sherry

一些代码片段

## 功能

- [Interner](src/main/java/io/github/hligaty/reference/Interner.java)：像 String.intern() 一样获取字符串，保证 equals 和 hashCode 相等的对象使用 Interner.intern(obj1) == Interner.intern(obj2)
- [InfiniteStriped](src/main/java/io/github/hligaty/reference/InfiniteStriped.java)：基于 Interner 的锁，保证两个 equals 和 hashCode 相同的对象获取同一把锁。更推荐 Guava Striped 使用 hashCode 减少锁对象

- [CircuitBreaker](src/main/java/io/github/hligaty/circuitBreaker/CircuitBreaker.java) 简单的限流器
- [EnumPropertyPreFilter](src/main/java/io/github/hligaty/reflection/EnumPropertyPreFilter.java) 序列化时为被标注为枚举的字段生成枚举名字字段（FastJson2 实现）
- [EnumPropertyProcessor](src/main/java/io/github/hligaty/reflection/EnumPropertyProcessor.java) 序列化时为被标注为枚举的字段生成枚举名字字段（Annotation Processor 和 Javassit 实现），以及为 Swagger Schema 注解生成 description 和 JakartaValidation NotNull 生成 message
- [EnumPropertyAgent](src/main/java/io/github/hligaty/reflection/EnumPropertyAgent.java) 效果同 EnumPropertyProcessor，JavaAgent 方式

-  [Raft](src\test\java\io\github\hligaty\raft\README.md) Raft 算法实现
