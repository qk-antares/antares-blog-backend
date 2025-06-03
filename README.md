# Antares博客系统

该项目是基于Spring Boot + Gateway + Redis + Elastic Search + Netty + RabbitMQ的编程博客分享平台，分为网关、用户、博客、搜索4个微服务。用户模块实现了标签系统和好友推荐、关注、私聊、消息通知等功能；博客模块实现了发布、推送、点赞、收藏、评论等功能；搜索模块实现了博客、用户和站外信息的聚合搜索。

项目的部署使用的是自己搭建的服务器，使用IPv6+DDNS实现公网访问。如果你无法访问，可能是由于你的网络环境**不支持IPv6（通常是公司内网和校园网）**，你可以到[IPv6 测试 (test-ipv6.com)](https://test-ipv6.com/)这个网站测试你是否支持IPv6，如果不支持可以连接手机热点后访问，手机网络一般支持IPv6

在线访问：[流火开发社区](http://blog.zqk.asia/note)（测试账号：oj@qq.com 密码：12345678）

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

![img](https://s2.loli.net/2025/06/03/ptPEXSe7qOL8Nsy.png)

![img](https://s2.loli.net/2025/06/03/RwLKzrG6thm8Ejk.png)

#### 1.2 用户信息

- OSS上传头像

  ![img](https://s2.loli.net/2025/06/03/jYuH5SWhIQTFUKZ.png)

- 选择个人标签，支持自定义标签，但不支持自定义分类（学历、专业等）

  ![img](https://s2.loli.net/2025/06/03/4f7m3PjdoDQIApb.png)

- 安全设置

  ![img](https://s2.loli.net/2025/06/03/inrJ8XkZheopR91.png)

- 用户中心

  ![img](https://s2.loli.net/2025/06/03/TRYBvu6pIiw49tD.png)

  查看自己的文章、收藏夹、关注、粉丝信息


#### 1.3 用户推荐

![img](https://s2.loli.net/2025/06/03/C1BVix2QPy5HtDk.png)

相似度计算基于余弦相似度，所有标签有相同的权重。由于这个推荐计算比较慢，为了优化用户体验，每隔24小时自动刷新到redis中，当然用户也可以点击刷新获取新的推荐用户。

#### 1.4 关注功能

![img](https://s2.loli.net/2025/06/03/fOuBR6lLwEJxUZW.png)

![img](https://s2.loli.net/2025/06/03/t1VNqAGP5XzdMZi.png)

![img](https://s2.loli.net/2025/06/03/O1KoBmIVzU8wQEx.png)

#### 1.5 聊天功能

![img](https://s2.loli.net/2025/06/03/9KSMyx8friODVsu.png)

实现了消息持久化、离线消息、滚动分页历史消息、消息通知。左侧可以搜索用户，在线过程中可以正确处理来自多个用户的消息（例如处理来自新conversation的消息，刷新conversation的unread和lastMessage）

![img](https://s2.loli.net/2025/06/03/NjBUA4ImZ159cXY.png)

![img](https://s2.loli.net/2025/06/03/U6Zio8pbKhG4NJP.png)

刷新页面后还可以查看最新的未读消息数

![img](https://s2.loli.net/2025/06/03/db7TFAnKvGJh6x5.png)

### 2. 博客模块

#### 2.1 创建博客

![img](https://s2.loli.net/2025/06/03/MdJXN5k7Gne9jHK.png)

- 文章缩略图的上传

- 文章标签，同样支持自定义标签

  ![img](https://s2.loli.net/2025/06/03/MiE5QYGL4Kc3Nwk.png)

#### 2.2 在线编辑博客

使用了md-editor-rt这个组件库来提供在线编辑功能，同时用户也可以在创建笔记后对笔记的标题、摘要、缩略图等信息进行修改。

![img](https://s2.loli.net/2025/06/03/65WJkDQgobRTCYO.png)

#### 2.3 博客浏览

- 分页查询博客：

  ![img](https://s2.loli.net/2025/06/03/ZPKUYs7gEvV2jnb.png)

  ![img](https://s2.loli.net/2025/06/03/6Z7pVNeD2SsgzGq.png)

- 博客动态，本质还是分页查询，只不过查询的条件是在自己关注的用户中

  ![img](https://s2.loli.net/2025/06/03/iL4mGBxYt3lzKog.png)

  ![img](https://s2.loli.net/2025/06/03/gHp4PXWBNqQfZOD.png)

- 查询笔记Detail

  ![img](https://s2.loli.net/2025/06/03/gve8kItEbflZsGH.png)

  ![img](https://s2.loli.net/2025/06/03/fEVHz3Swux982WU.png)

博客是整个系统的核心，因此在设计过程中大量使用了Redis缓存和lua脚本来优化查询，将在详细设计中讲述。

#### 2.4 博客点赞

![img](https://s2.loli.net/2025/06/03/Dv7ajJT3uIGhXR5.png)

如图所示，用户可以对博客点赞，并且点赞记录使用set存储在redis中的，所以单个用户的点赞不会累积。

#### 2.5 博客收藏

提供了类似B站的收藏夹系统，一篇笔记可以放在多个收藏夹内，用户可以将笔记在收藏夹中自由移动，当这篇笔记在用户的所有收藏夹内都删除时，笔记的收藏数-1

![img](https://s2.loli.net/2025/06/03/L5pdozFGCm8IYUs.png)

![img](https://s2.loli.net/2025/06/03/rRJMLNubt7hoVwQ.png)

header也提供了访问文件夹的方式（分页的）：

![img](https://s2.loli.net/2025/06/03/HgmG72VNEPLlqj4.png)

#### 2.6 博客评论

评论系统也参考了B站，默认展示根评论，当点击展开回复查看子评论，支持子评论和子子...评论

![img](https://s2.loli.net/2025/06/03/2o8e3jK5xzn16ZF.png)

![img](https://s2.loli.net/2025/06/03/IinfVrhqGbPSvO9.png)

#### 2.7 热点博客

文章有score和hot两个属性，前者取决于文章的总浏览量、收藏量、点赞量等，而后者取决于前一个时间段的hot（衰减的）和这个时间段内score的增加值。启动一个定时任务每隔1小时来刷新score和hot，并把热点博客预热到redis缓存中。

#### 2.8 通知功能

包括点赞、评论和消息通知

![img](https://s2.loli.net/2025/06/03/nMmXwzKdvVaRHDU.png)

![img](https://s2.loli.net/2025/06/03/fm6VoOAkvT7Gl1q.png)

### 3. 聚合搜索模块

博客和用户搜索支持关键词+标签

![img](https://s2.loli.net/2025/06/03/rI1oFEyVQqh83gO.png)

![img](https://s2.loli.net/2025/06/03/coluPzLVYa25bNA.png)

利用Jsoup爬虫，可以获取站外搜索结果（博客园做了防爬虫处理，搜索要通过验证，然后返回cookie，该cookie有一定期限，所以爬虫不稳定）

![img](https://s2.loli.net/2025/06/03/XsbnJmxwlRh93iH.png)