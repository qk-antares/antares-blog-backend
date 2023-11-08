### 本机部署流程

#### VMware

##### 创建虚拟机

自定义新建虚拟机，选择稍后安装操作系统，内存至少给8G

![image-20231107133759544](https://qk-antares.github.io/img/blog/deploy/1563e674d0e80d50fe3d5ae25484e69638279841.png)

![image-20231107133828669](https://qk-antares.github.io/img/blog/deploy/33bcd579e0db757006380767c890ba9b38279841.png)

![image-20231107133650802](https://qk-antares.github.io/img/blog/deploy/53a57bc337d2f105412e476ff8848b1438279841.png)

##### 安装Debian

注意点：

1. VMware选择镜像（我的是firmware-11.7.0-amd64-netinst.iso）

2. 选择Graphical install（这个只是可视化安装界面，不意味着最后安装的系统是带GUI的）

3. 选择中文

4. 磁盘分区，因为我是16G内存，所以把swap改为了16G

   ![image-20231107134416125](https://qk-antares.github.io/img/blog/deploy/8bab1d4547e1679cad55fa96871cfaa638279841.png)

5. 选择清华源，实测比官方源快

   ![image-20231107134600377](https://qk-antares.github.io/img/blog/deploy/36f5405900bbf201f2a2ffd36408521738279841.png)

6. （重要）取消勾选Debian桌面环境和GNOME，勾选SSH server，这样我们最终安装的才是无GUI的

   ![image-20231107135320301](https://qk-antares.github.io/img/blog/deploy/b8454217851d3f4330ce6a0c443ab2bc38279841.png)

##### 基础配置

###### vim和ssh

```shell
# 在VMware操作...
# 安装vim
apt-get install vim

# 配置ssh
vim /etc/ssh/sshd_config
# 修改其中PermitRootLogin后的prohibit-password为yes
/etc/init.d/ssh restart

# ip addr查看ip，之后xshell连接...

# 配置vim
vim ~/.vimrc
# 添加如下内容（修改tab的格数，以及支持鼠标右键直接粘贴）
set tabstop=4
syntax enable
if has('mouse')
    set mouse-=a
endif
```

###### 静态IP

```shell
vim /etc/network/interfaces

auto eth0 # 这里的eth0是网卡名，每个人可能不一样（比如ens3，ens192）
allow-hotplug eth0
iface eth0 inet static # static表示使用固定ip，dhcp表述使用动态ip
address 192.168.32.128 # 设置ip地址，前面的三个段要和你VMware的网络配置保持一致
netmask 255.255.255.0 # 设置子网掩码
gateway 192.168.32.2 # 设置网关，同样要和VMware保持一致
```

![image-20231107140035430](https://qk-antares.github.io/img/blog/deploy/6d152df09720fa9ed282eb06aadebb0c38279841.png)

###### 宝塔

```
wget -O install.sh https://download.bt.cn/install/install-ubuntu_6.0.sh && bash install.sh ed8484bec
bt 5
123456
bt 6
debian
bt 8
19876
```

在面板设置里，修改安全入口为/debian

![image-20231107141930072](https://qk-antares.github.io/img/blog/deploy/390c4afc08240b443060a55774767c0138279841.png)

根据个人喜好设置隐藏菜单

![image-20231107142152646](https://qk-antares.github.io/img/blog/deploy/882515d29e0cd4f404996217a555ec2338279841.png)

到这里基础的虚拟机环境已经创建好了，建议这个虚拟机不要动了，后面从这个虚拟机**克隆**然后再进行操作，方便**复用**这个基础虚拟机环境（复用时记得修改静态IP，防止IP冲突）。

![image-20231107142222330](https://qk-antares.github.io/img/blog/deploy/052779451c801affaa02c32ed5d1066e38279841.png)

![image-20231107142524188](https://qk-antares.github.io/img/blog/deploy/8e0563eee23c1eab64266b43091c56a038279841.png)

---

#### 依赖部署

以下操作应该在克隆的虚拟机上进行，并且注意放行对应的端口

##### JDK

提前准备好linux上的jdk安装包，以jdk-8u361-linux-x64.tar.gz为例

```shell
tar -zxvf jdk-8u361-linux-x64.tar.gz -C /software

vim /etc/profile

# set java environment
export JAVA_HOME=/software/jdk1.8.0_361
export PATH=$PATH:$JAVA_HOME/bin
export JRE_HOME=$JAVA_HOME/jre　
export CLASSPATH=.:$JAVA_HOME/jre/lib/rt.jar:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar

source /etc/profile
```

##### Nginx

安装openresty版本

![image-20231107142754445](https://qk-antares.github.io/img/blog/deploy/01179922fe37d8b4f069f0af7899564338279841.png)

根据自己的情况修改worker进程数，默认是CPU核数，我是16核，但用不到这么多worker线程，因为每个worker线程占用几十MB内存

![image-20230818164523913](https://qk-antares.github.io/img/blog/deploy/f08cf0cc2da7a9fd1978eae56f3d68738bc78a30.png)

---

##### Docker

直接安装

![image-20231107142822578](https://qk-antares.github.io/img/blog/deploy/360dbffcdca32b55ce612c09c869226c38279841.png)

安装完成后记得配置源，不然后面下载镜像巨慢，进入[阿里云](https://cr.console.aliyun.com/)，执行镜像加速器里的命令（debian可能要去掉sudo）

![image-20231107143350062](https://qk-antares.github.io/img/blog/deploy/72e261074d5c2adf151def97b6d969ca38279841.png)

```shell
mkdir -p /etc/docker
tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": ["https://ao8etm74.mirror.aliyuncs.com"]
}
EOF
systemctl daemon-reload
systemctl restart docker
```

---

##### MySQL

依然选择直接安装，版本是8.0，安装完后配置root密码，配置允许root远程登录

![image-20231107151322827](https://qk-antares.github.io/img/blog/deploy/79f5e49b50bc36c81ad0782f414b06cb38279841.png)

```shell
mysql -uroot -p

# 使用数据库mysql
use mysql;
# 查看主机和用户
mysql> select host, user from user;
+-----------+------------------+
| host      | user             |
+-----------+------------------+
| localhost | mysql.infoschema |
| localhost | mysql.session    |
| localhost | mysql.sys        |
| localhost | root             |
+-----------+------------------+
4 rows in set (0.00 sec)

mysql> update user set host = '%' where user = 'root';


mysql> flush privileges;
```

（注：MySQL的配置文件默认存放在/etc目录下的my.cnf，数据库文件存在/www/server/data目录下。）

---

##### Redis

直接安装，版本是6.2，在这个面板设置密码，并且把配置文件中的bind注释掉，从而可以所有IP连接Redis（线上环境可以设置成127.0.0.1或后端的IP）

![image-20231107161353727](https://qk-antares.github.io/img/blog/deploy/2699c74fc39336375b392f0e3ecd6f7e38279841.png)

---

##### RabbitMQ

选择软件商店安装

![image-20231107215236244](https://qk-antares.github.io/img/blog/deploy/64b111b657d29a8f7de0310d7aedfc2b38279841.png)

---

##### ElasticSearch

```shell
# 创建目录
mkdir -p /docker/elasticsearch/config
mkdir -p /docker/elasticsearch/data
mkdir -p /docker/elasticsearch/plugins

# 配置文件
echo "http.host: 0.0.0.0" >> /docker/elasticsearch/config/elasticsearch.yml
# 保证权限
chmod -R 777 /docker/elasticsearch/

# 设置密码
vim /docker/elasticsearch/config/elasticsearch.yml
```

```properties
http.host: 0.0.0.0
xpack.security.enabled: true
```

```shell
# 拷贝ik分词器文件

# 创建容器
docker run --name elasticsearch -p 9200:9200 -p 9300:9300 \
-e "discovery.type=single-node" \
-e ES_JAVA_OPTS="-Xms256m -Xmx256m" \
-v /docker/elasticsearch/config/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml \
-v /docker/elasticsearch/data:/usr/share/elasticsearch/data \
-v /docker/elasticsearch/plugins:/usr/share/elasticsearch/plugins \
--restart=always \
-d elasticsearch:7.4.2

# 进入docker容器
docker exec -it elasticsearch bash
# 设置密码-手动设置密码
elasticsearch-setup-passwords interactive
# 访问
curl 127.0.0.1:9200 -u elastic
```

----

##### Kibana

```shell
# 首先编辑配置文件
vim /docker/elasticsearch/config/kibana.yml
```

```properties
server.name: kibana
server.host: "0"
elasticsearch.hosts: [ "http://172.17.0.1:9200" ]
xpack.monitoring.ui.container.elasticsearch.enabled: true

# 此处设置elastic的用户名和密码
elasticsearch.username: elastic
elasticsearch.password: 123456
```

```shell
docker run --name kibana \
-v /docker/elasticsearch/config/kibana.yml:/usr/share/kibana/config/kibana.yml \
-p 5601:5601 \
--restart=always \
-d kibana:7.4.2
```

---

##### <span id=minio>Minio</span>

```shell
# 创建目录
mkdir -p /docker/minio/data
mkdir -p /docker/minio/config

docker run -p 9000:9000 -p 9090:9090 \
     --name minio \
     -d --restart=always \
     -e "MINIO_ROOT_USER=minio" \
     -e "MINIO_ROOT_PASSWORD=12345678" \
     -v /docker/minio/data:/data \
     -v /docker/minio/config:/root/.minio \
     minio/minio server \
     /data --console-address ":9090" -address ":9000"
```

9090是web控制面板的端口

---

##### Nacos

```shell
# 加载镜像（或下载），因为我使用阿里源老是下载到老版本的nacos，所以一般我直接加载之前自己打包的镜像
docker load -i nacos.tar
docker tag 0514f8ffee17 nacos/nacos-server:latest
docker pull nacos/nacos-server
#新建logs目录
mkdir -p /docker/nacos/logs/
#新建conf目录
mkdir -p /docker/nacos/conf/

# 创建nacos_config数据库

docker run -p 8848:8848 --name nacos -d nacos/nacos-server

docker cp nacos:/home/nacos/logs /docker/nacos
docker cp nacos:/home/nacos/conf /docker/nacos

docker stop nacos
docker remove nacos

# 开放端口8848 9848

vim /docker/nacos/conf/application.properties
```

nacos完整配置

```properties
# spring
server.servlet.contextPath=/nacos
server.contextPath=/nacos
server.port=8848
server.tomcat.accesslog.max-days=30
server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D %{User-Agent}i %{Request-Source}i
server.tomcat.accesslog.enabled=${TOMCAT_ACCESSLOG_ENABLED:false}
server.error.include-message=ALWAYS
# default current work dir
server.tomcat.basedir=file:.
#*************** Config Module Related Configurations ***************#
### Deprecated configuration property, it is recommended to use `spring.sql.init.platform` replaced.
spring.datasource.platform=mysql
spring.sql.init.platform=${SPRING_DATASOURCE_PLATFORM:}
nacos.cmdb.dumpTaskInterval=3600
nacos.cmdb.eventTaskInterval=10
nacos.cmdb.labelTaskInterval=300
nacos.cmdb.loadDataAtStart=false
db.num=1
db.url.0=jdbc:mysql://172.17.0.1:3306/nacos_config?allowPublicKeyRetrieval=true&characterEncoding=utf8&connectTimeout=1000&socketTimeout=30000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=UTC
db.user.0=root
db.password.0=123456

nacos.core.auth.system.type=nacos
nacos.core.auth.enabled=true
### The token expiration in seconds:
nacos.core.auth.plugin.nacos.token.expire.seconds=18000
### The default token:
nacos.core.auth.plugin.nacos.token.secret.key=Sji0q%f^zEHaGXsLyz^hNIjyf3Unr2%N&EMcuYuiWvl3FDux2%oabTAkr17pKFY!fufa^9trQhvBfru&CVUSI!tWm3Z0cr
### Turn on/off caching of auth information. By turning on this switch, the update of auth information would have a 15 seconds delay.
nacos.core.auth.caching.enabled=true
nacos.core.auth.enable.userAgentAuthWhite=false
nacos.core.auth.server.identity.key=nacos
nacos.core.auth.server.identity.value=nacos
## spring security config
### turn off security
nacos.security.ignore.urls=${NACOS_SECURITY_IGNORE_URLS:/,/error,/**/*.css,/**/*.js,/**/*.html,/**/*.map,/**/*.svg,/**/*.png,/**/*.ico,/console-fe/public/**,/v1/auth/**,/v1/console/health/**,/actuator/**,/v1/console/server/**}
# metrics for elastic search
management.metrics.export.elastic.enabled=false
management.metrics.export.influx.enabled=false
nacos.naming.distro.taskDispatchThreadCount=10
nacos.naming.distro.taskDispatchPeriod=200
nacos.naming.distro.batchSyncKeyCount=1000
nacos.naming.distro.initDataRatio=0.9
nacos.naming.distro.syncRetryDelay=5000
nacos.naming.data.warmup=true
```

```shell
docker run -d --name nacos \
-p 8848:8848 -p 9848:9848 \
--privileged=true \
-e JVM_XMS=128m \
-e JVM_XMX=128m \
-e MODE=standalone \
-e NACOS_SERVER_IP=127.0.0.1 \
-v /docker/nacos/logs:/home/nacos/logs \
-v /docker/nacos/conf:/home/nacos/conf \
--restart=always \
nacos/nacos-server
```

---

##### ddns-go*

自己搭建的支持IPv6的服务器可以部署ddns-go实现公网访问，不支持IPv6或不需要公网访问跳过

```shell
docker run -d --name ddns-go --restart=always --net=host -v /docker/ddns-go:/root jeessy/ddns-go
```

---

##### 结束

完成上述所有配置后的系统占用：

![image-20231107232315649](https://qk-antares.github.io/img/blog/deploy/44cde30d951ea8319f0c84623252c6f938279841.png)

---

#### 配置

##### 后端

将application-example.yml和bootstrap-example.yml重命名为application.yml和bootstrap.yml，并将其中的配置项修改为自己的。

###### Nacos

![img](https://qk-antares.github.io/img/blog/deploy/29b4fef2badf82ed03cba936b5ca3b3738279841.png)

![img](https://qk-antares.github.io/img/blog/deploy/1518c917be0c99d120a695ad704f793d38279841.png)

- 需要将bootstrap.yml中的nacos地址、username和password设置成自己的（本地nacos似乎不用设置username和password，默认是nacos/nacos）。
- bootstrap-dev.yml中namespace设置成自己创建的命名空间（一般是dev一个命名空间，test一个命名空间，prod一个，以此类推）

###### MySQL & Redis & RabbitMQ

![img](https://qk-antares.github.io/img/blog/deploy/a4e82278a2168d7796cfdb2d8b6a1ff038279841.png)

略，主要是配置地址，用户名，密码

---

###### 邮箱

member微服务的一个配置项

![image-20230918142819342](https://qk-antares.github.io/img/blog/deploy/79318da9fcadea84a11ca4d8ec0639d738279841.png)

[SpringBoot实现QQ邮箱发送功能_springboot qq邮箱_☆夜幕星河℡的博客-CSDN博客](https://blog.csdn.net/qq_45660133/article/details/123499839)

参考这篇博客，需要到自己的QQ邮箱中操作一下，主要是需要获取授权码

---

###### Cookie作用域

member微服务，不起眼但是重要的一个配置

![image-20230918143022120](https://qk-antares.github.io/img/blog/deploy/fe0d1f8eb0e96a1d6012740424f8bce038279841.png)

---

###### 第三方服务

![image-20230918143106688](https://qk-antares.github.io/img/blog/deploy/3852c4fc22592bede21ea9e13d780ea938279841.png)

sms是使用的榛子云短信，oss使用的是七牛云，oauth是gitee。可以搜索相关的博客看这些配置项如何配置，都需要你们到自己的账号上操作一下。如果你只想把项目跑起来，可以先不配置

---

##### 前端

所有需要配置的量我都抽取到utils下的constants里的，你主要是需要修改BACKEND_DOMAIN和CLIENT_ID（不是必要的）

![image-20231108152104354](https://qk-antares.github.io/img/blog/deploy/3d87612a520c898379e028c0b83d932338279841.png)

---

##### Nginx

将nginx文件夹下的配置拷贝到对应的位置（antares.conf除外，因为它是站点的配置）

![img](https://qk-antares.github.io/img/blog/deploy/687474703a2f2f696d6167652e616e74617265732e636f6f6c2f506963476f2f50726f6a6563742f426c6f672f4465706c6f792f363031303937313236316135373165653530313735336161363735333932666633383237393834312e706e67)

注意将lualib中的redisUtils-example重命名为redisUtils，并将其中的redis配置成自己的

![image-20231108152406131](https://qk-antares.github.io/img/blog/deploy/58f3d7e88e7c7a3b5dfcfb2817bdc73838279841.png)

创建站点

![image-20230918144003433](https://qk-antares.github.io/img/blog/deploy/06efadb19233ba057dae2b766699925638279841.png)

配置站点的配置文件，具体内容就是项目nginx/conf下antares.conf的内容

---

#### 数据初始化

##### MySQL

创建一个antares_blog数据库，执行sql脚本导入数据

##### ElasticSearch

1. 进入Kibana创建索引

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

   ```
   PUT user_v1
   {
     "aliases": {
       "user": {}
     },
     "mappings": {
       "properties": {
         "username": {
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
         "signature": {
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
         "tags": {
           "type": "keyword"
         }
       }
     }
   }
   ```

2. 执行antares-search下test/job/FullSyncPostToEs中的两个函数，将MySQL中的用户和文章信息全量同步至ES

---

##### Redis

将redis/dump.rdb拷贝到/www/server/redis下，然后重启redis

---

### 附录

#### Docker

当然，你想要Docker部署MySQL、Redis、RabbitMQ等也是可以的，这里就记录下（方便我查看哈哈~）

##### MySQL

```shell
mkdir -p /docker/mysql/data
mkdir -p /docker/mysql/conf

docker run -d --name mysql \
-e MYSQL_ROOT_PASSWORD="123456" \
-e TZ=Asia/Shanghai \
-p 3306:3306 \
mysql:8.0

docker cp mysql:/etc/mysql/my.cnf /docker/mysql/conf/my.cnf
```

```properties
#######-------my.cnf-----------#######
[mysqld]
pid-file        = /var/run/mysqld/mysqld.pid
socket          = /var/run/mysqld/mysqld.sock
datadir         = /var/lib/mysql
secure-file-priv= NULL
skip-host-cache
skip-name-resolve
default-time_zone='+8:00'
character_set_server=utf8mb4
collation-server=utf8mb4_unicode_ci
skip_ssl

server_id=1
log_bin=mysql-bin
binlog_format=ROW

[client]
default-character-set = utf8mb4

# Custom config should go here
!includedir /etc/mysql/conf.d/
```

```shell
docker stop mysql
docker rm mysql

# 运行mysql容器
docker run -d --name mysql \
-e MYSQL_ROOT_PASSWORD="123456" \
-e TZ=Asia/Shanghai \
-p 3306:3306 \
-v /docker/mysql/conf/my.cnf:/etc/mysql/my.cnf \
-v /docker/mysql/data:/var/lib/mysql \
--restart=always \
mysql:8.0
```

---

##### RabbitMQ

```shell
docker run -d --name rabbitmq \
-p 5671:5671 -p 5672:5672 \
-p 4369:4369 \
-p 25672:25672 \
-p 15671:15671 -p 15672:15672 \
--restart=always \
rabbitmq:management
```

---

##### Redis

###### 单节点

```
mkdir -p /docker/redis/redis-master/data
mkdir -p /docker/redis/redis-master/conf
cd /docker/redis/redis-master/conf
# 拷贝配置文件，这里一定要拷贝
# 拷贝rdb文件

docker run -d --name redis \
--privileged=true \
-p 6379:6379 \
-v /docker/redis/redis-master/conf/redis.conf:/etc/redis/redis.conf \
-v /docker/redis/redis-master/data:/data \
--restart=always \
redis:6.2.8 redis-server /etc/redis/redis.conf \
--appendonly no
```

###### 主从集群

注意：从节点的配置文件不变

首先创建网络

```shell
docker network create --driver bridge --subnet 172.24.0.0/16 --gateway 172.24.0.1 redis_net
```

redis集群的docker-compose文件：

```yml
version: '3.7'
networks:
  net:
    ipam:
      driver: default
      config:
        - subnet: "172.24.0.0/24"
          gateway: 172.24.0.1
services:
  master:
    image: redis:6.2.8
    container_name: redis-master
    restart: always
    command: redis-server /etc/redis/conf/redis.conf
    ports:
      - 6379:6379
    volumes:
      - /docker/redis/redis-master/conf:/etc/redis/conf
      - /docker/redis/redis-master/data:/data
    networks:
      net:
        ipv4_address: 172.24.0.2
  slave1:
    image: redis:6.2.8
    container_name: redis-slave1
    restart: always
    command: redis-server /etc/redis/conf/redis.conf --slaveof redis-master 6379
    ports:
      - 6380:6379
    volumes:
      - /docker/redis/redis-slave1/conf:/etc/redis/conf
      - /docker/redis/redis-slave1/data:/data
    networks:
      net:
        ipv4_address: 172.24.0.3
  slave2:
    image: redis:6.2.8
    container_name: redis-slave2
    restart: always
    command: redis-server /etc/redis/conf/redis.conf --slaveof redis-master 6379
    ports:
      - 6381:6379
    volumes:
      - /docker/redis/redis-slave2/conf:/etc/redis/conf
      - /docker/redis/redis-slave2/data:/data
    networks:
      net:
        ipv4_address: 172.24.0.4
```

```shell
# 构建集群
docker-compose -f redis.yml up -d
```

###### 哨兵集群

首先修改配置文件所在目录的权限

```
chmod -R 777 redis
```

创建sentinel的配置文件

```ini
port 26379
sentinel auth-pass mymaster password
sentinel monitor mymaster 172.24.0.2 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
dir "/tmp"
```

sentinel集群的配置文件：

```yml
version: '3.7'
networks:
  default:
    name: redis_net
services:
  sentinel1:
    image: redis:6.2.8
    container_name: redis-sentinel1
    restart: always
    ports:
      - 26379:26379
    command: redis-sentinel /etc/redis/conf/sentinel.conf
    volumes:
      - /docker/redis/redis-master/conf:/etc/redis/conf
  sentinel2:
    image: redis:6.2.8
    container_name: redis-sentinel2
    restart: always
    ports:
    - 26380:26379
    command: redis-sentinel /etc/redis/conf/sentinel.conf
    volumes:
      - /docker/redis/redis-slave1/conf:/etc/redis/conf
  sentinel3:
    image: redis:6.2.8
    container_name: redis-sentinel3
    ports:
      - 26381:26379
    command: redis-sentinel /etc/redis/conf/sentinel.conf
    volumes:
      - /docker/redis/redis-slave2/conf:/etc/redis/conf
```

```shell
# 构建集群
docker-compose -f sentinel.yml up -d
```

注：

同时停止redis容器和sentinel容器的指令：

```shell
docker stop $(docker ps -a | grep "redis" | awk '{print $1}')
```

---

#### 图床

Minio可以作为私人图床，使用阿里云或七牛云的OSS都有一定限制，要么是收费的，要么需要有已备案的域名，下面是IPv6+DDNS服务器使用Minio搭建私人图床的流程：

首先是Docker安装Minio，见[依赖部署Minio](#minio)。创建一个名为blog的桶，并设置桶的权限为public（这样所有的人都可以访问你的图片，前提是他的网络环境支持IPv6）

![image-20231108000146467](https://qk-antares.github.io/img/blog/deploy/883965e49256f2d8f49c9df96d84282b38279841.png)

然后对PicGO进行配置，搜索并安装minio插件，配置项如下

![image-20231108103526421](https://qk-antares.github.io/img/blog/deploy/71a7c09cb196c336d59b12911d4ab12538279841.png)

这里的debian.zqk.asia就是你在ddns-go中配置的域名，也就是你IPv6服务器的域名，为了区分不同的功能，你也可以在ddns-go中多添加一个minio.zqk.asia的解析，然后这里填minio.zqk.asia也行。

最后对Typora进行配置，选择粘贴图片上进行上传