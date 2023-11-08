# Antares博客系统

该项目是基于Spring Boot + Gateway + Redis + Elastic Search + Netty + RabbitMQ的编程博客分享平台，分为网关、用户、博客、搜索4个微服务。用户模块实现了标签系统和好友推荐、关注、私聊、消息通知等功能；博客模块实现了发布、推送、点赞、收藏、评论等功能；搜索模块实现了博客、用户和站外信息的聚合搜索。

项目的部署使用的是自己搭建的服务器，使用IPv6+DDNS实现公网访问。如果你无法访问，可能是由于你的网络环境**不支持IPv6（通常是公司内网和校园网）**，你可以到[IPv6 测试 (test-ipv6.com)](https://test-ipv6.com/)这个网站测试你是否支持IPv6，如果不支持可以连接手机热点后访问，手机网络一般支持IPv6

在线访问：[流火开发社区](http://blog.antares.cool/note)（测试账号：oj@qq.com 密码：12345678）

github仓库：

- 后端：[qk-antares/antares-blog-backend (github.com)](https://github.com/qk-antares/antares-blog-backend)
- 前端：[qk-antares/antares-blog-frontend (github.com)](https://github.com/qk-antares/antares-blog-frontend)

## 目录

- [介绍](https://github.com/qk-antares/antares-blog-backend/blob/master/README.md)
- [用户微服务](https://github.com/qk-antares/antares-blog-backend/blob/master/doc/USER_MODULE.md)
- [博客微服务](https://github.com/qk-antares/antares-blog-backend/blob/master/doc/ARTICLE_MODULE.md)
- [搜索微服务](https://github.com/qk-antares/antares-blog-backend/blob/master/doc/SEARCH_MODULE.md)
- [优化和测试](https://github.com/qk-antares/antares-blog-backend/blob/master/doc/OPTIMIZE_TEST.md)
- [部署](https://github.com/qk-antares/antares-blog-backend/blob/master/doc/DEPLOY.md)

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

![image-20230530200851071](https://qk-antares.github.io/img/blog/readme/80062281e65837942b02db643a6cf3d838279841.png)

![image-20230530200916067](https://qk-antares.github.io/img/blog/readme/b05ce0ca60fede10a3fd32b57bf96ef738279841.png)

#### 1.2 用户信息

- OSS上传头像

  ![image-20230530201103411](https://qk-antares.github.io/img/blog/readme/d1ccdfd139fe9e9ad3c70320a409c89238279841.png)

- 选择个人标签，支持自定义标签，但不支持自定义分类（学历、专业等）

  ![image-20230530201150352](https://qk-antares.github.io/img/blog/readme/0c77b8339cd4144c205c1813b518ef8838279841.png)

- 安全设置

  ![image-20230530201222717](https://qk-antares.github.io/img/blog/readme/66272a7e507fe646a4cc03cff4bea9a938279841.png)

- 用户中心

  ![image-20230530205410139](https://qk-antares.github.io/img/blog/readme/ec7b2b4e81d4da107fc7480ec60450c238279841.png)

  查看自己的文章、收藏夹、关注、粉丝信息


#### 1.3 用户推荐

![image-20230530202218784](https://qk-antares.github.io/img/blog/readme/95027d8d022887323c7cdb7dd5a229cf38279841.png)

相似度计算基于余弦相似度，所有标签有相同的权重。由于这个推荐计算比较慢，为了优化用户体验，每隔24小时自动刷新到redis中，当然用户也可以点击刷新获取新的推荐用户。

#### 1.4 关注功能

![image-20230530201620512](https://qk-antares.github.io/img/blog/readme/fedee88e5d8ec7ef51c81b7f66b2485b38279841.png)

![image-20230530202246245](https://qk-antares.github.io/img/blog/readme/bfdb288bdbfbfce933552c7fcc6d187338279841.png)

![image-20230530202324380](https://qk-antares.github.io/img/blog/readme/316d2b47777034b753c8b5e5c5d7caf338279841.png)

#### 1.5 聊天功能

![image-20230530202343935](https://qk-antares.github.io/img/blog/readme/778656c058bc8732783fb4ad1263992438279841.png)

实现了消息持久化、离线消息、滚动分页历史消息、消息通知。左侧可以搜索用户，在线过程中可以正确处理来自多个用户的消息（例如处理来自新conversation的消息，刷新conversation的unread和lastMessage）

![image-20230530202713827](https://qk-antares.github.io/img/blog/readme/25ca2cc8c36eae24a8385a2a072b0ad438279841.png)

![image-20230530203205781](https://qk-antares.github.io/img/blog/readme/a5fbb501b20da909723c022e167f3b8f38279841.png)

刷新页面后还可以查看最新的未读消息数

![image-20230530205055763](https://qk-antares.github.io/img/blog/readme/0b1b696e0bcb0212118682773016391d38279841.png)

### 2. 博客模块

#### 2.1 创建博客

![image-20230530205514406](https://qk-antares.github.io/img/blog/readme/81e345c81087ecaa09b0e11b5a070f8238279841.png)

- 文章缩略图的上传

- 文章标签，同样支持自定义标签

  ![image-20230530205617002](https://qk-antares.github.io/img/blog/readme/afc6fe21efd3b78f42c41cbee6fb799238279841.png)

#### 2.2 在线编辑博客

使用了md-editor-rt这个组件库来提供在线编辑功能，同时用户也可以在创建笔记后对笔记的标题、摘要、缩略图等信息进行修改。

![image-20230530205820157](https://qk-antares.github.io/img/blog/readme/7099a190724e89d79f372414405b005638279841.png)

#### 2.3 博客浏览

- 分页查询博客：

  ![image-20230530211142982](https://qk-antares.github.io/img/blog/readme/fd4c685a4d48eb593710dd07a5161ee938279841.png)

  ![image-20230530211224006](https://qk-antares.github.io/img/blog/readme/5a2d2c929584c0d173efe3db3d3e8edc38279841.png)

- 博客动态，本质还是分页查询，只不过查询的条件是在自己关注的用户中

  ![image-20230530211431616](https://qk-antares.github.io/img/blog/readme/bea169bff58fbc6480ebee0ac6cf2cf138279841.png)

  ![image-20230530211559673](https://qk-antares.github.io/img/blog/readme/990251cc09a248a775c497559968bfb938279841.png)

- 查询笔记Detail

  ![image-20230530210852750](https://qk-antares.github.io/img/blog/readme/c22f285a0d891b93d0fb9578d178a82838279841.png)

  ![image-20230530210916464](https://qk-antares.github.io/img/blog/readme/4dd725206f6571309ac5e5f4aa53ca0638279841.png)

博客是整个系统的核心，因此在设计过程中大量使用了Redis缓存和lua脚本来优化查询，将在详细设计中讲述。

#### 2.4 博客点赞

![image-20230530210036401](https://qk-antares.github.io/img/blog/readme/333fec70812e8898bdcb83fd58d1788038279841.png)

如图所示，用户可以对博客点赞，并且点赞记录使用set存储在redis中的，所以单个用户的点赞不会累积。

#### 2.5 博客收藏

提供了类似B站的收藏夹系统，一篇笔记可以放在多个收藏夹内，用户可以将笔记在收藏夹中自由移动，当这篇笔记在用户的所有收藏夹内都删除时，笔记的收藏数-1

![image-20230530210323315](https://qk-antares.github.io/img/blog/readme/933301afa02f1244c7a31b5a6ea6b12238279841.png)

![image-20230530210342014](https://qk-antares.github.io/img/blog/readme/7a6db071921fa31ec786343d7efa046838279841.png)

header也提供了访问文件夹的方式（分页的）：

![image-20230530210441638](https://qk-antares.github.io/img/blog/readme/e60b5f7491f36e1dd4334201c8a7f7f838279841.png)

#### 2.6 博客评论

评论系统也参考了B站，默认展示根评论，当点击展开回复查看子评论，支持子评论和子子...评论

![image-20230530210544367](https://qk-antares.github.io/img/blog/readme/0b4c81b220ac0d87c04448373b9e39c438279841.png)

![image-20230530210720295](https://qk-antares.github.io/img/blog/readme/ec9f62c63c179d2ec9d83c4c8129845438279841.png)

#### 2.7 热点博客

文章有score和hot两个属性，前者取决于文章的总浏览量、收藏量、点赞量等，而后者取决于前一个时间段的hot（衰减的）和这个时间段内score的增加值。启动一个定时任务每隔1小时来刷新score和hot，并把热点博客预热到redis缓存中。

#### 2.8 通知功能

包括点赞、评论和消息通知

![image-20230530220511400](https://qk-antares.github.io/img/blog/readme/d428f2ee6a88d64ae54db7492e6ad50a38279841.png)

![image-20230530220534803](https://qk-antares.github.io/img/blog/readme/daff37f78f28f26035188ba286e2e35a38279841.png)

### 3. 聚合搜索模块

博客和用户搜索支持关键词+标签

![image-20230530220143814](https://qk-antares.github.io/img/blog/readme/29598f862c98beaea6c6978adb0ab09d38279841.png)

![image-20230530220228431](https://qk-antares.github.io/img/blog/readme/bdaa1ddabf3cc9b472c7e71807c871b338279841.png)

利用Jsoup爬虫，可以获取站外搜索结果（博客园做了防爬虫处理，搜索要通过验证，然后返回cookie，该cookie有一定期限，所以爬虫不稳定）

![image-20230530220413339](https://qk-antares.github.io/img/blog/readme/6247bf868f6c4eb117064c246664193438279841.png)