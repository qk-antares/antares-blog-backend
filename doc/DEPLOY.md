# 部署

以下是一个比较粗略的部署教程，如果你们遇到了其他问题，可以提issue

## 后端

### 配置文件

将application-example.yml和bootstrap-example.yml重命名为application.yml和bootstrap.yml，并将其中的配置项修改为自己的。

#### Nacos

![image-20230918142256083](http://image.antares.cool/PicGo/Project/Blog/Deploy/1708a96926ed6d5787d1f6b8b3bc2a6438279841.png)

![image-20230918142310171](http://image.antares.cool/PicGo/Project/Blog/Deploy/74cf02b029485f8ed210160ef14d2d7438279841.png)

- 需要将bootstrap.yml中的nacos地址、username和password设置成自己的（本地nacos似乎不用设置username和password，默认是nacos/nacos）。
- bootstrap-dev.yml中namespace设置成自己创建的命名空间（一般是dev一个命名空间，test一个命名空间，prod一个，以此类推）

---

#### MySQL & Redis & RabbitMQ

![image-20230918142715631](http://image.antares.cool/PicGo/Project/Blog/Deploy/ff0a6df6c1aaf91f15c4e824a33e2cb738279841.png)

略，主要是配置地址，用户名，密码

---

#### 邮箱

member微服务的一个配置项

![image-20230918142819342](http://image.antares.cool/PicGo/Project/Blog/Deploy/e67f484f98632480804a505b5d98b29f38279841.png)

[SpringBoot实现QQ邮箱发送功能_springboot qq邮箱_☆夜幕星河℡的博客-CSDN博客](https://blog.csdn.net/qq_45660133/article/details/123499839)

参考这篇博客，需要到自己的QQ邮箱中操作一下，主要是需要获取授权码

#### Cookie作用域

member微服务，不起眼但是重要的一个配置

![image-20230918143022120](http://image.antares.cool/PicGo/Project/Blog/Deploy/a1a2abc7d1207dd447ae981ab128f90938279841.png)

#### 第三方服务

![image-20230918143106688](http://image.antares.cool/PicGo/Project/Blog/Deploy/28731937374cf360fbf255994fb0118b38279841.png)

sms是使用的榛子云短信，oss使用的是七牛云，oauth是gitee。可以搜索相关的博客看这些配置项如何配置，都需要你们到自己的账号上操作一下。如果你只想把项目跑起来，可以先不配置

---

### 服务器环境

使用的Debian+宝塔Linux

#### Nacos & Nacos & MySQL & Redis & RabbitMQ

略，我是全部通过docker部署的

![image-20230918144530735](http://image.antares.cool/PicGo/Project/Blog/Deploy/29c45f6b50a371afaae09e5c20f92d8538279841.png)

---

#### Nginx

##### Linux下

安装OpenResty

![image-20230918143518378](http://image.antares.cool/PicGo/Project/Blog/Deploy/7a71d20d3241958a014ba1e31aa07bb838279841.png)

将nginx文件夹下的配置拷贝到对应的位置（antares.conf除外）

![image-20230918143631534](http://image.antares.cool/PicGo/Project/Blog/Deploy/6010971261a571ee501753aa675392ff38279841.png)

创建站点

![image-20230918144003433](http://image.antares.cool/PicGo/Project/Blog/Deploy/45325ea467556d7a5ca5295e16130b3a38279841.png)

配置站点的配置文件，具体内容就是项目nginx/conf下antares.conf的内容

##### Windows下

自行搜索OpenResty下载安装

同上拷贝文件，只是文件名略有不同，luajit对应lua文件夹

![image-20230918143656604](http://image.antares.cool/PicGo/Project/Blog/Deploy/1ca3893ce7c8fd57827f189edf115f0a38279841.png)

conf中配置

nginx.conf

```
worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/json;
    sendfile        on;
    keepalive_timeout  65;
    resolver 8.8.8.8;

    #lua 模块
    lua_package_path "../lualib/?.lua;;";
    #c模块     
    lua_package_cpath "../lualib/?.so;;";
    # 共享字典，也就是本地缓存，名称叫做：article_cache，大小150m
    # lua_shared_dict article_cache 150m; 
    
    upstream antares_gateway {
        server 127.0.0.1:8899;
    }

    upstream antares {
        server 127.0.0.1:8000;
    }
    include conf.d/*.conf;
}
```

conf.d/antares.conf

```
server {
    listen 80;
    server_name blog.antares.cool;

    # 指定前端项目所在的位置
    location /static/ {
        root html;
        index index.html index.htm;
    }

    location /test {
      default_type application/json;
      content_by_lua_file lua/test.lua;
    }

    location ~ /api/ngx/blog/article/(\d+)$ {
        default_type application/json;
        content_by_lua_file lua/article.lua;
        include cors.conf;
    }

    location ~ /api/ngx/blog/article/(\d+)/basic {
        default_type application/json;
        content_by_lua_file lua/article_basic.lua;
        include cors.conf;
    }

    location ~ /api/ngx/blog/article/(\d+)/content {
        default_type application/json;
        content_by_lua_file lua/article_content.lua;
        include cors.conf;
    }

    location /api {
        proxy_set_header Host $host; 
        proxy_pass http://antares_gateway;
    }

    location /{
        proxy_set_header Host localhost; 
        proxy_pass http://antares;
    }

    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root html;
    }
}
```

---

### 其他

- 执行sql下的脚本创建数据库

- 进入ES创建索引

  ```
  PUT article_v1
  {
    "aliases": {
      "article": {}
    },
    "mappings": {
      "properties": {
        "title": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "summary": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "content": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
          "status": {
            	"type": "integer"  
          },
        "tags": {
          "type": "keyword"
        },
        "username": {
          "type": "keyword"
        },
          "score": {
  			"type": "integer"
          },
        "createTime": {
          "type": "date"
        },
        "updateTime": {
          "type": "date"
        }
      }
    }
  }
  ```

- 执行antares-search下test/job/FullSyncPostToEs中的两个函数，将MySQL中的用户和文章信息全量同步至ES

## 前端

![image-20230918145018965](http://image.antares.cool/PicGo/Project/Blog/Deploy/8a5ac66da0ec2a3dd5b18a9468e22eac38279841.png)

baseURL修改成自己的后端地址

![image-20230918145112630](http://image.antares.cool/PicGo/Project/Blog/Deploy/9582b64178a1990cb1906d57f45fe3fb38279841.png)

Notification/components/Chat.tsx中，WebSocket地址也要修改成自己的

![image-20230918145220676](http://image.antares.cool/PicGo/Project/Blog/Deploy/42d62169851bac27d932ddd818bdda9b38279841.png)

User/Login/index.tsx中，这个gitee的跳转地址也要修改成自己的

自己代码写的比较不合理，以后这些应该统一用一个常量文件管理