const fs = require("node:fs");
const path = require("node:path");

const repoRoot = path.resolve(__dirname, "..");
const outputPath = path.join(repoRoot, "data", "knowledge_cards.sql");

const sources = {
  JAVA: ["小林 coding, JavaGuide", "https://javaguide.cn/home.html"],
  JVM: ["小林 coding, JavaGuide", "https://javaguide.cn/java/jvm/"],
  MYSQL: ["小林 coding, JavaGuide", "https://xiaolincoding.com/mysql/"],
  REDIS: ["小林 coding, JavaGuide", "https://xiaolincoding.com/redis/"],
  SPRING: ["JavaGuide", "https://javaguide.cn/system-design/framework/spring/"],
  AI: ["项目原创整理", ""],
};

const leaves = [
  {
    category: "JAVA",
    baseOrder: 10,
    tag: "面向对象",
    difficulty: "EASY",
    titles: ["封装、继承、多态的面试表达", "接口和抽象类如何取舍", "重载与重写的区别", "Java 对象创建过程", "组合优于继承的原因"],
  },
  {
    category: "JAVA",
    baseOrder: 20,
    tag: "数据类型",
    difficulty: "EASY",
    titles: ["基本类型和包装类型区别", "自动装箱与拆箱风险", "String 为什么不可变", "StringBuilder 和 StringBuffer 区别", "BigDecimal 为什么适合金额计算"],
  },
  {
    category: "JAVA",
    baseOrder: 30,
    tag: "异常处理",
    difficulty: "MEDIUM",
    titles: ["Checked Exception 和 RuntimeException 区别", "try-catch-finally 执行顺序", "业务异常如何设计", "异常被吞掉的排查思路", "全局异常处理的分层边界"],
  },
  {
    category: "JAVA",
    baseOrder: 40,
    tag: "反射与泛型",
    difficulty: "MEDIUM",
    titles: ["反射的使用场景和代价", "泛型擦除是什么", "Class 对象和类加载关系", "注解如何配合反射工作", "泛型通配符 extends 和 super 区别"],
  },
  {
    category: "JAVA",
    baseOrder: 50,
    tag: "List",
    difficulty: "EASY",
    titles: ["ArrayList 和 LinkedList 的区别", "ArrayList 扩容机制", "ArrayList 删除元素的坑", "CopyOnWriteArrayList 适用场景", "List 遍历时修改为什么会失败"],
  },
  {
    category: "JAVA",
    baseOrder: 60,
    tag: "Map",
    difficulty: "MEDIUM",
    titles: ["HashMap 在 JDK 1.8 中的底层结构", "HashMap put 流程", "HashMap 扩容为什么是 2 的幂", "ConcurrentHashMap 如何保证线程安全", "LinkedHashMap 如何实现 LRU"],
  },
  {
    category: "JAVA",
    baseOrder: 70,
    tag: "Set",
    difficulty: "EASY",
    titles: ["HashSet 如何保证元素唯一", "TreeSet 的排序和去重规则", "LinkedHashSet 如何保持插入顺序", "Set 中可变对象为什么危险", "HashSet 和 ConcurrentHashMap.newKeySet 的区别"],
  },
  {
    category: "JAVA",
    baseOrder: 80,
    tag: "JUC",
    difficulty: "MEDIUM",
    titles: ["synchronized 和 ReentrantLock 的区别", "ThreadLocal 使用场景和风险", "volatile 能保证什么", "线程池核心参数如何设置", "CompletableFuture 适合什么场景"],
  },
  {
    category: "JVM",
    baseOrder: 100,
    tag: "JVM",
    difficulty: "MEDIUM",
    titles: ["JVM 运行时内存区域", "CMS 和 G1 垃圾收集器区别", "类加载过程和双亲委派", "对象从创建到回收的过程", "线上 Full GC 如何排查"],
  },
  {
    category: "MYSQL",
    baseOrder: 200,
    tag: "索引",
    difficulty: "HARD",
    titles: ["MySQL 索引失效场景及优化", "InnoDB 为什么使用 B+ 树索引", "联合索引最左前缀原则", "覆盖索引和回表", "Explain 执行计划怎么看"],
  },
  {
    category: "MYSQL",
    baseOrder: 210,
    tag: "事务",
    difficulty: "HARD",
    titles: ["事务 ACID 如何理解", "隔离级别解决哪些问题", "可重复读为什么还可能有当前读", "事务传播到业务代码的边界", "长事务会带来什么风险"],
  },
  {
    category: "MYSQL",
    baseOrder: 220,
    tag: "锁",
    difficulty: "HARD",
    titles: ["行锁、表锁和间隙锁区别", "Next-Key Lock 解决什么问题", "死锁如何定位和避免", "乐观锁和悲观锁怎么选", "select for update 使用边界"],
  },
  {
    category: "MYSQL",
    baseOrder: 230,
    tag: "MVCC",
    difficulty: "HARD",
    titles: ["MVCC 是什么", "Read View 如何判断版本可见", "undo log 和版本链关系", "快照读和当前读区别", "MVCC 为什么不能完全替代锁"],
  },
  {
    category: "REDIS",
    baseOrder: 300,
    tag: "数据结构",
    difficulty: "MEDIUM",
    titles: ["Redis 为什么快", "String 的典型使用场景", "Hash 结构适合存什么", "List、Set、ZSet 如何选择", "大 key 会带来什么问题"],
  },
  {
    category: "REDIS",
    baseOrder: 310,
    tag: "缓存问题",
    difficulty: "MEDIUM",
    titles: ["Redis 缓存击穿、穿透与雪崩", "缓存与数据库一致性", "热点 key 如何治理", "布隆过滤器解决什么问题", "缓存预热和降级怎么设计"],
  },
  {
    category: "REDIS",
    baseOrder: 320,
    tag: "持久化",
    difficulty: "MEDIUM",
    titles: ["Redis RDB 和 AOF 持久化", "AOF 重写解决什么问题", "RDB 快照触发方式", "混合持久化的价值", "Redis 宕机恢复如何取舍"],
  },
  {
    category: "REDIS",
    baseOrder: 330,
    tag: "分布式锁",
    difficulty: "HARD",
    titles: ["Redis 分布式锁基本实现", "SET NX EX 为什么要原子", "锁续期和看门狗机制", "Redisson 分布式锁原理", "Redis 锁误删如何避免"],
  },
  {
    category: "SPRING",
    baseOrder: 400,
    tag: "IOC",
    difficulty: "MEDIUM",
    titles: ["Spring Bean 生命周期", "IOC 和依赖注入的关系", "BeanFactory 和 ApplicationContext 区别", "循环依赖三级缓存", "BeanPostProcessor 扩展点"],
  },
  {
    category: "SPRING",
    baseOrder: 410,
    tag: "AOP",
    difficulty: "MEDIUM",
    titles: ["Spring AOP 实现原理", "JDK 动态代理和 CGLIB 区别", "同类内部调用为什么绕过 AOP", "切点和通知如何组织", "AOP 适合哪些横切逻辑"],
  },
  {
    category: "SPRING",
    baseOrder: 420,
    tag: "事务",
    difficulty: "HARD",
    titles: ["Spring 事务失效场景", "事务传播行为怎么理解", "rollbackFor 什么时候需要配置", "声明式事务和编程式事务区别", "事务边界如何设计"],
  },
  {
    category: "SPRING",
    baseOrder: 430,
    tag: "Spring MVC",
    difficulty: "MEDIUM",
    titles: ["Spring MVC 请求处理流程", "DispatcherServlet 的职责", "HandlerMapping 和 HandlerAdapter 区别", "参数解析和返回值处理", "拦截器和过滤器区别"],
  },
  {
    category: "AI",
    baseOrder: 500,
    tag: "Agent",
    difficulty: "MEDIUM",
    titles: ["Agent 工作流为什么要拆成 Planner、Tool 和 Observation", "Tool Calling 的工程边界", "Agent Step Trace 如何帮助排查", "Agent 失败降级怎么设计", "Memory 在面试训练中的作用"],
  },
  {
    category: "AI",
    baseOrder: 510,
    tag: "RAG",
    difficulty: "MEDIUM",
    titles: ["RAG 在当前项目中为什么作为 Agent 内部 Tool", "RAG 检索结果为什么只是证据", "用户记忆检索为什么必须隔离", "MySQL 结构化 RAG 的取舍", "RAG 失败为什么不能阻塞诊断"],
  },
  {
    category: "AI",
    baseOrder: 520,
    tag: "LangChain",
    difficulty: "HARD",
    titles: ["LangChain 和本项目自定义 Agent 编排有什么区别", "Chain、Tool、Memory 分别解决什么问题", "为什么 MVP 阶段不强依赖 LangChain", "LangChain 接入 Spring Boot 的边界", "LCEL 表达式适合什么场景"],
  },
];

