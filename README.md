# Antares博客系统

在线访问：http://blog.antares.cool

项目的部署使用的是自己搭建的服务器，使用IPv6+DDNS实现公网访问。如果你无法访问，可能是由于你的网络环境不支持IPv6（通常是公司内网和校园网），你可以到IPv6 测试 (test-ipv6.com)这个网站测试你是否支持IPv6，如果不支持可以连接手机热点后访问，手机网络一般支持IPv6

## 技术栈

### 后端

- Spring Cloud Gateway：
    1. 最简单的应用，将请求转发到不同的微服务
- Spring Boot：
    1. AOP切面编程搭配自定义异常来对异常做统一处理；
    2. 定时任务（计算文章得分并刷新热榜，每日刷新推荐用户，定时将文章浏览量从redis同步到数据库）
    3. 使用validation相关注解对请求的参数进行校验
- MySQL
    1. 数据持久化，项目涉及到了极少量复杂查询
- Redis：
    1. 缓存，包括热点文章，文章点赞、收藏、浏览量，推荐用户，消息通知数等。其中文章点赞、收藏、浏览量、消息通知是永久存储的（结合OpenResty的lua脚本，把查询缓存的逻辑前置到nginx，进一步提高响应速度）
    2. 分布式锁（只用到了定时任务）
- Elastic Search：
    1. 搭配Jsoup爬虫，实现了聚合搜索功能
    2. 搭配canal实现MySQL和ES的数据同步
- Netty：
    1. 私聊（消息持久化、离线消息、消息通知，在线聊天过程中切换对话，接收不同对话的消息...）
- RabbitMQ：
    1. 最简单的应用，异步处理点赞、收藏、关注、发邮件等消息，提高响应速度。只用到了直接交换机
- Nginx：
    1. 反向代理服务器
    2. lua脚本
- 其他：
    1. CompletableFuture异步编程
    2. 余弦相似度算法

---

### 前端

- React+umijs4+ant design pro（仅限会使用，不深入）

---

## 功能模块

### 1. 用户模块

#### 1.1 登录、注册

- OAuth2第三方登录
- 接入了短信和邮件发送服务
- 使用token作为会话保持的途径

