# shortLink(短链跳转项目)


## 项目简介          
一句话：把长链转成 6 位短码，支持高并发跳转
## 技术栈            
Spring Boot 4 / MySQL / Redis / 线程池异步
## 架构图           
```mermaid
flowchart LR
    C[客户端] -->|"GET /{shortCode}"| S[Service]
    S -->|"先查缓存"| RD[(Redis)]
    RD --> HIT{缓存命中?}
    HIT -->|"是"| R[组装响应]
    HIT -->|"否 · 回源"| DB[(MySQL)]
    DB -->|"回填 · TTL 7 天"| RD
    DB --> FOUND{查到映射?}
    FOUND -->|"是"| R
    FOUND -->|"否"| E404[404]
    R -->|"302 + Location"| C
    E404 --> C
    R -.->|"投递日志 · 不阻塞主链路"| P[线程池]
    P -.->|"异步写"| LOG[(t_access_log)]
     
```

```mermaid
flowchart LR
    C[客户端] -->|"写操作 POST / DELETE"| IK[拦截器 · 校验 X-API-Key]
    IK --> S[Service]
    S -->|"创建 · 取号"| IDG[IdGenerator 号段池]
    IDG -->|"id → 短码"| S
    S -->|"存映射"| DB[(MySQL)]
    S -->|"软删除 · status=0"| DB
    S -->|"软删除时 · 删缓存"| RD[(Redis)]
    S -->|"启用 · status=1"| DB
```
## 核心设计
  - 短码生成   
    id × K mod 62^6  
    id由发号器通过对比Redis最大id号后发送，K是混淆乘数，防止有人恶意攻击，62^6中62是短链使用的字符种类数，是0~9数字加上大小写字母，6是短链长度。

  - 发号器  
    号段池，使用Redis记录最大id值，每次取号用当前号current和max对比，如果小于则取号，如果大于等于则取1000个号，max += 1000。
  - 跳转使用响应码302 而非 301    
    短链后端需要统计跳转访问情况，301响应浏览器会缓存跳转数据下一次访问直接跳，所以使用302临时跳转。

  - Cache-Aside     
    读先缓存、写后删缓存，TTL 7 天。

  - 异步统计        
    跳转不等写库，单次访问P99 从 86ms 降到 71ms
## 接口文档

[接口文档](shortLinkAPI.md)


## 本地运行 
依赖：JDK 17 / MySQL 8 / Redis
1. mvn clean package
2. 启动 Redis 与 MySQL，建表（见 sql/schema.sql）
3. java -jar target/shortlink-0.0.1-SNAPSHOT.jar

    
## 压测数据  

跳转主链路采用异步写日志。压测（100 并发 / 60s）显示：P99 从 86ms 降至 71ms，长尾（Max）从 860ms 降至 230ms。