const leafNotes = {
  "JAVA:面向对象": {
    conclusion: "面向对象题要先说明封装、继承和多态如何降低业务变化带来的修改成本，而不是只背三个名词。",
    mechanism: "封装把状态和行为收在对象边界内，继承表达稳定的 is-a 关系，多态让调用方依赖父类型或接口完成扩展。",
    risk: "常见误区是滥用继承、把实体类写成贫血数据容器，或者只说语法差异却讲不出为什么组合通常更稳。",
  },
  "JAVA:数据类型": {
    conclusion: "数据类型题要围绕值语义、引用语义、自动装箱、不可变对象和精度风险展开。",
    mechanism: "基本类型直接保存值，包装类型是对象；String 不可变带来线程安全和常量池复用；BigDecimal 通过十进制表示规避浮点误差。",
    risk: "常见问题包括包装类型空指针、Integer 缓存误判、字符串拼接产生多余对象，以及金额计算误用 double。",
  },
  "JAVA:异常处理": {
    conclusion: "异常处理的目标是把失败路径显式化，让调用方知道能否恢复、是否回滚、该返回什么错误信息。",
    mechanism: "Checked Exception 强迫调用方处理可恢复异常，RuntimeException 多表示编程错误或业务不可继续；finally 常用于资源释放。",
    risk: "最危险的是 catch 后吞异常、把所有异常包成同一种错误、或者在事务方法里捕获异常导致本应回滚的数据提交。",
  },
  "JAVA:反射与泛型": {
    conclusion: "反射和泛型要放到框架扩展场景里讲：一个解决运行期元数据访问，一个解决编译期类型约束。",
    mechanism: "反射通过 Class、Field、Method 等对象访问类结构；泛型经过类型擦除后主要保留编译期检查，运行期要借助签名或显式类型信息。",
    risk: "反射会牺牲可读性和性能，泛型擦除会导致运行期类型信息不足，框架代码要格外注意边界校验。",
  },
  "JAVA:List": {
    conclusion: "List 题要围绕顺序存储、随机访问、扩容、遍历修改和并发读写取舍来回答。",
    mechanism: "ArrayList 基于连续数组，读下标快但中间插入删除要移动元素；LinkedList 基于节点，定位慢且对象开销更高。",
    risk: "常见误区是以为 LinkedList 插入删除一定更快，或者在 for-each 中直接删除元素触发 ConcurrentModificationException。",
  },
  "JAVA:Map": {
    conclusion: "Map 题的核心是 hash 定位、冲突处理、扩容迁移和并发安全边界。",
    mechanism: "HashMap 用数组桶承载节点，JDK 1.8 中冲突链过长会树化；ConcurrentHashMap 用 CAS、synchronized 和分段扩容降低并发冲突。",
    risk: "常见风险包括 key 的 equals/hashCode 不一致、容量设置不合理造成频繁扩容、以及把 HashMap 当并发容器使用。",
  },
  "JAVA:Set": {
    conclusion: "Set 题要说明唯一性判断来自底层结构，而不是 Set 自己拥有神秘去重能力。",
    mechanism: "HashSet 借 HashMap 的 key 去重，TreeSet 借比较器维护排序和唯一性，LinkedHashSet 额外维护插入顺序。",
    risk: "可变对象放入 Set 后再修改参与 hash 或比较的字段，会导致查找失败、重复数据或顺序异常。",
  },
  "JAVA:JUC": {
    conclusion: "JUC 题要先区分可见性、原子性、有序性、线程调度和任务编排五类问题。",
    mechanism: "volatile 解决可见性和禁止部分重排序，锁解决互斥，线程池管理线程生命周期，CompletableFuture 组织异步依赖。",
    risk: "常见风险是 ThreadLocal 在线程池里不清理、线程池参数套模板、锁释放不在 finally，或者把 volatile 当成万能原子性保障。",
  },
  "JVM:JVM": {
    conclusion: "JVM 题要把内存区域、类加载、对象生命周期和 GC 停顿串起来讲。",
    mechanism: "堆承载对象实例，栈保存调用帧，方法区保存类元数据；类加载经历加载、验证、准备、解析、初始化；GC 根据可达性分析回收对象。",
    risk: "排查时不能只背收集器名称，要结合 GC 日志、对象分配速率、老年代占用、线程栈和应用吞吐定位问题。",
  },
  "MYSQL:索引": {
    conclusion: "索引题要说明它如何减少扫描行数和回表成本，而不是简单说“索引能加速查询”。",
    mechanism: "InnoDB B+ 树让查询沿树高定位到叶子节点，联合索引按最左前缀匹配，覆盖索引可以直接从索引拿到结果。",
    risk: "常见失效点包括函数包裹索引列、隐式转换、LIKE 前置百分号、联合索引跳列和低选择度字段乱建索引。",
  },
  "MYSQL:事务": {
    conclusion: "MySQL 事务题要围绕 ACID、隔离级别、快照读/当前读和长事务影响展开。",
    mechanism: "redo log 保证持久性，undo log 支撑回滚和 MVCC，锁和 Read View 一起维护隔离性。",
    risk: "长事务会拖住 undo 清理和版本链，隔离级别理解不清会导致幻读、不可重复读或当前读行为解释错误。",
  },
  "MYSQL:锁": {
    conclusion: "MySQL 锁题要先区分锁粒度和锁目的：行锁保护记录，间隙锁保护范围，意向锁协调表级判断。",
    mechanism: "InnoDB 在索引记录上加锁，范围查询可能产生 next-key lock；死锁检测会选择代价较小事务回滚。",
    risk: "没有命中索引可能扩大锁范围，事务顺序不一致容易死锁，select for update 用错会把读接口变成并发瓶颈。",
  },
  "MYSQL:MVCC": {
    conclusion: "MVCC 题要说明它用多版本减少读写互斥，但并不能替代所有锁。",
    mechanism: "每行隐藏事务字段配合 undo log 形成版本链，Read View 根据活跃事务集合判断哪个版本对当前事务可见。",
    risk: "快照读和当前读混淆是高频错误；写写冲突、范围约束和唯一性检查仍然需要锁参与。",
  },
  "REDIS:数据结构": {
    conclusion: "Redis 数据结构题要从使用场景、编码实现和性能边界三个角度讲。",
    mechanism: "String 可做计数和缓存，Hash 适合对象字段，List 适合队列，Set 适合去重集合，ZSet 适合排行榜和延时场景。",
    risk: "大 key、热 key、慢命令和不合理的数据结构选择，会让单线程命令执行被拖慢。",
  },
  "REDIS:缓存问题": {
    conclusion: "缓存问题题要把穿透、击穿、雪崩和一致性分别定义清楚，再给出对应治理方案。",
    mechanism: "穿透用布隆过滤器或缓存空值，击穿用互斥锁或逻辑过期，雪崩用随机 TTL、限流降级和高可用部署。",
    risk: "缓存更新顺序、热点 key 过期和故障降级没有设计好，会把压力瞬间打回数据库。",
  },
  "REDIS:持久化": {
    conclusion: "Redis 持久化题要说明 RDB 偏恢复效率，AOF 偏数据安全，生产常组合使用。",
    mechanism: "RDB 保存时间点快照，AOF 追加写命令并通过重写压缩文件；混合持久化用快照加增量日志兼顾恢复速度和丢失窗口。",
    risk: "只谈开启持久化不够，还要说明磁盘 IO、fork 开销、fsync 策略和故障恢复演练。",
  },
  "REDIS:分布式锁": {
    conclusion: "分布式锁题要围绕互斥、过期、续期、唯一标识和释放原子性来回答。",
    mechanism: "SET NX EX 保证加锁和设置过期原子化，释放时用 Lua 校验锁值，Redisson 通过看门狗续期降低业务超时误释放。",
    risk: "错误删除别人的锁、锁过期业务没执行完、Redis 主从切换丢锁，是面试官最常追问的风险。",
  },
  "SPRING:IOC": {
    conclusion: "IOC 题要说明 Spring 不只是 new 对象，而是统一负责依赖注入、生命周期和扩展点调用。",
    mechanism: "容器读取 BeanDefinition，实例化 Bean，填充属性，执行 Aware、BeanPostProcessor、初始化和销毁回调。",
    risk: "循环依赖、过早暴露代理、原型 Bean 销毁、BeanPostProcessor 顺序，都是生命周期题的追问重点。",
  },
  "SPRING:AOP": {
    conclusion: "AOP 题要说明代理对象如何把横切逻辑织入业务调用链。",
    mechanism: "JDK 动态代理基于接口，CGLIB 基于子类；外部调用先进入代理，再执行通知链和目标方法。",
    risk: "同类内部调用绕过代理、final 方法无法被 CGLIB 正常增强、切点范围过宽都会导致行为不符合预期。",
  },
  "SPRING:事务": {
    conclusion: "Spring 事务题要把数据库事务和 Spring AOP 代理边界同时讲清楚。",
    mechanism: "声明式事务通过代理拦截方法，在进入目标方法前开启事务，方法异常后按 rollback 规则提交或回滚。",
    risk: "内部调用、非 public 方法、异常被吞、checked exception 未配置 rollbackFor、非 Spring Bean 都可能让事务失效。",
  },
  "SPRING:Spring MVC": {
    conclusion: "Spring MVC 题要围绕 DispatcherServlet 如何把 HTTP 请求分发到 Controller 并写回响应展开。",
    mechanism: "请求先进入 DispatcherServlet，通过 HandlerMapping 找处理器，再由 HandlerAdapter 调用，参数解析和返回值处理完成对象转换。",
    risk: "过滤器和拦截器边界不清、参数绑定失败、全局异常处理缺失，会让接口行为难以排查。",
  },
  "AI:Agent": {
    conclusion: "Agent 题要强调可解释工作流：计划、工具调用、观察、诊断、记忆和训练计划是分步完成的。",
    mechanism: "Planner 决定步骤，Tool 调用代码执行/RAG/AI 判断等能力，Observation 把工具输出转成后续诊断事实，Trace 记录每一步。",
    risk: "如果把所有逻辑塞进单次 prompt，就很难定位失败步骤，也无法说明代码执行结果为什么优先于模型猜测。",
  },
  "AI:RAG": {
    conclusion: "RAG 题要说明检索只是给模型提供证据，不能替代代码执行事实和业务规则。",
    mechanism: "当前项目用 MySQL 结构化检索 problem、knowledge_card、ai_diagnosis、mistake_card，用户记忆按 user_id 隔离。",
    risk: "证据过期、召回不准、用户数据串用、RAG 失败阻塞主流程，都会破坏诊断可信度。",
  },
  "AI:LangChain": {
    conclusion: "LangChain 题要讲清楚框架能力和项目自定义编排的取舍，而不是简单说用或不用。",
    mechanism: "LangChain 提供 Chain、Tool、Memory、Agent 等抽象；本项目用 Spring Service 和状态机直接表达业务链路。",
    risk: "MVP 阶段过早引入复杂框架会增加调试成本；但保留清晰 Tool 边界后，未来接入 LangChain 仍可复用服务能力。",
  },
};

