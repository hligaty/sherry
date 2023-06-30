# Sherry

Guava 等开源代码的简单实现（原理不一定相同）

## 功能

- [Interner](src/main/java/io/github/hligaty/reference/Interner.java)：像 String.intern() 一样获取字符串，保证 equals 和 hashCode 相等的对象使用 Interner.intern(obj1) == Interner.intern(obj2)
- [InfiniteStriped](src/main/java/io/github/hligaty/reference/InfiniteStriped.java)：基于 Interner 的锁，保证两个 equals 和 hashCode 相同的对象获取同一把锁

更推荐 Guava Striped 使用 hashCode 减少锁对象

- [CircuitBreaker.java](src/main/java/io/github/hligaty/circuitBreaker/CircuitBreaker.java) 简单的限流器

## 路线图

- 按照访问时间过期的缓存
- EventLoop

## License

The Sherry is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
