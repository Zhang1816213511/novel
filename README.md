# novel
本地写作智能助手 使用ClaudeCode生成

# novel
本地写作智能助手

## 技术栈

### 后端
- Spring Boot 3.2.5
- MyBatis-Plus 3.5.6
- SQLite (本地数据库)
- Caffeine (本地缓存)
- Knife4j (API 文档)

### 前端
- Vue 3.4 + Vite 5
- Vue Router + Pinia
- Axios

## 快速开始

### 环境要求
- Java 17+
- Node.js 18+ (可选，仅前端开发需要)

### 方式一：仅后端运行（推荐）

```bash
# 直接启动后端，访问 API 和文档
./mvnw spring-boot:run
# 或 Windows
mvnw.cmd spring-boot:run
```

访问：
- API 文档：http://localhost:8080/doc.html
- 前端页面：需要先打包（见下方）

### 方式二：前后端完整运行

```bash
# 1. 打包前端
./build-frontend.sh
# 或 Windows
build-frontend.bat

# 2. 构建并运行
./mvnw clean package
java -jar target/novel-1.0.0.jar
```

访问：http://localhost:8080

### 开发模式（前后端分离）

```bash
# 终端 1: 启动后端
./mvnw spring-boot:run

# 终端 2: 启动前端开发服务器
./dev-frontend.bat
# 或 Linux/Mac
./build-frontend.sh && npm run dev
```

## 项目结构

```
novel/
├── src/main/
│   ├── java/com/novel/
│   │   ├── common/           # 通用组件（Result、异常处理）
│   │   ├── config/           # 配置类（数据源、缓存、Web）
│   │   ├── controller/       # REST API 控制器
│   │   ├── service/          # 业务逻辑层
│   │   ├── mapper/           # MyBatis-Plus Mapper
│   │   └── entity/           # 实体类
│   ├── resources/
│   │   └── application.yml   # 应用配置
│   └── webapp/               # Vue 前端源码
├── data/                     # SQLite 数据库文件
├── build-frontend.bat        # 前端打包脚本 (Windows)
├── build-frontend.sh         # 前端打包脚本 (Linux/Mac)
└── dev-frontend.bat          # 前端开发服务器脚本
```

## 功能特性

- 本地存储：所有数据保存在 SQLite 数据库，无需外部数据库服务
- 缓存加速：Caffeine 缓存热点数据，查询更高效
- 前后端分离：Vue 3 前端，打包后由 Spring Boot 静态托管
- API 文档：Knife4j 接口文档，方便调试


## 许可证（双许可证）

本项目采用 **双许可证** 模式：

### 1. 开源许可证（默认）：GPL-3.0
- ✅ 适用于：个人学习、开源项目、非商业用途
- ✅ 允许：使用、修改、分发
- ❌ 要求：衍生项目必须同样以 GPL-3.0 开源
- ℹ️ 详见 [LICENSE](./LICENSE) 文件

### 2. 商业许可证（需购买）
- ✅ 适用于：企业内部使用、SaaS 服务、闭源商业产品
- ✅ 允许：闭源部署、修改后不公开代码、用于盈利业务
- 💰 获取方式：发送邮件至 1816213511@qq.com 询价

**任何不符合 GPL-3.0 条款的使用场景（如闭源商用），均需购买商业许可证。**