## 1. 用户模块

### 1.1 表设计

#### user

```sql
create table if not exists antares_blog.user
(
    uid          bigint auto_increment comment '用户id'
        primary key,
    username     varchar(64)                                                                                           null comment '用户名',
    password     varchar(64)                                                                                           null comment '密码',
    tags         varchar(512) default '[]'                                                                             null comment '用户标签',
    signature    varchar(256) default '这个人很懒，什么都没写。'                                                         null comment '个性签名',
    email        varchar(64)                                                                                           null comment '邮箱',
    phone        varchar(32)                                                                                           null comment '手机号',
    sex          tinyint(1)   default 1                                                                                not null comment '性别',
    avatar       varchar(256) default 'https://gw.alipayobjects.com/zos/antfincdn/XAosXuNZyF/BiazfanxmamNRoxxVxka.png' null comment '头像',
    follow       int          default 0                                                                                null comment '关注',
    fans         int          default 0                                                                                null comment '粉丝',
    topic        int          default 0                                                                                null comment '博客/动态',
    social_uid   varchar(256)                                                                                          null comment '第三方id',
    access_token varchar(256)                                                                                          null comment '访问第三方信息的token',
    expires_in   int                                                                                                   null comment '访问令牌的有效期',
    create_time  timestamp                                                                                             null comment '创建时间',
    update_time  timestamp                                                                                             null comment '修改时间',
    is_delete    tinyint(1)   default 0                                                                                not null comment '是否删除',
    constraint email
        unique (email),
    constraint social_uid
        unique (social_uid),
    constraint username
        unique (username)
);
```

---

#### user_tag

```sql
create table if not exists antares_blog.user_tag
(
    id          bigint auto_increment comment '主键'
        primary key,
    parent_id   bigint                               not null comment '类别id',
    name        varchar(64)                          null comment '标签名',
    color       varchar(8)                           null comment '颜色',
    created_by  bigint                               not null comment '创建用户',
    create_time datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP not null comment '更新时间',
    is_delete   tinyint(1) default 0                 not null comment '删除标志',
    constraint a_user_tag_pk
        unique (name)
);
```

---

#### user_tag_category

```sql
create table if not exists antares_blog.user_tag_category
(
    id          bigint auto_increment comment '主键'
        primary key,
    name        varchar(64)                          not null comment '标签名',
    create_time datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP null comment '更新时间',
    is_delete   tinyint(1) default 0                 null comment '删除标志',
    constraint name
        unique (name)
);
```

---

#### follow

```sql
create table if not exists antares_blog.follow
(
    id          bigint auto_increment
        primary key,
    uid         bigint                             not null,
    follow_uid  bigint                             not null,
    unread      int      default 0                 null,
    create_time datetime default CURRENT_TIMESTAMP null,
    update_time datetime default CURRENT_TIMESTAMP null
);
```

#### conversation

```sql
create table if not exists antares_blog.conversation
(
    id           bigint auto_increment
        primary key,
    from_uid     bigint                             not null,
    to_uid       bigint                             not null,
    from_unread  int      default 0                 null,
    to_unread    int      default 0                 null,
    last_message varchar(1024)                      null,
    create_time  datetime default CURRENT_TIMESTAMP null,
    update_time  datetime default CURRENT_TIMESTAMP not null,
    is_delete    tinyint  default 0                 not null
);
```

#### chat_message

```sql
create table if not exists antares_blog.chat_message
(
    id              bigint auto_increment
        primary key,
    conversation_id bigint                             null,
    type            int      default 1                 null,
    from_uid        bigint                             not null,
    to_uid          bigint                             not null,
    to_group_id     int                                null,
    content         varchar(1024)                      null,
    create_time     datetime default CURRENT_TIMESTAMP not null,
    is_delete       tinyint  default 0                 not null
);
```

### 1.2 设计思路

#### 1.2.1 登录