function sql(value) {
  if (value === null || value === undefined || value === "") return "NULL";
  return `'${String(value).replace(/'/g, "''")}'`;
}

function categoryLabel(category) {
  return {
    JAVA: "Java",
    JVM: "JVM",
    MYSQL: "MySQL",
    REDIS: "Redis",
    SPRING: "Spring",
    AI: "AI 工程",
  }[category];
}

function focusForTitle(title) {
  const rules = [
    [/封装、继承、多态/, "封装强调对象边界，继承强调复用和层次，多态强调面向接口调用时的运行期分派。"],
    [/接口和抽象类/, "接口更像能力契约，抽象类适合沉淀共享状态和模板步骤，取舍时要看是否存在稳定父类关系。"],
    [/重载与重写/, "重载发生在同一类的编译期方法选择，重写发生在继承体系的运行期动态绑定。"],
    [/对象创建过程/, "对象创建要经过类加载检查、内存分配、零值初始化、对象头设置和构造方法执行。"],
    [/组合优于继承/, "组合把变化委托给成员对象，能降低继承层级过深带来的脆弱基类问题。"],
    [/基本类型和包装类型/, "基本类型没有 null 且存值更直接，包装类型参与泛型、集合和反射但要注意空指针。"],
    [/自动装箱/, "装箱会生成包装对象或走缓存，拆箱会调用 xxxValue，包装对象为 null 时会直接触发空指针。"],
    [/String 为什么不可变/, "String 不可变让常量池复用、Hash 缓存和线程共享更安全，但大量拼接要用可变构建器。"],
    [/StringBuilder/, "StringBuilder 非线程安全但快，StringBuffer 方法加锁更安全但开销更高。"],
    [/BigDecimal/, "BigDecimal 要用字符串构造并指定舍入规则，避免 double 二进制浮点误差传入。"],
    [/Checked Exception/, "Checked Exception 表示调用方可感知可处理的问题，RuntimeException 更多表示业务不可继续或编程错误。"],
    [/try-catch-finally/, "finally 通常会执行，但 return 覆盖和资源关闭异常会让结果变复杂，生产代码更推荐 try-with-resources。"],
    [/业务异常/, "业务异常应携带稳定错误码和可展示信息，不要把底层异常栈直接暴露给前端。"],
    [/异常被吞掉/, "吞异常会破坏日志、事务回滚和调用方决策，至少要记录上下文并按业务语义继续抛出或转换。"],
    [/全局异常处理/, "全局异常处理负责把异常翻译成统一响应，业务判断仍应留在 service 层。"],
    [/反射的使用场景/, "反射常见于框架装配、注解扫描、序列化和测试工具，代价是性能、类型安全和可读性下降。"],
    [/泛型擦除/, "泛型擦除意味着运行期主要看到原始类型，编译器通过桥接方法和强转维持类型语义。"],
    [/Class 对象/, "Class 对象是 JVM 对类元数据的运行期入口，和类加载器共同决定类型身份。"],
    [/注解如何配合反射/, "注解提供元数据，反射读取元数据并把它转成运行期行为，例如依赖注入或路由映射。"],
    [/通配符/, "extends 适合安全读取上界类型，super 适合安全写入下界类型，可以用 PECS 记忆。"],
    [/ArrayList 和 LinkedList/, "ArrayList 优势在连续内存和随机访问，LinkedList 的节点开销和遍历成本常被低估。"],
    [/ArrayList 扩容/, "ArrayList 扩容会创建更大数组并复制旧元素，频繁扩容会带来时间和内存抖动。"],
    [/ArrayList 删除/, "按下标或 for-each 删除会引起元素移动或 fail-fast，迭代器删除更可控。"],
    [/CopyOnWriteArrayList/, "写时复制适合读多写少，写操作会复制数组，不能用于高频写入场景。"],
    [/遍历时修改/, "fail-fast 通过 modCount 检测结构变化，目的是尽早暴露并发或遍历修改错误。"],
    [/HashMap 在 JDK 1.8/, "JDK 1.8 HashMap 是数组、链表和红黑树组合，树化需要链表长度和数组容量同时满足条件。"],
    [/HashMap put/, "put 流程包括 hash 扰动、定位桶、处理冲突、必要时树化或扩容。"],
    [/2 的幂/, "容量为 2 的幂能用位运算替代取模，并让扩容后节点只在原位置或原位置加旧容量间移动。"],
    [/ConcurrentHashMap/, "JDK 1.8 ConcurrentHashMap 主要用 CAS 初始化和插入，桶冲突时用 synchronized 控制局部互斥。"],
    [/LinkedHashMap/, "LinkedHashMap 维护双向链表，开启 accessOrder 后可按访问顺序淘汰最久未使用元素。"],
    [/HashSet 如何保证/, "HashSet 把元素作为 HashMap 的 key，唯一性依赖 hashCode 和 equals 一致。"],
    [/TreeSet/, "TreeSet 依赖 Comparable 或 Comparator，同一个比较结果为 0 的元素会被视为重复。"],
    [/LinkedHashSet/, "LinkedHashSet 在 HashSet 去重基础上维护插入顺序，适合需要稳定遍历顺序的场景。"],
    [/可变对象/, "参与 hash 或比较的字段一旦变化，集合内部位置不会自动重排，可能导致 contains 失败。"],
    [/synchronized/, "synchronized 由 JVM 管理锁释放，ReentrantLock 提供公平锁、可中断和条件队列等高级能力。"],
    [/ThreadLocal/, "ThreadLocal 数据挂在线程上，线程池复用时必须 finally remove，避免串值和内存滞留。"],
    [/volatile/, "volatile 保证可见性和禁止部分重排序，但复合操作仍然需要锁或原子类。"],
    [/线程池/, "线程池参数要结合任务类型、队列长度、拒绝策略和监控指标设置，而不是机械套公式。"],
    [/CompletableFuture/, "CompletableFuture 适合编排异步依赖和聚合结果，但要注意线程池隔离和异常传播。"],
    [/JVM 运行时内存/, "堆和方法区线程共享，栈、本地方法栈和程序计数器线程私有，OOM 类型也不同。"],
    [/CMS 和 G1/, "CMS 关注老年代低停顿但有碎片，G1 用 Region 和停顿预测面向大堆服务端应用。"],
    [/类加载过程/, "类加载包括加载、验证、准备、解析、初始化，双亲委派能避免核心类被随意替换。"],
    [/对象从创建到回收/, "对象生命周期从分配开始，经引用可达性判断、可能的晋升和最终回收结束。"],
    [/Full GC/, "Full GC 排查要看日志、老年代增长、元空间、显式 GC、对象分配速率和引用链。"],
    [/索引失效/, "索引失效题要结合 EXPLAIN 的 type、key、rows 和 Extra 判断，而不是罗列口诀。"],
    [/B\+ 树/, "B+ 树树高低、叶子有序且链表相连，适合磁盘 IO 和范围查询。"],
    [/最左前缀/, "联合索引按定义顺序建立有序结构，跳过前导列会让后续列无法用于快速定位。"],
    [/覆盖索引/, "覆盖索引能直接从二级索引拿到查询列，减少回表访问聚簇索引的成本。"],
    [/Explain/, "Explain 要重点看访问类型、使用索引、扫描行数、过滤比例和额外操作。"],
    [/ACID/, "ACID 分别对应原子性、一致性、隔离性、持久性，每一项背后都有日志或锁机制支撑。"],
    [/隔离级别/, "隔离级别是在并发性能和读一致性之间取舍，不同级别解决脏读、不可重复读和幻读。"],
    [/当前读/, "快照读读版本链，当前读读取最新并加锁，因此在可重复读下也可能看到新提交数据。"],
    [/长事务/, "长事务会持有锁和 Read View，导致 undo 版本清理滞后并影响整体性能。"],
    [/Next-Key/, "Next-Key Lock 是记录锁加间隙锁，用来保护范围并降低幻读风险。"],
    [/死锁/, "死锁排查要看事务持锁顺序、SQL 命中索引和 InnoDB 死锁日志。"],
    [/乐观锁/, "乐观锁适合冲突少的更新，悲观锁适合强一致和冲突概率高的关键资源。"],
    [/select for update/, "select for update 必须在事务中使用，并确保命中合适索引以免扩大锁范围。"],
    [/Read View/, "Read View 记录活跃事务边界，用来判断版本链中哪个版本对当前事务可见。"],
    [/undo log/, "undo log 既支持回滚，也为 MVCC 提供历史版本。"],
    [/快照读和当前读/, "快照读不加锁读历史版本，当前读读取最新记录并参与加锁。"],
    [/Redis 为什么快/, "Redis 快来自内存、单线程命令执行、高效数据结构和 IO 多路复用。"],
    [/String 的典型/, "Redis String 适合缓存、计数器、分布式锁 value 和简单状态存储。"],
    [/Hash 结构/, "Hash 适合对象字段级读写，但大对象仍要警惕 big key 和热字段。"],
    [/ZSet/, "ZSet 通过 score 排序，适合排行榜、延时任务和范围查询。"],
    [/大 key/, "大 key 会拖慢单线程命令、网络传输和持久化，必须拆分或异步清理。"],
    [/击穿、穿透与雪崩/, "穿透查不存在，击穿打热点，雪崩是大面积失效，三者治理手段不同。"],
    [/一致性/, "缓存一致性通常采用先更新数据库再删除缓存，并通过重试或消息补偿降低失败窗口。"],
    [/热点 key/, "热点 key 可用本地缓存、分片、逻辑过期、预热和限流降低单点压力。"],
    [/布隆过滤器/, "布隆过滤器用位图和多 hash 判断可能存在，能过滤不存在请求但存在误判。"],
    [/预热和降级/, "预热减少冷启动打库，降级保证缓存异常时系统仍能给出可控响应。"],
    [/RDB 和 AOF/, "RDB 是快照，AOF 是写命令日志，取舍点是恢复速度和数据丢失窗口。"],
    [/AOF 重写/, "AOF 重写通过当前数据状态生成更短命令序列，降低日志体积。"],
    [/RDB 快照/, "RDB 触发涉及 fork 子进程和写时复制，高写入场景要关注内存峰值。"],
    [/混合持久化/, "混合持久化先加载 RDB 再重放 AOF 增量，兼顾速度和数据安全。"],
    [/宕机恢复/, "恢复策略要看业务能接受的数据丢失窗口、启动时间和主从复制状态。"],
    [/分布式锁基本/, "Redis 锁的最小正确性包括唯一锁值、过期时间、原子加锁和校验释放。"],
    [/SET NX EX/, "SET NX EX 把互斥和过期放在一个原子命令里，避免加锁成功但设置过期失败。"],
    [/续期和看门狗/, "看门狗会在业务未结束时延长锁过期时间，但也要求客户端存活且续期线程正常。"],
    [/Redisson/, "Redisson 封装 Lua、续期和可重入语义，降低手写分布式锁错误率。"],
    [/锁误删/, "释放锁必须校验 value，不能只按 key 删除，否则可能删掉别的线程新获得的锁。"],
    [/Bean 生命周期/, "Bean 生命周期包括实例化、属性填充、Aware、前后置处理器、初始化、使用和销毁。"],
    [/IOC 和依赖注入/, "IOC 是控制权交给容器，依赖注入是容器把依赖传给对象的一种实现方式。"],
    [/BeanFactory/, "BeanFactory 是基础容器，ApplicationContext 在其上提供事件、国际化和自动装配等能力。"],
    [/循环依赖/, "三级缓存用于提前暴露单例和代理引用，但构造器循环依赖无法靠它解决。"],
    [/BeanPostProcessor/, "BeanPostProcessor 是 Spring 扩展核心，AOP、注解处理等都依赖它介入生命周期。"],
    [/Spring AOP 实现/, "Spring AOP 本质是代理模式，外部调用进入代理后再执行通知链和目标方法。"],
    [/JDK 动态代理/, "JDK 动态代理基于接口，CGLIB 基于继承生成子类代理。"],
    [/同类内部调用/, "同类内部调用没有经过代理对象，因此事务或 AOP 增强不会触发。"],
    [/切点和通知/, "切点定义拦截范围，通知定义增强动作，两者组合成可复用的横切逻辑。"],
    [/横切逻辑/, "日志、权限、事务、监控适合 AOP，核心业务分支不应藏进切面。"],
    [/事务失效/, "事务失效通常不是数据库不支持，而是 Spring 代理没有生效或异常规则不匹配。"],
    [/传播行为/, "传播行为定义已有事务和新方法调用之间的关系，比如加入、挂起或新开事务。"],
    [/rollbackFor/, "默认只回滚 RuntimeException，受检异常需要显式 rollbackFor。"],
    [/编程式事务/, "编程式事务适合细粒度控制，声明式事务适合清晰的 service 方法边界。"],
    [/事务边界/, "事务边界应放在业务一致性范围内，避免跨远程调用或长时间持有数据库连接。"],
    [/请求处理流程/, "Spring MVC 请求从 DispatcherServlet 开始，经映射、适配、调用、返回值处理和视图/JSON 写出结束。"],
    [/DispatcherServlet/, "DispatcherServlet 是前端控制器，负责统一调度而不是承载业务逻辑。"],
    [/HandlerMapping/, "HandlerMapping 找到处理器，HandlerAdapter 负责以统一方式调用不同形态的处理器。"],
    [/参数解析/, "参数解析器把 HTTP 请求转成方法参数，返回值处理器把方法结果转成响应。"],
    [/拦截器和过滤器/, "过滤器属于 Servlet 规范，拦截器属于 Spring MVC，更靠近 Controller 调用链。"],
    [/Planner、Tool 和 Observation/, "Planner 管步骤，Tool 调服务，Observation 把工具结果变成诊断事实。"],
    [/Tool Calling/, "Tool Calling 的边界应是稳定服务能力，工具只负责编排调用，不绕过业务层直接操作数据库。"],
    [/Step Trace/, "Agent Step Trace 能记录输入摘要、输出摘要、耗时和错误，方便演示和排查。"],
    [/失败降级/, "Agent 可选步骤失败应记录 warning 并继续核心诊断，避免辅助能力拖垮主流程。"],
    [/Memory/, "Memory 保存用户弱点和错题，帮助后续训练计划延续，而不是让模型无限制记住所有内容。"],
    [/内部 Tool/, "RAG 放在 Agent 内部，是为了服务诊断证据，而不是把产品扩散成通用问答。"],
    [/检索结果为什么只是证据/, "RAG 证据只辅助解释，Piston 执行结果和测试用例仍是判断代码对错的事实来源。"],
    [/用户记忆检索/, "用户记忆必须按 user_id 隔离，任何跨用户错题召回都是严重数据泄漏。"],
    [/MySQL 结构化 RAG/, "MySQL RAG 适合 MVP：可解释、易落库、易演示，但语义召回能力弱于向量库。"],
    [/RAG 失败/, "RAG 失败不应阻塞判题和诊断，只需要记录 Agent Step 并让 AI 少一份辅助上下文。"],
    [/自定义 Agent 编排/, "自定义编排更贴合 Spring Boot 分层，能把每个 Tool 的输入输出和降级边界讲清楚。"],
    [/Chain、Tool、Memory/, "Chain 管流程，Tool 接外部能力，Memory 管上下文延续，三者要对应清晰业务边界。"],
    [/不强依赖 LangChain/, "MVP 优先可控和可解释，自定义状态机比引入大框架更容易调试和面试表达。"],
    [/接入 Spring Boot/, "接入 LangChain 时应复用现有 Service，而不是让框架直接穿透控制器和 mapper。"],
    [/LCEL/, "LCEL 适合声明式组合模型、提示和解析器，但复杂业务状态仍要谨慎拆分。"],
  ];
  return rules.find(([pattern]) => pattern.test(title))?.[1] || "本题要把概念、机制和项目边界讲完整，避免只给出一句术语解释。";
}