![image-20230530200851071](http://image.antares.cool/PicGo/image-20230530200851071.png)

![image-20230530200916067](http://image.antares.cool/PicGo/image-20230530200916067.png)

#### 1.2 用户信息

- OSS上传头像

  ![image-20230530201103411](http://image.antares.cool/PicGo/image-20230530201103411.png)

- 选择个人标签，支持自定义标签，但不支持自定义分类（学历、专业等）

  ![image-20230530201150352](http://image.antares.cool/PicGo/image-20230530201150352.png)

- 安全设置

  ![image-20230530201222717](http://image.antares.cool/PicGo/image-20230530201222717.png)

- 用户中心

  ![image-20230530205410139](http://image.antares.cool/PicGo/image-20230530205410139.png)

  查看自己的文章、收藏夹、关注、粉丝信息


#### 1.3 用户推荐

![image-20230530202218784](http://image.antares.cool/PicGo/image-20230530202218784.png)

相似度计算基于余弦相似度，所有标签有相同的权重。由于这个推荐计算比较慢，为了优化用户体验，每隔24小时自动刷新到redis中，当然用户也可以点击刷新获取新的推荐用户。

#### 1.4 关注功能

![image-20230530201620512](http://image.antares.cool/PicGo/image-20230530201620512.png)

![image-20230530202246245](http://image.antares.cool/PicGo/image-20230530202246245.png)

![image-20230530202324380](http://image.antares.cool/PicGo/image-20230530202324380.png)

#### 1.5 聊天功能

![image-20230530202343935](http://image.antares.cool/PicGo/image-20230530202343935.png)

实现了消息持久化、离线消息、滚动分页历史消息、消息通知。左侧可以搜索用户，在线过程中可以正确处理来自多个用户的消息（例如处理来自新conversation的消息，刷新conversation的unread和lastMessage）

![image-20230530202713827](http://image.antares.cool/PicGo/image-20230530202713827.png)

![image-20230530203205781](http://image.antares.cool/PicGo/image-20230530203205781.png)

刷新页面后还可以查看最新的未读消息数

![image-20230530205055763](http://image.antares.cool/PicGo/image-20230530205055763.png)

### 2. 博客模块

#### 2.1 创建博客

![image-20230530205514406](http://image.antares.cool/PicGo/image-20230530205514406.png)

- 文章缩略图的上传

- 文章标签，同样支持自定义标签

  ![image-20230530205617002](http://image.antares.cool/PicGo/image-20230530205617002.png)

#### 2.2 在线编辑博客

使用了md-editor-rt这个组件库来提供在线编辑功能，同时用户也可以在创建笔记后对笔记的标题、摘要、缩略图等信息进行修改。

![image-20230530205820157](http://image.antares.cool/PicGo/image-20230530205820157.png)

#### 2.3 博客浏览

- 分页查询博客：

  ![image-20230530211142982](http://image.antares.cool/PicGo/image-20230530211142982.png)

  ![image-20230530211224006](http://image.antares.cool/PicGo/image-20230530211224006.png)

- 博客动态，本质还是分页查询，只不过查询的条件是在自己关注的用户中

  ![image-20230530211431616](http://image.antares.cool/PicGo/image-20230530211431616.png)

  ![image-20230530211559673](http://image.antares.cool/PicGo/image-20230530211559673.png)

- 查询笔记Detail

  ![image-20230530210852750](http://image.antares.cool/PicGo/image-20230530210852750.png)

  ![image-20230530210916464](http://image.antares.cool/PicGo/image-20230530210916464.png)

博客是整个系统的核心，因此在设计过程中大量使用了Redis缓存和lua脚本来优化查询，将在详细设计中讲述。

#### 2.4 博客点赞

![image-20230530210036401](http://image.antares.cool/PicGo/image-20230530210036401.png)

如图所示，用户可以对博客点赞，并且点赞记录使用set存储在redis中的，所以单个用户的点赞不会累积。

#### 2.5 博客收藏

提供了类似B站的收藏夹系统，一篇笔记可以放在多个收藏夹内，用户可以将笔记在收藏夹中自由移动，当这篇笔记在用户的所有收藏夹内都删除时，笔记的收藏数-1

![image-20230530210323315](http://image.antares.cool/PicGo/image-20230530210323315.png)

![image-20230530210342014](http://image.antares.cool/PicGo/image-20230530210342014.png)

header也提供了访问文件夹的方式（分页的）：

![image-20230530210441638](http://image.antares.cool/PicGo/image-20230530210441638.png)

#### 2.6 博客评论

评论系统也参考了B站，默认展示根评论，当点击展开回复查看子评论，支持子评论和子子...评论

![image-20230530210544367](http://image.antares.cool/PicGo/image-20230530210544367.png)

![image-20230530210720295](http://image.antares.cool/PicGo/image-20230530210720295.png)

#### 2.7 热点博客

文章有score和hot两个属性，前者取决于文章的总浏览量、收藏量、点赞量等，而后者取决于前一个时间段的hot（衰减的）和这个时间段内score的增加值。启动一个定时任务每隔1小时来刷新score和hot，并把热点博客预热到redis缓存中。

#### 2.8 通知功能

包括点赞、评论和消息通知

![image-20230530220511400](http://image.antares.cool/PicGo/image-20230530220511400.png)

![image-20230530220534803](http://image.antares.cool/PicGo/image-20230530220534803.png)

### 3. 聚合搜索模块

博客和用户搜索支持关键词+标签

![image-20230530220143814](http://image.antares.cool/PicGo/image-20230530220143814.png)

![image-20230530220228431](http://image.antares.cool/PicGo/image-20230530220228431.png)

利用Jsoup爬虫，可以获取站外搜索结果（博客园做了防爬虫处理，搜索要通过验证，然后返回cookie，该cookie有一定期限，所以爬虫不稳定）

![image-20230530220413339](http://image.antares.cool/PicGo/image-20230530220413339.png)