首先，使用token+Redis实现分布式session，并通过设置token的domain实现同域名下的单点登录（该博客系统和[OJ系统](https://github.com/qk-antares/antares-oj-backend)共享了用户微服务），具体的流程如下：

用户登录后，后端为该用户生成一个token，之后将token作为key，用户状态作为value存入redis，同时将该token作为cookie返回给客户端，当用户再次请求时，通过拿到请求头中的token，可以去redis中获取用户状态，从而进行后续操作。

之所以不使用Spring Session，是因为在后续的文章模块，需要通过lua脚本去获取redis中用户是否点赞的信息，那么这就涉及到一个问题——如何获取当前请求用户的uid，以判断这个uid是否已经在点赞的set中存在了了？

解决方法是请求头（Cookie）中携带token，lua脚本可以取出请求头中的token，再通过token去获取redis中的用户信息，这样一来可以将判断用户有没有点赞的逻辑直接前置到nginx（事实上最后我将整个文章详情获取的逻辑都写到了lua里，如果redis中没有再请求后端，后端只负责查询数据库，放进redis，不再去redis查）

**问题：**这种方法属于JWT吗？

```
你的项目使用了token和cookie以及Redis等技术，属于基于Token的方案，但不属于JWT。因为JWT是一种规范，它规定了token的格式和签名方法。而你的方案并没有参照JWT规范来生成和验证token。

在JWT方案中，token包含了三部分信息：头部、载荷和签名。载荷部分包含了用户身份信息，如用户名、用户ID等。签名部分用于验证token的真实性和完整性。而token的生成和验证过程也都需要遵循一定的规范和算法。

虽然你的方案没有采用JWT，但也确实可以实现基于token的身份验证和会话管理，而且在某些情况下可能更加灵活和适用。但需要注意的是，由于没有标准的规范和算法，可能存在一些漏洞和安全问题，需要仔细考虑和处理。
```

**第三方登录OAuth2.0的流程**

```
OAuth2.0是一种用于授权的开放标准，它定义了一种授权的流程和规范，可以用于在不同的应用程序之间实现单点登录或授权管理等功能。

它的基本工作流程如下：

1. 应用程序向授权服务器发起授权请求，请求包括申请的授权类型、客户端ID、回调URL等信息。
2. 授权服务器对请求进行验证和审批，决定是否授权，如果授权通过，会返回授权码。
3. 应用程序通过授权码向授权服务器请求访问令牌(Token)，请求中还需要提供客户端ID、密钥等信息，以及之前申请授权时提供的回调URL。
4. 授权服务器对请求进行验证，如果通过验证，会颁发访问令牌。
5. 应用程序使用访问令牌向资源服务器请求资源，请求中需要包括访问令牌和相关参数。
6. 资源服务器对请求进行验证，如果通过验证，会返回请求的资源。

在上述流程中，授权服务器主要负责对应用程序发起的授权请求进行验证和审批，并返回授权码和访问令牌。资源服务器则负责根据访问令牌对请求进行验证和授权，并返回相应的资源。

OAuth2.0标准定义了多种授权类型，包括授权码模式、密码模式、客户端模式等，可以根据具体的应用场景选择不同的授权类型。另外，OAuth2.0也提供了一些扩展机制，如令牌刷新、权限范围等，以适应更多的应用场景。
```

----

#### 1.2.2 注册

涉及到邮件发送和短信发送服务（还有后面的OSS上传头像），可以回顾AK/SK的原理。

```
AK/SK是调用第三方服务时的一种身份认证方式，它的全称是Access Key / Secret Key，通常由第三方服务提供商发放给调用方，用于识别和验证调用方的身份和权限。

AK是Access Key的缩写，是调用方的公钥，可以公开给第三方服务提供商，用于区分不同的调用方。SK是Secret Key的缩写，是调用方的私钥，需要妥善保管，不应该被泄露。

当调用方通过AK/SK向第三方服务发起调用请求时，需要将AK和请求参数一起发送给服务提供商。服务提供商会对AK进行验证，确认调用方的身份。同时，服务提供商还会使用调用方的SK对请求参数进行签名，生成一个签名值。调用方需要将签名值和请求参数一起发送给服务提供商。服务提供商会使用调用方公布的AK和签名值进行匹配，确认请求参数的完整性和真实性。

AK/SK能够保证调用安全的原因主要有两点。一方面，AK作为调用方的公钥，是公开的，不包含敏感信息。另一方面，SK作为调用方的私钥，需要妥善保管，只有调用方自己知道，如果不被泄露，可以保证调用方的身份和权限不被冒充。同时，通过使用SK对请求参数进行签名，可以保证请求的完整性和真实性，避免了中间人攻击和数据篡改等风险。
```

-----

#### 1.2.3 用户信息管理

关于用户标签的设计，我将其设计为多表存储，具体是这样的：

1. 标签首先有分类（大类），如前端、后端、专业、兴趣等等
2. 每个分类下有具体的标签，如Java、Vue、二次元等等
3. 支持用户自定义（新增）标签

在这样的场景下，如果将标签直接以JSON数组存储在user表的某个字段中存在很大问题，例如：如果我要查询所有的标签就非常耗时，它需要查询数据库的所有用户，将每一个用户的标签取出来，然后还要做一个去重的操作。

我的设计是：首先有一个UserTagCategory存储分类，然后UserTag存储用户标签，最后在user中存储其标签的id数字，这样也算结合了各个方法的优点。

当然这样的设计也有缺陷，例如要根据标签查询用户，如果是通过MYSQL查询就非常耗时，因为没有存储标签和用户之间的关系，需要便利数据库的所有用户来获取有这个标签的用户。如果是Elastic存储用户信息的话还好一些。

##### 1.2.4.1 缓存

用户有tags属性，存储自己**选择标签的id构成的数组**

由于标签是一个经常使用的数据，所以将其常驻在redis中，当用户添加新的标签中，直接在redis中添加，同时标签数据也要写入mysql

redis中怎么存？使用List数据结构：

- 对于大类，key是“user:tags:category”，value是每一个Category对象以JSON形式存储形成的List

- 对于每个大类下的tags，key是user:tags:categoryId，value是Tag对象以JSON形式存储形成的List

当服务第一次启动，将标签数据加载到redis。具体做法是实现CommandLineRunnner，重写其中的run方法，这个方法会在启动后执行一次

---

#### 1.2.4 用户关注

- 基础的关注功能

建立follow表，同时查询用户的接口返回的UserVo中，要包含当前用户是否关注了请求的用户（获取用户信息的接口又麻烦了些，还要去查关注表，用异步编排来优化查询速度）

##### 1.2.4.1 消息队列加快响应时间

```
现在要在Java后端完成一个关注功能。
关注的后端逻辑非常复杂，包括
1.判断用户是否登录（要去redis里取）
2.查询follow表判断是否已经关注过了
3.根据用户是否关注进行不同的处理，例如，修改user表的followers和fans字段
这些逻辑不仅复杂，而且存在前后的依赖关系，不像普通的查询可以用异步编排来优化，这使得这个接口的响应时间很长，如果我想加快响应的时间，最好判断用户登录后就直接返回关注成功，应该怎么做？
```

```
如果你想在用户登录后直接返回关注成功，而不进行复杂的后端逻辑判断和处理，可以采用异步处理的方式。

在用户登录后，你可以将关注请求放入消息队列中，然后立即返回关注成功的响应给用户。后台可以开启一个独立的线程或使用异步任务进行消息队列的消费，实现对关注请求的处理。这样可以避免用户等待复杂逻辑的执行时间，提高响应速度。

具体实现步骤如下：

1. 用户登录成功后，将关注请求放入消息队列，消息队列可以使用RabbitMQ、Kafka等常见的消息队列中间件。

2. 在后台创建一个消费者线程或使用异步任务，负责消费消息队列中的关注请求，并执行相关的复杂逻辑。

3. 在消费者线程或异步任务中，进行判断用户是否已经关注过、修改用户表等复杂逻辑的处理。

这样做的好处是用户在登录后可以立即得到关注成功的响应，而复杂逻辑的处理则在后台异步进行，不影响用户的操作体验。同时，通过消息队列的方式，可以提高系统的并发处理能力和可扩展性。

需要注意的是，异步处理的方式可能会引入一定的延迟，因为关注的结果可能不是立即生效，而是在异步处理完成后才真正生效。因此，在设计上需要考虑到这种异步处理的特性，并适当进行提示或通知给用户。
```

可以测试一下使用消息队列前后的响应时间变化（忘记测了）。

---

#### 1.2.5 用户推荐

- 根据标签计算相似度

##### 1.2.5.1 算法

采用余弦相似度算法：

```
现在我要做一个好友推荐的功能，具体的需求如下：
每个用户都有若干标签，例如A用户的标签为[1,2,3]，B用户的标签为[2,3,4,5,6]，C用户的标签为[1,6,7,8]
我该如何计算两两用户之间的相似度，以实现推荐功能？
例如对于A用户，B和C与A的相似度怎么计算？
```

```
计算两个用户之间的相似度可以使用余弦相似度（Cosine Similarity）算法。

余弦相似度是用来比较两个向量的相似程度，其计算方法如下：

1. 首先，需要将用户的标签转换成一个向量，向量的维度是标签的数量，每个维度上的值表示该标签在用户标签中出现的次数或权重，例如A用户的标签[1,2,3]可以表示为向量[1,1,1,0,0,0,0,0]。

2. 然后，计算两个用户标签向量的点积，即将两个向量中对应位置上的元素相乘，并将结果累加起来。

3. 最后，计算两个用户标签向量的范数的乘积，即将两个向量的元素平方并将结果累加起来，然后取平方根。

计算出的结果越接近1，表示两个用户越相似；越接近0，表示两个用户越不相似。

以A用户和B用户为例，计算方法如下：

A用户的标签向量为[1,1,1,0,0,0,0,0]，B用户的标签向量为[0,1,1,1,1,1,0,0]。

计算点积：1*0 + 1*1 + 1*1 + 0*1 + 0*1 + 0*1 + 0*0 + 0*0 = 2

计算A用户标签向量的范数的平方：1^2 + 1^2 + 1^2 + 0^2 + 0^2 + 0^2 + 0^2 + 0^2 = 3

计算B用户标签向量的范数的平方：0^2 + 1^2 + 1^2 + 1^2 + 1^2 + 1^2 + 0^2 + 0^2 = 5

计算A用户和B用户的相似度：2 / (sqrt(3) * sqrt(5)) = 0.6172134

可以按照上述方法计算出A用户和C用户的相似度，以及B用户和C用户的相似度。通过计算每对用户之间的相似度，可以实现好友推荐功能。
```

##### 1.2.5.2 虚拟用户的批量插入

首先进行用户的批量插入（1000个）

| 用户名          | 密码   | 邮箱                   | tags |
| --------------- | ------ | ---------------------- | ---- |
| antares_fake000 | 123456 | antare_fake000@126.com |      |
| ...             |        |                        |      |
| antares_fake999 | 123456 | antare_fake999@126.com |      |

###### 1.2.5.2.1 tags的生成策略

```
1. 生成一个0~10之间的随机数n，代表这个用户的标签数
2. 对所有标签构成的静态数组进行打乱，选取前n个
```

###### 1.2.5.2.2 插入方式比较

- 1条1条地插入，每插入1条就创建一个SqlSession
- saveBatch，只创建一个SqlSession，效率提升
- saveBatch+线程池，效率最高，具体来说，开启10个线程，每个线程存储100个用户

```java
@Test
public void insertUser(){
    String prefix = "antares_fake";
    List<UserTag> userTags = userTagMapper.selectList(null);
    int len = userTags.size();

    long start = System.currentTimeMillis();
    ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        ArrayList<User> users = new ArrayList<>(100);
        for (int j = 0; j < 100; j++) {
            User user = new User();
            user.setUsername(prefix + String.format("%03d", i*100+j));
            user.setPassword("$2a$10$EqQgm9wDCXnhg6qgIv1JceLpLb4g9Cv5b");
            user.setEmail(user.getUsername() + "@126.com");

            //代表该用户的标签数
            int n = RandomUtils.nextInt(0, 11);

            if(n > 0){
                //打乱userTags
                for (int k = 0; k < len; k++) {
                    int index = RandomUtils.nextInt(0,len);
                    UserTag tmp = userTags.get(index);
                    userTags.set(index, userTags.get(k));
                    userTags.set(k, tmp);
                }
                user.setTags(JSON.toJSONString(userTags.subList(0, n)
                        .stream().map(UserTag::getId).collect(Collectors.toList())));
            }
            users.add(user);
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> userService.saveBatch(users), threadPoolExecutor);
        futures.add(future);
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    long end = System.currentTimeMillis();
    System.out.println("耗时" + (end-start));
}
```

```
开启日志的情况下：
耗时16905ms
```

##### 1.2.5.3 缓存预热

获取推荐用户是一个比较耗时的接口，尤其是用户量比较大的时候，因此可以通过缓存预热，每隔24小时执行一次，计算当前数据库所有用户的推荐用户，然后保存至redis。当用户首次进入首页，获取自己的推荐用户就比较快，当再次刷新时，再主动调用接口获取。

核心代码：

```Java
@Scheduled(cron = "0 0 0 1/1 * ?")
public void getRecommendUsersJob(){
    long start = System.currentTimeMillis();

    int tagCount = userTagMapper.selectCount(null);
    List<User> userList = userService.lambdaQuery().select(User::getUid, User::getTags).list();
    ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();

    int loopCount = userList.size() / SystemConstants.RANDOM_RECOMMEND_BATCH_SIZE;
    for (User user : userList) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            userService.getRecommendUserIdsAndCache(user.getUid(), user.getTags(), tagCount, loopCount);
        }, threadPoolExecutor);
        futures.add(future);
    }

    //缓存一个未登录用户，uid为-1
    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        userService.getRandomUserIdsAndCache(tagCount, loopCount);
    }, threadPoolExecutor);
    futures.add(future);

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    long end = System.currentTimeMillis();
    System.out.println("执行耗时:" + (end - start));
}
```

```Java
@Override
public Map<String, Double> getRecommendUserIdsAndCache(Long uid, String tags, int tagCount, int loopCount){
    int i = 0;
    //存储结果的用户id和对应的分数
    Map<String, Double> result = new HashMap<>();

    while (i < loopCount && result.size() < SystemConstants.RECOMMEND_SIZE && !tags.equals("[]")) {
        i++;
        //随机获取50个没关注的用户
        List<User> randomRecommend = baseMapper.getRandomRecommend(uid, SystemConstants.RANDOM_RECOMMEND_BATCH_SIZE);
        //依次计算，超过阈值就放进res
        for (User recommendUser : randomRecommend) {
            double score = AlgorithmUtils.calculate(tags, recommendUser.getTags(), tagCount);
            if (score > SystemConstants.RECOMMEND_THRESHOLD) {
                result.put(recommendUser.getUid().toString(), score);
            }
            //放满了就break
            if (result.size() == SystemConstants.RECOMMEND_SIZE) {
                break;
            }
        }
    }

    //有可能loopSize达到了还没放满（基本把用户表遍历了1遍），那就随机取
    if (result.size() < SystemConstants.RECOMMEND_SIZE) {
        List<User> randomRecommends = baseMapper.getRandomRecommend(uid, SystemConstants.RECOMMEND_SIZE - result.size());
        randomRecommends.forEach(recommendUser -> {
            double score = AlgorithmUtils.calculate(tags, recommendUser.getTags(), tagCount);
            result.put(recommendUser.getUid().toString(), score);
        });
    }
    //将其缓存到redis中（只缓存id，因为考虑到这个数量极大）
    cacheRecommendUids(uid, result);
    return result;
}
```

###### 1.2.5.3.1 execute和executePipelined

在使用spring-data-redis操作redis时，有两个方法可以执行redis命令：execute和executePipelined。

execute方法是同步执行redis命令，即在执行完一个命令之后再执行下一个命令。当需要一次性执行多个redis命令时，可以将这些命令一起放在一个List中，然后通过execute方法批量执行。例如，使用ZADD命令将多个成员添加到ZSet中：

```
List<Object> results = redisTemplate.execute(new RedisCallback<List<Object>>() {
  @Override
  public List<Object> doInRedis(RedisConnection connection) throws DataAccessException {
    connection.openPipeline();
    for (int i = 0; i < 100; i++) {
      connection.zAdd("zset", i, "member" + i);
    }
    return connection.closePipeline();
  }
});
```

executePipelined方法是使用redis的管道技术批量执行redis命令，将多个命令一起发送给redis服务器，然后一起等待返回结果，可以大大提高执行效率。使用executePipelined方法需要注意的是，不能在执行过程中使用非线程安全的RedisConnection实例，需要使用RedisConnectionUtils工具类来获取RedisConnection。例如，使用ZADD命令将多个成员添加到ZSet中：

```java
//将其缓存到redis中（只缓存id，因为考虑到这个数量极大）
stringRedisTemplate.executePipelined(new RedisCallback<Object>() {
    @Override
    public Object doInRedis(RedisConnection connection) throws DataAccessException {
        result.entrySet().forEach(entry -> {
            byte[] keyBytes = stringRedisTemplate.getStringSerializer().serialize(RedisConstants.USER_RECOMMEND_PREFIX + user.getUid());
            byte[] valueBytes = stringRedisTemplate.getStringSerializer().serialize(entry.getKey());
            connection.zAdd(keyBytes, entry.getValue(), valueBytes);
        });
        return null;
    }
});
```

注意，这里返回的结果是null，因为所有命令的结果都会放在results列表中返回。如果需要获取每个命令的执行结果，可以在回调函数中使用RedisSerializer将结果反序列化。

###### 1.2.5.3.2 调优

在不使用线程池的情况下，1000+用户量执行一次这个定时任务就需要3min32s。更何况这还只是查出id信息，没有根据id去查询用户的详细信息！！

![image-20230514010413777](http://image.antares.cool/PicGo/image-20230514010413777.png)

优化点：

- 把日志关了
- 使用线程池

结果：12s5，这个结果非常可观了，质的飞跃！！！

![image-20230514011201613](http://image.antares.cool/PicGo/image-20230514011201613.png)

------

#### 1.2.6 聊天功能

一进入聊天界面，就与后端建立websocket连接。用户进入聊天界面有两种方式：点击某个用户card中的私聊按钮；点击我的消息。前者需要传递一个targetUid参数，而后者不需要。

##### 1.2.6.1 通信的类型

前端与后端的websocket通信/指令可以分为以下几种：

- 建立连接。后端用ConnectionHandler处理，主要是判断连接是否已经建立了，把连接以key-value形式放进一个ConcurrentHashMap里。其中key是这个用户的id，value是<Channel, ConversationId>构成的数对。对于不存在的conversation（两个用户没发过消息，或者用户进入聊天界面没有选择跟谁聊天），其conversationId为0。
- 切换conversation。也就是更换当前选择聊天的对象，后端要对map中相应的conversationId进行修改。为什么map中还要保存用户当前的conversationId？因为这涉及到“记录未读消息数”的问题，对于普通的消息，发送出去就意味着消息接收者的对应conversation的未读消息数要+1，可是有一种情况例外，那就是消息接收者也在线，而且正好打开了跟这个用户的聊天窗口（就是两个人同时在线对聊和其他情况的区别），为了区分这两种情况以对conversation的unread进行不同处理，所以要记录用户当前正打开的conversationId。用户切换conversation还要主动将其unread清0。
- 发送消息。根据对方在不在线，以及如果对方在线的话打开的是哪个conversation窗口做不同的处理。

建立command如下：

```java
public class Command {
    private Integer code; //用来区分要进行什么操作
    private Long uid; //消息来自谁
    private Long conversationId; //要改变的conversationId
    private ChatMessage chatMessage; //具体的消息，也就是在聊天窗口中发的消息
}
```

##### 2.1.6.2 conversation的建立

必须要明确conversation是什么时候建立的。想象一个新的conversation是怎么来的？

用户选择某个用户的card，点击私聊按钮跳转至聊天界面。进入聊天界面后就需要拉取这个用户的所有conversation，判断这些conversation中有没有跟这个用户的，如果有了把它置顶，同时查询这个conversation的历史消息；如果没有，只在前端显示一个（在所有conversation的基础上加一个，它的conversationId是0，在数据库里没有），只有用户发送了第一条消息，conversation才真正插入到数据库中。

两个用户之间的conversation，不论是A向B发送私聊建立的，还是B向A发送私聊建立的，都只用保存一份。那么这就面临一个问题，对于A、B用户，他们的conversation的unread是不同的——conversation用from_unread和to_unread来区分不同用户的unread。当然这也有其缺点，比如某个用户查询自己的conversationList时，vo的转化非常麻烦（因为无法确认这个用户是conversation的from_uid还是to_uid），其次就是更新时必须执行两次更新操作（虽然最后只更新了一个字段），或者1次查询+1次更新，总之两次操作。

我们可以想象这么一个更新操作，conversation有from_uid，to_uid，from_unread，to_unread这四个字段，如果传入的uid参数等于from_uid，就增加from_unread字段，等于to_uid就增加to_unread字段（我原本以为这可以用一个复杂查询解决，但是思来想去还是要用两次操作），实质就是下面这种sql（然而是不合语法的，还是要两次命令才行）

```
update conversation set from_unread = from_unread + 1 where #{uid} = from_uid and id = #{conversationId}
					set to_unread = to_unread + 1 where #{uid} = to_uid and id = #{conversationId}
```

##### 2.1.6.3 聊天消息的处理

conversation建立否？

- 是

  toUid登录否？

    - 是

      toUid是否打开了同一个conversation？

        - 是：存储消息，不增加unread
        - 否：存储消息，增加unread

    - 否：存储消息同时增加unread

- 否：建立conversation，存储消息，增加unread