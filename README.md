# 操作手册（training24-tomcat）

## 一、环境准备与导入
- JDK：建议安装 `JavaSE-1.8`（确保系统与 Eclipse 使用的是 1.8）
- IDE：Eclipse for Enterprise Java（含 WTP），或 IDEA 也可运行
- 服务器：`Apache Tomcat v8.5`
- 导入：Eclipse > File > Import > Existing Projects into Workspace，选择 `training24-tomcat`
- Facets：Project Properties > Project Facets 勾选 `Dynamic Web Module 3.1`、`Java 1.8`
- 绑定运行时：Project Properties > Targeted Runtimes 勾选 `Apache Tomcat v8.5`

## 二、目录结构速览
- 前端：`WebRoot/*.html`、`WebRoot/assets/*`（静态资源）
- 后端：`src/*.java`（Servlet 与工具类），编译输出到 `WebRoot/WEB-INF/classes`
- 库与导出：`WebRoot/WEB-INF/lib`（第三方库），`WebRoot/exports/`（CSV 导出目录）

## 三、启动与预览
- Eclipse 运行：右键项目 > Run As > Run on Server
- 主页地址：`http://localhost:8080/training24-tomcat/`
- 主要页面入口：
  - `index.html` 登录/注册
  - `students.html` 学生端（课表、成绩、推荐、CSV 导出）
  - `courses.html` 课程管理（管理员）
  - `enroll.html` 选课与退课（学生）
  - `admin.html` 统计与日志（管理员）

## 四、构建与发布（可选的手动方式）
- 清理旧编译：删除 `WebRoot/WEB-INF/classes/*`
- 编译到 Java 8（示例，Windows 路径需按实际调整）：
  - 准备 classpath：包含 Tomcat 的 `servlet-api.jar` 与项目自带库，例如：
  - "%TOMCAT_HOME%\\lib\\servlet-api.jar";"WebRoot\\WEB-INF\\lib\\json-org-build20180908.jar"`
  - 编译命令（在 `src` 目录上执行）：
    - `javac --release 8 -encoding UTF-8 -cp <上面classpath> -d WebRoot\\WEB-INF\\classes src\\**\\*.java`
- 同步前端静态文件到 Tomcat 部署目录（如需）：
  - `robocopy WebRoot %TOMCAT_HOME%\\webapps\\training24-tomcat /E`
- 重启 Tomcat：在 IDE 中或使用 `bin\\startup.bat`/`shutdown.bat`

## 五、接口速查与示例
- 统一风格：所有接口返回 JSON，成功：`{"error":0,"reason":"ok","data":...}`，失败：`{"error":非0,"reason":"错误原因"}`
- 常用接口（统一使用 `POST` 且请求体为 JSON）：
  - 用户：`/api/user`，`action=register|login|logout|reset`
  - 课程：`/api/course`，`action=create|update|delete|list|filter`
  - 选课：`/api/enroll`，`action=enroll|drop|mylist`
  - 学生：`/api/student`，`action=calendar|grades|recommend|grades_export`
  - 管理员：`/api/admin`，`action=stats|logs_query`
- curl 示例（登录）：
  - `curl -X POST -H "Content-Type: application/json" -d "{\"action\":\"login\",\"username\":\"student\",\"password\":\"123456\"}" "http://localhost:8080/training24-tomcat/api/user"`

## 六、常见问题与排错
- 404 页面/接口找不到：
  - 检查上下文路径是否为 `training24-tomcat`，Servlet 是否正确映射
- 500 服务器错误：
  - 查看 Eclipse Console 或 `logs_query` 输出；后端抛出的 `lwWebException` 会有错误码与信息
- UnsupportedClassVersionError（类版本不匹配）：
  - 现象：报类版本 61.0（Java 17）而 Tomcat 仅支持 52.0（Java 8）
  - 处理：用 `javac --release 8` 重新编译，并在 classpath 加入 `servlet-api.jar`，将输出写回 `WEB-INF/classes`
- `HttpSession`/`HttpServlet` 找不到：
  - 原因：编译时缺少 Servlet API；确保 classpath 包含 `Tomcat/lib/servlet-api.jar`
- CSV 未生成或打不开：
  - 检查 `WebRoot/exports` 目录存在与写权限；文件名示例 `grades-YYYYMMDD-HHMM.csv`
- 前端显示错乱（例如 `enroll.html` “已选”列）：
  - 学生应显示“是/否”，管理员显示总人数；确保角色检测完成后再渲染（`detectAdmin` 回调后调用 `renderEnrollPage()`）

## 七、演示与自测清单
- 演示账户：管理员 `admin/admin`，学生请自注册
- 推荐演示顺序：注册/登录 → 课程管理 → 选课/退课/候补转正 → 学生视图与导出 → 管理员统计与日志
- 自测要点：
  - 接口均返回 JSON 且结构一致（`ok` 字段）
  - 选课冲突与候补可复现（课程容量设为 1/2）
  - CSV 导出文件存在且内容含表头与时间戳
  - 管理员统计显示用户/课程/选课总数，日志可查询