function explain(leaf, title, index) {
  const label = categoryLabel(leaf.category);
  const notes = leafNotes[`${leaf.category}:${leaf.tag}`];
  const mechanism = `${title} 的回答可以先落在 ${label} 的具体场景里。${notes.conclusion}`;
  const detail = `本题落点是：${focusForTitle(title)} ${notes.mechanism} 回答时不要停在名词解释，要补充触发条件、运行过程和为什么这种设计能解决问题。`;
  const scenario = `${notes.risk} 在项目表达上，可以联系 AI 面试教练的代码执行、Agent Step、RAG 证据或后端分层，让面试官听到你能把知识点落到工程链路。`;
  const expression = `推荐表达顺序是“结论 -> 机制 -> 场景 -> 风险 -> 项目落点”。第 ${index + 1} 张卡的重点是把 ${title} 讲成一次可追问的诊断思路，而不是孤立背诵。`;
  return [mechanism, detail, scenario, expression].join("\n\n");
}

function keyPoints(leaf, title) {
  return [
    `先给出 ${title} 的一句话结论`,
    `说明 ${leaf.tag} 的核心机制和触发条件`,
    "结合真实业务场景说明适用边界",
    "指出常见误区、失败表现或排查方向",
    "用项目中的训练闭环或后端分层落到工程表达",
  ].join("\n");
}

