USE ai_interview_coach;

DELETE FROM knowledge_card WHERE source_name = '小林 coding';

INSERT INTO knowledge_card
(category, title, question, answer, follow_up, key_points, difficulty, tags, source_name, source_url, enabled, sort_order, created_at, updated_at)
VALUES
('JAVA', 'HashMap 底层结构', 'HashMap 在 JDK 1.8 中的底层结构是什么？', 'HashMap 底层主要由数组、链表和红黑树组成。数组用于定位桶，链表用于处理哈希冲突。当同一个桶中的链表长度达到阈值，并且数组容量满足条件时，链表会转为红黑树以提升极端冲突场景下的查询效率。', '为什么链表长度超过阈值后不是一定立即转红黑树？', '数组定位桶
链表处理哈希冲突
红黑树优化极端冲突
扩容会重新分布元素', 'MEDIUM', '基础,集合,HashMap', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 1, NOW(), NOW()),
('JAVA', 'HashMap 扩容机制', 'HashMap 什么时候扩容，扩容时会发生什么？', 'HashMap 的元素数量超过容量乘以负载因子时会触发扩容。扩容通常把数组容量变为原来的两倍，并根据新的容量重新计算元素位置。JDK 1.8 中节点可能留在原索引，也可能移动到原索引加旧容量的位置。', '为什么负载因子默认是 0.75？', '容量乘以负载因子触发扩容
扩容后数组长度翻倍
元素位置可能重新分布
默认负载因子折中空间和冲突', 'MEDIUM', '集合,HashMap,扩容', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 2, NOW(), NOW()),
('JAVA', 'ConcurrentHashMap 原理', 'ConcurrentHashMap 如何保证并发安全？', 'ConcurrentHashMap 通过更细粒度的并发控制减少锁竞争。JDK 1.8 中主要使用 CAS、synchronized 和 volatile 协作完成插入、扩容和可见性控制，不再使用 JDK 1.7 的 Segment 分段锁结构。', 'ConcurrentHashMap 的 size 为什么不是简单读取一个变量？', 'CAS 减少无锁更新开销
synchronized 锁住桶级别节点
volatile 保证可见性
扩容可由多个线程协助', 'HARD', '集合,并发,ConcurrentHashMap', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 3, NOW(), NOW()),
('JAVA', 'ArrayList 扩容', 'ArrayList 添加元素时容量不够会怎么处理？', 'ArrayList 底层是数组。添加元素时如果容量不足，会创建一个更大的数组并复制旧元素。常见扩容策略是变为原容量的 1.5 倍左右，因此频繁扩容会带来数组复制成本。', '为什么预估容量时可以使用构造方法指定 initialCapacity？', '底层结构是数组
容量不足会创建新数组
扩容需要复制旧元素
预估容量可减少复制成本', 'EASY', '基础,集合,ArrayList', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 4, NOW(), NOW()),
('JAVA', 'LinkedList 与 ArrayList', 'ArrayList 和 LinkedList 有什么区别？', 'ArrayList 基于数组，支持高效随机访问，尾部追加通常较快，但中间插入删除需要移动元素。LinkedList 基于双向链表，随机访问需要遍历，插入删除在已定位节点后更方便，但节点对象有额外内存开销。', '为什么实际开发中 ArrayList 使用更常见？', 'ArrayList 随机访问快
LinkedList 随机访问慢
链表节点有额外内存
插入删除是否快取决于定位成本', 'EASY', '基础,集合,List', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 5, NOW(), NOW()),
('JAVA', 'Java equals 和 hashCode', '为什么重写 equals 通常也要重写 hashCode？', 'equals 表示对象逻辑相等，hashCode 用于哈希容器定位桶。两个对象如果 equals 为 true，hashCode 必须相同，否则放入 HashMap、HashSet 等容器时可能出现查找失败或重复存储问题。', 'hashCode 相同的两个对象 equals 一定为 true 吗？', 'equals true 要求 hashCode 相同
hashCode 相同不代表对象相等
哈希容器依赖 hashCode 定位
违反约定会导致查找异常', 'EASY', '基础,对象,HashMap', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 6, NOW(), NOW()),
('JAVA', 'Java 线程池参数', 'Java 线程池的核心参数有哪些？', '线程池常见核心参数包括核心线程数、最大线程数、空闲线程存活时间、任务队列、线程工厂和拒绝策略。它们共同决定任务提交后是创建线程、进入队列，还是触发拒绝策略。', '任务提交到线程池后的执行流程是什么？', '核心线程数
最大线程数
任务队列
拒绝策略
线程工厂', 'MEDIUM', '并发,线程池,Executor', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 7, NOW(), NOW()),
('JAVA', '线程池拒绝策略', '线程池常见拒绝策略有哪些？', '当线程数达到最大值且队列已满时会触发拒绝策略。常见策略包括抛异常、调用者线程执行、丢弃当前任务、丢弃队列中最旧任务。业务中通常需要根据任务重要性选择合适策略。', '调用者运行策略有什么风险？', 'AbortPolicy 抛异常
CallerRunsPolicy 调用者执行
DiscardPolicy 丢弃当前任务
DiscardOldestPolicy 丢弃最旧任务', 'MEDIUM', '并发,线程池,拒绝策略', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 8, NOW(), NOW()),
('JAVA', 'volatile 关键字', 'volatile 能解决什么问题？', 'volatile 主要保证变量的可见性和一定的有序性。一个线程修改 volatile 变量后，其他线程能及时看到新值。但 volatile 不能保证复合操作的原子性，例如 i++ 仍然不是线程安全的。', '为什么 volatile 不能保证 i++ 的线程安全？', '保证可见性
禁止部分指令重排
不保证复合操作原子性
常用于状态标记', 'MEDIUM', '并发,volatile,JMM', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 9, NOW(), NOW()),
('JAVA', 'synchronized 锁升级', 'synchronized 锁升级大致过程是什么？', 'synchronized 的实现会根据竞争情况进行优化，常见状态包括无锁、偏向锁、轻量级锁和重量级锁。锁升级的目标是在低竞争场景减少阻塞开销，在竞争激烈时再使用更重的互斥机制。', '为什么锁只能升级，通常不能降级？', '偏向锁优化单线程重复进入
轻量级锁使用 CAS 尝试竞争
重量级锁依赖阻塞唤醒
锁升级服务于性能折中', 'HARD', '并发,synchronized,锁升级', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 10, NOW(), NOW()),
('JAVA', 'ThreadLocal 原理', 'ThreadLocal 的作用和风险是什么？', 'ThreadLocal 为每个线程保存一份独立变量副本，常用于保存用户上下文、事务上下文等线程内数据。它的风险是在线程池场景中如果不及时 remove，可能造成数据串用或内存泄漏。', '为什么线程池中使用 ThreadLocal 后必须清理？', '每个线程独立副本
数据存在线程的 ThreadLocalMap
线程池会复用线程
使用后及时 remove', 'MEDIUM', '并发,ThreadLocal,内存泄漏', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 11, NOW(), NOW()),
('JAVA', 'Java 异常体系', 'Java checked exception 和 unchecked exception 有什么区别？', 'checked exception 在编译期要求显式处理或继续抛出，适合调用方可以恢复的异常。unchecked exception 通常继承 RuntimeException，编译器不强制处理，多表示编程错误或运行时不可预期问题。', '业务异常应该设计成 checked 还是 unchecked？', 'checked 编译期强制处理
unchecked 不强制捕获
RuntimeException 常用于业务异常
异常设计要避免污染调用链', 'EASY', '基础,异常,RuntimeException', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 12, NOW(), NOW()),
('JVM', 'JVM 内存区域', 'JVM 运行时内存区域有哪些？', 'JVM 运行时数据区通常包括堆、方法区、虚拟机栈、本地方法栈和程序计数器。堆主要存放对象实例，是垃圾回收重点区域；栈保存方法调用帧；程序计数器记录当前线程执行位置。', '哪些区域是线程私有的？', '堆是对象主要存放区域
栈保存方法调用帧
程序计数器线程私有
方法区保存类元数据', 'EASY', '内存区域,JVM,GC', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 101, NOW(), NOW()),
('JVM', '对象创建过程', 'Java 对象创建过程大致是什么？', '对象创建通常包括类加载检查、分配内存、初始化零值、设置对象头、执行构造方法等步骤。分配内存时可能采用指针碰撞或空闲列表，具体取决于堆是否规整。', '对象头里通常包含哪些信息？', '类加载检查
分配对象内存
初始化零值
设置对象头
执行构造方法', 'MEDIUM', '对象创建,JVM,对象头', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 102, NOW(), NOW()),
('JVM', '垃圾回收可达性分析', 'JVM 如何判断对象是否可以被回收？', '现代 JVM 主要使用可达性分析。从 GC Roots 出发沿引用链查找，能被到达的对象认为存活，不能到达的对象可能被回收。常见 GC Roots 包括栈中引用、静态变量引用和 JNI 引用。', '哪些对象可以作为 GC Roots？', '从 GC Roots 出发
可达对象存活
不可达对象可回收
栈引用和静态引用常见', 'MEDIUM', 'GC,可达性分析,JVM', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 103, NOW(), NOW()),
('JVM', '类加载机制', 'Java 类加载过程包括哪些阶段？', '类加载过程通常包括加载、验证、准备、解析和初始化。加载负责获取字节码并生成 Class 对象，验证保证字节码安全，准备给静态变量分配默认值，初始化执行类构造器。', '双亲委派模型解决了什么问题？', '加载字节码
验证安全性
准备静态变量默认值
解析符号引用
初始化执行类构造器', 'MEDIUM', '类加载,JVM,双亲委派', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 104, NOW(), NOW()),
('JVM', 'Full GC 常见原因', 'Full GC 常见触发原因有哪些？', 'Full GC 可能由老年代空间不足、元空间不足、显式调用 System.gc、晋升失败或大对象分配失败等原因触发。排查时需要结合 GC 日志、堆使用情况和对象分配速率分析。', '如何初步排查频繁 Full GC？', '老年代空间不足
元空间不足
大对象分配失败
晋升失败
结合 GC 日志分析', 'HARD', 'GC,Full GC,JVM调优', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 105, NOW(), NOW()),
('SPRING', 'IOC 是什么', 'Spring IOC 的核心思想是什么？', 'IOC 是控制反转，核心是对象创建和依赖管理从业务代码转移到 Spring 容器。开发者通过配置或注解声明依赖，容器负责实例化 Bean、注入依赖并管理生命周期。', 'IOC 和 DI 有什么关系？', '对象创建交给容器
依赖由容器注入
降低对象之间耦合
Bean 生命周期由容器管理', 'EASY', 'IOC,Bean,依赖注入', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 201, NOW(), NOW()),
('SPRING', 'Bean 生命周期', 'Spring Bean 生命周期大致包括哪些阶段？', 'Bean 生命周期包括实例化、属性填充、Aware 回调、BeanPostProcessor 前置处理、初始化方法、后置处理、使用和销毁。理解生命周期有助于解释扩展点和框架自动装配。', 'BeanPostProcessor 常用于什么场景？', '实例化
属性填充
初始化回调
BeanPostProcessor
销毁回调', 'MEDIUM', 'Bean,生命周期,Spring', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 202, NOW(), NOW()),
('SPRING', 'AOP 原理', 'Spring AOP 的基本原理是什么？', 'Spring AOP 通过代理对象在方法调用前后织入增强逻辑。常见代理方式包括 JDK 动态代理和 CGLIB。它适合处理日志、事务、权限等横切关注点。', 'JDK 动态代理和 CGLIB 有什么区别？', '代理对象织入增强
JDK 动态代理基于接口
CGLIB 基于子类
适合横切逻辑', 'MEDIUM', 'AOP,代理,Spring', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 203, NOW(), NOW()),
('SPRING', 'Spring 事务传播', 'Spring 事务传播行为解决什么问题？', '事务传播行为定义一个事务方法调用另一个事务方法时如何处理事务边界。常见 REQUIRED 表示有事务就加入，没有就新建；REQUIRES_NEW 表示挂起当前事务并新建事务。', 'REQUIRED 和 REQUIRES_NEW 的区别是什么？', '定义嵌套调用事务边界
REQUIRED 加入或新建
REQUIRES_NEW 挂起并新建
传播行为影响提交和回滚范围', 'MEDIUM', '事务,传播行为,Spring', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 204, NOW(), NOW()),
('SPRING', 'Spring 事务失效', 'Spring 声明式事务常见失效场景有哪些？', '声明式事务依赖代理生效。常见失效场景包括同类内部方法调用、方法不是 public、异常被捕获未抛出、抛出的异常类型不触发回滚、对象未交给 Spring 容器管理。', '为什么同类内部方法调用会导致事务失效？', '事务依赖代理
内部调用绕过代理
异常被吞掉不会回滚
非 Spring Bean 不受事务管理', 'HARD', '事务,代理,Spring', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 205, NOW(), NOW()),
('SPRING', 'Spring Boot 自动配置', 'Spring Boot 自动配置的基本思路是什么？', 'Spring Boot 自动配置通过条件注解和约定配置减少手动配置。框架根据 classpath、配置项和已有 Bean 判断是否创建默认 Bean，从而快速启动常见技术栈。', 'ConditionalOnMissingBean 有什么作用？', '约定优于配置
条件注解决定是否生效
根据 classpath 装配
允许用户自定义 Bean 覆盖默认配置', 'MEDIUM', 'Spring Boot,自动配置,条件注解', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 206, NOW(), NOW()),
('MYSQL', 'MySQL 索引为什么能加速查询', 'MySQL 索引为什么可以提升查询速度？', '索引通过额外的数据结构减少扫描数据量。InnoDB 常用 B+ 树索引，查询时可以从根节点逐层定位到叶子节点，而不是全表扫描。索引适合高选择性的查询条件，但会带来额外存储和写入维护成本。', '为什么 InnoDB 常用 B+ 树而不是普通二叉树？', '减少扫描范围
B+ 树层高低
叶子节点有序
写入需要维护索引', 'EASY', '索引,B+树,InnoDB', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 301, NOW(), NOW()),
('MYSQL', '聚簇索引和二级索引', '聚簇索引和二级索引有什么区别？', 'InnoDB 的聚簇索引叶子节点保存整行数据，通常就是主键索引。二级索引叶子节点保存索引列和主键值，查询非索引列时可能需要根据主键回表查询整行数据。', '什么是回表查询？', '聚簇索引叶子保存整行
二级索引叶子保存主键
回表根据主键取整行
覆盖索引可减少回表', 'MEDIUM', '索引,聚簇索引,回表', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 302, NOW(), NOW()),
('MYSQL', '事务 ACID', '数据库事务 ACID 分别是什么意思？', 'ACID 指原子性、一致性、隔离性和持久性。原子性保证事务内操作要么都成功要么都失败；隔离性控制并发事务互相影响；持久性保证提交后数据不会因宕机丢失。', 'MySQL 通过什么机制保证持久性？', '原子性
一致性
隔离性
持久性
redo log 支持持久性', 'EASY', '事务,ACID,MySQL', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 303, NOW(), NOW()),
('MYSQL', 'MVCC 原理', 'MVCC 解决了什么问题？', 'MVCC 通过多版本数据让读写操作在一定程度上并发执行，减少锁冲突。InnoDB 通过隐藏字段、undo log 和 Read View 判断某个事务能看到哪个版本的数据。', 'Read View 中主要包含哪些信息？', '多版本并发控制
减少读写冲突
undo log 保存历史版本
Read View 判断可见性', 'HARD', '事务,MVCC,隔离级别', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 304, NOW(), NOW()),
('MYSQL', '间隙锁', '什么是 MySQL 间隙锁？', '间隙锁锁住索引记录之间的范围，主要用于在可重复读隔离级别下防止幻读。它不锁定具体某一行，而是限制其他事务在某个范围内插入新记录。', '为什么间隙锁可能导致并发性能下降？', '锁住索引区间
防止幻读
依赖索引范围
可能阻塞插入', 'HARD', '锁,间隙锁,幻读', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 305, NOW(), NOW()),
('MYSQL', 'Explain 执行计划', 'Explain 中哪些字段常用于判断 SQL 是否走索引？', 'Explain 常看 type、possible_keys、key、rows、Extra 等字段。key 表示实际使用的索引，type 反映访问类型，rows 估算扫描行数，Extra 中 Using index 表示可能使用覆盖索引。', 'type 中 const、ref、range、ALL 大致代表什么？', 'key 看实际索引
type 看访问类型
rows 看扫描估算
Extra 看额外信息', 'MEDIUM', 'Explain,SQL优化,索引', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 306, NOW(), NOW()),
('REDIS', 'Redis 缓存穿透', '什么是缓存穿透，常见解决方案有哪些？', '缓存穿透指查询的数据在缓存和数据库中都不存在，导致请求每次都绕过缓存打到数据库。常见解决方案包括缓存空值、布隆过滤器、参数校验和限流。', '缓存空值会带来什么问题？', '请求命中不存在数据
数据库压力增大
缓存空值
布隆过滤器', 'EASY', '缓存,穿透,布隆过滤器', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 401, NOW(), NOW()),
('REDIS', '缓存击穿和雪崩', '缓存击穿和缓存雪崩有什么区别？', '缓存击穿通常指某个热点 key 失效后大量请求同时打到数据库。缓存雪崩指大量 key 同时失效或 Redis 不可用，导致请求大面积打到数据库。常见处理包括互斥锁、逻辑过期、过期时间随机化和限流降级。', '热点 key 失效为什么适合用互斥锁或逻辑过期？', '击穿针对热点 key
雪崩是大面积失效
过期时间随机化
互斥锁保护数据库', 'MEDIUM', '缓存,击穿,雪崩', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 402, NOW(), NOW()),
('REDIS', 'Redis 持久化', 'Redis RDB 和 AOF 有什么区别？', 'RDB 是某个时间点的数据快照，恢复速度快但可能丢失最近写入。AOF 记录写命令日志，数据安全性通常更好，但文件可能更大，恢复时需要重放命令。实际使用中可以结合两者。', 'AOF 重写解决了什么问题？', 'RDB 是快照
AOF 是命令日志
RDB 恢复快
AOF 数据更完整
AOF 重写压缩日志', 'MEDIUM', '持久化,RDB,AOF', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 403, NOW(), NOW()),
('REDIS', 'Redis 分布式锁', 'Redis 分布式锁需要注意哪些问题？', 'Redis 分布式锁通常使用 set nx ex 实现原子加锁，并设置过期时间避免死锁。释放锁时要校验锁的唯一值，避免误删其他客户端的锁。业务耗时超过过期时间时还要考虑续期。', '为什么释放锁要用 Lua 脚本？', 'set nx ex 原子加锁
必须设置过期时间
释放前校验唯一值
Lua 保证校验和删除原子性', 'HARD', '分布式锁,Redis,Lua', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 404, NOW(), NOW()),
('REDIS', 'Redis 数据结构', 'Redis 常用数据结构有哪些？', 'Redis 常用数据结构包括 String、Hash、List、Set、ZSet、Bitmap、HyperLogLog、Stream 等。不同结构适合不同场景，例如 String 做缓存计数，Hash 存对象字段，ZSet 做排行榜。', '排行榜为什么常用 ZSet？', 'String 缓存和计数
Hash 存对象字段
List 队列
Set 去重
ZSet 排序集合', 'EASY', '数据结构,Redis,ZSet', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 405, NOW(), NOW()),
('REDIS', 'Redis 过期删除策略', 'Redis key 过期后是如何删除的？', 'Redis 结合惰性删除和定期删除处理过期 key。惰性删除是在访问 key 时检查是否过期，定期删除是周期性抽样删除部分过期 key。这样能在内存回收和 CPU 开销之间做平衡。', '如果过期 key 很多但迟迟不访问会怎样？', '惰性删除访问时检查
定期删除周期抽样
平衡 CPU 和内存
内存不足时还会触发淘汰策略', 'MEDIUM', '过期删除,淘汰策略,Redis', '小林 coding', 'https://xiaolincoding.com/interview/', 1, 406, NOW(), NOW());
