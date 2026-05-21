# CLAUDE.md

此文件为 Claude Code 在仓库中工作提供指导。

## 项目目标

**本地写作智能助手** — 基于大语言模型（Ollama / OpenAI 兼容 API）辅助创作小说和文章。核心流程：用户提供主题或已有内容，AI 生成续写/改写/扩写，支持人工编辑修改。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.2.5 + Java 17 |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库 | SQLite（`data/novel.db`，首次启动自动创建） |
| 缓存 | Caffeine（Spring `@Cacheable`，60min TTL） |
| AI 模型层 | AgentScope（ReActAgent + SequentialPipeline，支持 Ollama 本地 + OpenAI 兼容 API） |
| API 文档 | Knife4j 4.5（`/doc.html`） |
| 前端 | Vue 3.4 + Vite 5 + Hash 路由 |
| 打包方式 | `java -jar` 单文件运行，Vue 构建产物输出到 `src/main/resources/static/` |

## 项目结构

```
novel/
├── NovelApplication.java          启动类（@EnableCaching）
├── config/                        数据源、MyBatis-Plus、Web、Knife4j、AgentScope 配置
├── common/                        Result<T> 统一返回、全局异常处理
├── controller/                    REST API
├── service/                       业务逻辑 + AgentScope 双智能体管道 + Caffeine 缓存
├── mapper/                        MyBatis-Plus Mapper
├── entity/                        实体（Novel, ModelConfig）
├── dto/                           请求/响应 DTO
├── entity/                        实体（Novel, Chapter, ModelConfig）
└── web/                           Vue 前端
    ├── src/views/Home.vue         首页
    ├── src/views/NovelList.vue    作品列表（卡片网格）
    ├── src/views/NovelDetail.vue  编辑器（分步生成 + 章节管理，自动保存）
    └── src/views/ModelList.vue    模型配置 CRUD（弹窗表单）
```

## 路由

| 路径 | 页面 | 说明 |
|---|---|---|
| `/#/` | 首页 | 项目介绍、功能展示 |
| `/#/novel` | 作品列表 | 卡片式展示所有作品 |
| `/#/novel/:id` | 编辑器 | 分步生成（简介→大纲→章节） + 章节管理 |
| `/#/models` | 模型管理 | LLM 配置（Ollama/OpenAI） |

## 关键模式

### 生成工作流（分步）

用户创建作品后，按以下步骤依次生成，每步可手动编辑：

1. **生成简介** — 根据标题生成，点击「生成简介」→ `POST /api/generate/{id}/synopsis`
2. **生成大纲** — 根据简介生成，点击「生成大纲」→ `POST /api/generate/{id}/outline`
3. **创建章节** — 点击「添加章节」→ `POST /api/chapters/novel/{id}`
4. **生成每章梗概** — 基于大纲 + 前几章梗概 → `POST /api/generate/{id}/chapter-summary/{chId}`
5. **生成每章正文** — 基于本章梗概 → `POST /api/generate/{id}/chapter-content/{chId}`

每个步骤同步调用 LLM，前端显示 loading，支持重生成。

### 数据流
- 作品信息（标题/简介/大纲）→ `PUT /api/novels/:id` → `novel_entity` 表
- 章节信息（标题/梗概/正文）→ `PUT /api/chapters/:id` → `chapter` 表
- 自动保存：800ms 防抖，离开页面时触发一次保存
- AgentScope 初始化时从 DB 加载已启用的模型配置到内存缓存

### 模型配置
- 支持 `ollama` 和 `openai` 两种 provider
- 表 `model_config` 存储名称、提供商、模型名、API 地址/密钥、options JSON、启用状态
- `AgentScopeConfig` 在 `@PostConstruct` 中加载已启用的模型到 `Map<String, Map<String, String>>`
- 后续的 AI 生成功能通过 `AgentScopeConfig.getModel(name)` 获取模型配置

### SQLite 注意事项
- JDBC 不支持 `getGeneratedKeys()` → 实体使用 `@TableId(type = IdType.INPUT)` + insert 后调用 `last_insert_rowid()` 回填 ID
- `chapter` 表使用 `IdType.AUTO`（SQLite AUTOINCREMENT 正常工作）
- MyBatis-Plus 分页拦截器使用 `DbType.SQLITE`
- 表结构通过 `DataSourceConfig` 中的 DDL 自动初始化（`CREATE TABLE IF NOT EXISTS`）

### 数据库表
- `novel_entity`: `id, title, content, synopsis, outline, create_time, update_time`
- `chapter`: `id, novel_id, chapter_number, title, summary, content, create_time, update_time`
- `model_config`: `id, name, provider, base_url, model_name, api_key, options, enabled, create_time, update_time`

### 缓存
- Spring `@Cacheable` + `@EnableCaching`
- Caffeine 本地缓存 `Cache<Long, Novel>`，60 分钟过期，仅缓存单篇查询
- 更新/删除时 `cache.invalidate(id)`

### LLM 生成机制（AgentScope 双智能体管道）
- `NovelGenerationService` 提供 4 个同步生成方法（简介/大纲/章节梗概/章节正文），委托给 `AgentPipelineService`
- `AgentPipelineService` 使用 AgentScope 的 `SequentialPipeline` 串联两个 `ReActAgent`：
  1. **Writer 智能体** — 根据用户提示生成内容（创作助手角色）
  2. **Reviewer 智能体** — 审查内容质量，合格时在第一行输出「合格」并附原文，否则给出修改建议
- 最多重试 3 次，每次将修改意见附加到提示中重新生成，达到最大次数后直接输出结果
- `AgentScopeConfig.buildModel(name)` 根据 DB 配置构建 `OpenAIChatModel` 或 `OllamaChatModel`
- 章节梗概生成时自动传入前几章的梗概作为上下文，确保情节连贯
- 章节正文生成时自动传入上一章正文末尾 500 字作为衔接参考

### API 响应格式
所有接口返回 `Result<T>`：`{ code, message, data }`。成功 `Result.success(data)`，失败 `Result.failed(ResultCode.xxx, msg)`。

### 前端规范
- Hash 路由（`createWebHashHistory`），Vue 构建产物输出到 `src/main/resources/static/`
- Sidebar 全局导航（App.vue），Nav 不放在各页面里
- NovelDetail 使用三个可折叠区域（简介/大纲/章节管理），每个区域有独立 textarea 和生成按钮
- 章节管理支持添加/删除章节，每章可独立编辑标题/梗概/正文
- 字数统计基于所有章节正文总长度
- 800ms 防抖分别保存作品信息和章节信息，组件卸载时强制保存一次
- CSS 变量统一管理色彩体系（`--color-primary: #42b983`）

### 编码约定
- Entity/DTO 使用 Lombok `@Data`，日志 `@Slf4j`，DI 使用 `@RequiredArgsConstructor` + `final` 字段
- MyBatis-Plus 实体加 `@TableName` + `@TableId`
- 全局异常由 `GlobalExceptionHandler` 统一处理