function followUps(leaf, title) {
  return [
    `${title} 在真实项目里最容易踩的坑是什么？`,
    `如果线上出现和 ${leaf.tag} 相关的问题，你会先看哪些现象？`,
    `这个知识点和 Spring Boot 后端分层有什么关系？`,
    `如何用一两句话向面试官说明你不是只背概念？`,
  ].join("\n");
}

function question(title) {
  return `请结合后端面试场景解释：${title}？`;
}

function tags(leaf) {
  const common = {
    JAVA: "Java 核心",
    JVM: "JVM",
    MYSQL: "MySQL",
    REDIS: "Redis",
    SPRING: "Spring",
    AI: "AI 工程",
  }[leaf.category];
  const extras = {
    面向对象: "OOP,封装,继承,多态",
    数据类型: "数据类型,包装类型,String",
    异常处理: "异常处理,RuntimeException,全局异常",
    反射与泛型: "反射,泛型,注解",
    List: "集合框架,List,ArrayList,LinkedList",
    Map: "集合框架,Map,HashMap,ConcurrentHashMap",
    Set: "集合框架,Set,HashSet,TreeSet",
    JUC: "并发编程,JUC,锁,线程池,volatile,ThreadLocal",
    JVM: "JVM,内存区域,GC,类加载",
    索引: "MySQL索引,B+树,最左前缀,EXPLAIN",
    事务: leaf.category === "MYSQL" ? "MySQL事务,ACID,隔离级别" : "Spring事务,AOP,传播行为",
    锁: "MySQL锁,行锁,间隙锁,死锁",
    MVCC: "MVCC,Read View,undo log,版本链",
    数据结构: "Redis数据结构,String,Hash,List,Set,ZSet",
    缓存问题: "Redis缓存,缓存穿透,缓存击穿,缓存雪崩,布隆过滤器",
    持久化: "Redis持久化,RDB,AOF",
    分布式锁: "Redis分布式锁,SETNX,Redisson,锁续期",
    IOC: "Spring,IOC,Bean,依赖注入",
    AOP: "Spring,AOP,动态代理,切面",
    "Spring MVC": "Spring MVC,DispatcherServlet,HandlerMapping,HandlerAdapter",
    Agent: "AI 工程,Agent,Planner,Tool Calling,Observation,Memory",
    RAG: "AI 工程,RAG,检索,证据,用户记忆,MySQL 检索",
    LangChain: "AI 工程,LangChain,Chain,Tool,Memory,LCEL",
  }[leaf.tag];
  return `${common},${extras}`;
}

const cards = leaves.flatMap((leaf) =>
  leaf.titles.map((title, index) => {
    const [sourceName, sourceUrl] = sources[leaf.category];
    return {
      category: leaf.category,
      title,
      question: question(title),
      answer: explain(leaf, title, index),
      followUp: followUps(leaf, title),
      keyPoints: keyPoints(leaf, title),
      difficulty: leaf.difficulty,
      tags: tags(leaf),
      sourceName,
      sourceUrl,
      sortOrder: leaf.baseOrder + index,
    };
  })
);

const header = `USE ai_interview_coach;

DELETE FROM knowledge_card
WHERE source_name IN ('小林 coding', 'JavaGuide', '小林 coding, JavaGuide', '项目原创整理');

INSERT INTO knowledge_card
(category, title, question, answer, follow_up, key_points, difficulty, tags, source_name, source_url, enabled, sort_order, created_at, updated_at)
VALUES
`;

const rows = cards.map((card) => `(${[
  sql(card.category),
  sql(card.title),
  sql(card.question),
  sql(card.answer),
  sql(card.followUp),
  sql(card.keyPoints),
  sql(card.difficulty),
  sql(card.tags),
  sql(card.sourceName),
  sql(card.sourceUrl),
  "1",
  String(card.sortOrder),
  "NOW()",
  "NOW()",
].join(", ")})`);

fs.writeFileSync(outputPath, `${header}${rows.join(",\n")};\n`, "utf8");
console.log(`Wrote ${cards.length} knowledge cards to ${path.relative(repoRoot, outputPath)}`);
