# Export Jar Plugin 重构计划 (2026-1)

## 概述

基于 IntelliJ Platform Gradle Plugin 2.16.0 升级后的技术栈，本计划涵盖代码质量提升、架构优化和技术债务清理。

---

## 优先级排序

| 优先级 | 类别 | 说明 |
|--------|------|------|
| P0 | 反射代码修复 | 插件验证可能失败的反射使用 |
| P0 | API 废弃警告 | 必须修复的废弃/内部 API 使用 |
| P1 | Workaround 消除 | UI/Dialog 的 workaround 方案重构 |
| P1 | 核心功能重构 | 影响稳定性和可维护性的改进 |
| P2 | 自行实现优化 | API 替代与代码重复消除 |
| P3 | 代码清理 | TODO 处理 |
| P4 | Kotlin 迁移 | 语言迁移（长期目标） |

---

## P0: 反射代码与 API 废弃修复

### 0.1 修复废弃 API 警告

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/ui/SettingDialog.java:245`

**问题**: 使用废弃的 `FileChooserDescriptorFactory.createSingleFileDescriptor()`

**解决方案**:
```java
// 替换为推荐的方式
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.roots.ProjectFileIndex;

public void onSelectJarFileButton(ActionEvent event) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(
        true, false, false, false, false, false
    );
    // 或使用 FileChooserDescriptorFactory.createSingleFolderDescriptor() + 过滤
}
```

### 0.2 修复内部 API 使用警告

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/ui/FileListDialog.java`

**问题**: 重写了内部方法 `AbstractSelectFilesDialog.createCenterPanel()`

**解决方案**:
- 检查是否有公开 API 可以替代
- 如无替代方案，添加 `@SuppressWarnings("InternalApi")` 并记录原因
- 考虑重构为使用标准 DialogWrapper 模式

### 0.3 反射代码分析与修复

#### 0.3.1 Opcodes 版本获取反射

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/utils/CommonUtils.java:51-60`

**问题代码**:
```java
private static int versionOpcodes;

static {
    try {
        final Field apiVersion = Opcodes.class.getField("API_VERSION");
        versionOpcodes = (int) apiVersion.get(null);
    } catch (Exception e) {
        versionOpcodes = Opcodes.API_VERSION;
    }
}
```

**问题分析**:
- 通过反射获取 `Opcodes.API_VERSION` 字段值
- 这是为了获取 ASM 的 API 版本号用于 `ClassVisitor`
- 反射在插件验证时可能被拒绝

**解决方案**:
```java
// 方案1: 直接使用常量（推荐）
// since-build 已升至 251 (2025.1)，ASM 版本稳定
private static final int VERSION_OPCODES = Opcodes.ASM9; // 或最新版本

// 方案2: 如果需要动态检测
private static final int VERSION_OPCODES = detectVersionOpcodes();

private static int detectVersionOpcodes() {
    // 只在初始化时检测一次，不再每次反射
    try {
        Field field = Opcodes.class.getField("ASM_VERSION");
        Object val = field.get(null);
        return val instanceof Integer ? (Integer) val : Opcodes.ASM9;
    } catch (Exception e) {
        return Opcodes.ASM9; // 默认值
    }
}
```

**优先级**: P0 - 插件验证可能失败

#### 0.3.2 ChangesTree 字段反射替换

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/ui/FileListDialog.java:65-90`

**问题代码**:
```java
private void replaceFilesTree(...) {
    try {
        final Field treeField = getTreeField(getFileList());
        if (treeField != null) {
            final var newTree = new FileListTree(project, selectableFiles, deletableFiles, files);
            treeField.setAccessible(true);
            treeField.set(this, newTree);
        }
    } catch (IllegalAccessException e) { ... }
}

private Field getTreeField(ChangesTree orgFilesTree) throws IllegalAccessException {
    final List<Field> fields = ReflectionUtil.collectFields(SelectFilesDialog.class);
    for (Field field : fields) {
        field.setAccessible(true);
        if (field.get(this) == orgFilesTree) {
            return field;
        }
    }
    return null;
}
```

**问题分析**:
- 通过反射查找 `SelectFilesDialog` 中与 `ChangesTree` 对象相同的字段
- 设置 `setAccessible(true)` 访问私有字段
- 这种方式脆弱：字段名/类型变化会直接失败

**解决方案**:
```java
// 方案1: 使用 TreeModelBuilder 直接构建自定义树（推荐）
// 在 FileListTree.buildTreeModel() 中已有部分实现
// 需要扩展为完全脱离 SelectFilesDialog

// 方案2: 组合而非继承
// 不继承 SelectFilesDialog，而是创建独立的 FileListDialog
// 直接使用 ChangesTree 和 TreeModelBuilder

// 方案3: 提交 Issue 请求官方 API
// 如果需要替换树的实际场景有公开 API 可用
```

**优先级**: P0 - 高风险，插件验证可能失败

#### 0.3.3 反射代码汇总

| 位置 | 类型 | 风险 | 优先级 |
|------|------|------|--------|
| `CommonUtils.java:55` | Field.get() | 中 | P0 |
| `FileListDialog.java:71,84` | Field.setAccessible() | 高 | P0 |

---

## P0-B: Workaround 方案分析与修复

### 0.4 SelectFilesDialog 继承 workaround

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/ui/FileListDialog.java:38`

**问题**:
```java
public class FileListDialog extends SelectFilesDialog {
    // 需要替换内部的 ChangesTree，但父类没有公开 API
    // 只能通过反射替换私有字段
}
```

**问题分析**:
- `SelectFilesDialog` 是 IntelliJ 内部类，API 不完整
- 需要自定义 `FileListTree` 继承 `VirtualFileList`
- 但无法通过构造器注入，只能反射替换

**解决方案**:
```java
// 方案1: 完全脱离继承，使用组合
public class FileListDialog extends DialogWrapper {
    private final ChangesTree myTree;  // 直接持有
    private final FileListTreeHandler handler;
    
    public FileListDialog(...) {
        super(project);
        // 直接创建自定义树，不依赖反射
        myTree = new FileListTree(project, selectableFiles, deletableFiles, files);
        // ... 初始化逻辑
    }
}

// 方案2: 如果 SelectFilesDialog 有 protected 方法可用
// 重写 createTree() 或类似方法，插入自定义树
```

**优先级**: P1 - 架构问题，但当前可用

### 0.5 版本兼容 workaround

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/utils/MessagesUtils.java:174-185`

**问题代码**:
```java
/**
 * delegate get message view method to platform method for compatible with old version.
 * use com.intellij.ui.content.MessageView#getInstance(com.intellij.openapi.project.Project) when this plugin support mini version update to greater than 222.2680.4.
 */
public static MessageView getMessageView(Project project){
    return project.getService(MessageView.class);
}

/**
 * delegate get ContentFactory method to platform method for compatible with old version.
 * use com.intellij.ui.content.ContentFactory#getInstance() when this plugin support mini version update to greater than 222.2680.4.
 */
public static ContentFactory getContentFactory() {
    return ApplicationManager.getApplication().getService(ContentFactory.class);
}
```

**问题分析**:
- `since-build: 251` 已远超 222.2680.4
- 这些兼容方法可以移除，直接使用新 API
- 代码中有大量注释说明兼容版本

**解决方案**:
```java
// since-build 已升至 251，直接简化
public static MessageView getMessageView(Project project) {
    return project.getService(MessageView.class);
}

// 删除旧版本兼容分支
```

**优先级**: P1 - 可以简化代码

### 0.6 buildTreeModel 双方法兼容

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/ui/FileListDialog.java:267-288`

**问题代码**:
```java
// from 2023.2 should use following line
//	final DefaultTreeModel defaultTreeModel = super.buildTreeModel(grouping, changes);
// for pass the IntelliJ Plugin Verifier
defaultTreeModel = TreeModelBuilder.buildFromVirtualFiles(myProject, grouping, changes);

// for compatible with before version 2022
protected @NotNull DefaultTreeModel buildTreeModel(@NotNull List<? extends VirtualFile> changes) {
    DefaultTreeModel defaultTreeModel;
    final ChangesGroupingPolicyFactory grouping = getGrouping();
    defaultTreeModel = TreeModelBuilder.buildFromVirtualFiles(myProject, grouping, changes);
    // ...
}
```

**问题分析**:
- 保留了两个 `buildTreeModel` 方法签名用于兼容
- 注释说明某些版本需要用不同的方法签名
- since-build 251 后不再需要这些兼容代码

**解决方案**:
```java
// since-build 251 后，删除旧版本兼容代码
// 只保留新版本的方法签名
protected @NotNull DefaultTreeModel buildTreeModel(
        @NotNull ChangesGroupingPolicyFactory grouping,
        @NotNull List<? extends VirtualFile> changes) {
    DefaultTreeModel defaultTreeModel = TreeModelBuilder.buildFromVirtualFiles(myProject, grouping, changes);
    expandDirWhenSetting(defaultTreeModel);
    collapseDirIfNeed(defaultTreeModel);
    return defaultTreeModel;
}
```

**优先级**: P1 - 代码清理

### 0.7 Workaround 方案汇总

| 位置 | 问题 | 原因 | 优先级 |
|------|------|------|--------|
| `FileListDialog.java:38` | 继承内部类 | 缺少公开 API | P1 |
| `MessagesUtils.java:174-185` | 版本兼容方法 | 旧版本兼容代码 | P1 |
| `FileListDialog.java:267-288` | 双方法签名 | 版本兼容 | P1 |
| `FileListDialog.java:281-288` | 旧版本方法保留 | 2022 以前兼容 | P1 |

---

## P0-C: 自行实现 vs API 分析

### 0.8 文件收集重复实现

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/utils/CommonUtils.java:77-90, 98-113`

**问题代码**:
```java
// 自定义递归实现
public static void collectExportFilesNest(Project project, Set<VirtualFile> collected, VirtualFile parentVf) {
    if (!parentVf.isDirectory() && isValidExport(project, parentVf)) {
        collected.add(parentVf);
    }
    final VirtualFile[] children = parentVf.getChildren();
    for (VirtualFile child : children) {
        if (child.isDirectory()) {
            collectExportFilesNest(project, collected, child);
        } else if (isValidExport(project, child)) {
            collected.add(child);
        }
    }
}

// VfsUtil 已有的实现（未使用）
public static Collection<VirtualFile> collectFilesNest(VirtualFile parentVf) {
    VfsUtil.visitChildrenRecursively(parentVf, new VirtualFileVisitor<Void>() {
        @Override
        public boolean visitFile(@NotNull VirtualFile file) {
            if (!file.isDirectory()) {
                files.add(file);
            }
            return true;
        }
    });
    return files;
}
```

**问题分析**:
- `collectExportFilesNest()` 是自定义递归实现
- `collectFilesNest()` 使用了 `VfsUtil.visitChildrenRecursively`
- 但前者没有使用后者，说明有特殊过滤逻辑
- 可以考虑合并或标准化

**解决方案**:
```java
// 方案1: 合并两个方法
public static Collection<VirtualFile> collectExportFiles(
        Project project, 
        VirtualFile parentVf,
        Predicate<VirtualFile> filter) {
    // 使用 VfsUtil 的高效实现 + 过滤器
}

// 方案2: 提取过滤器作为参数
public static void collectExportFilesNest(
        Project project, 
        Set<VirtualFile> collected, 
        VirtualFile parentVf,
        Predicate<VirtualFile> extraFilter) {
    // ...
}
```

**优先级**: P2 - 优化建议，非紧急

### 0.9 ASM 内部类解析

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/utils/CommonUtils.java:310-330`

**问题代码**:
```java
public static void findOffspringClassName(@NotNull Set<String> offspringClassNames, Path ancestorClassFile) {
    try {
        ClassReader reader = new ClassReader(Files.readAllBytes(ancestorClassFile));
        final String ancestorClassName = reader.getClassName();
        reader.accept(new ClassVisitor(versionOpcodes) {
            @Override
            public void visitInnerClass(String name, String outer, String inner, int access) {
                // 手动解析内部类信息
                final int indexSplash = name.lastIndexOf('/');
                String className = indexSplash.length() > 0 ? name.substring(indexSplash + 1) : name;
                // ...
            }
        }, new Attribute[0], ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}
```

**问题分析**:
- 使用 ASM 手动解析 `.class` 文件获取内部类信息
- 这是因为 IntelliJ PSI 不包含编译后的内部类信息
- 递归调用自身解析嵌套内部类

**是否可替换**: **否**
- PSI 只分析源文件，不包含编译后的内部类
- 必须通过字节码分析获取内部类关系
- ASM 是官方推荐的字节码分析库

**建议**: 保留但添加注释说明必要性

**优先级**: 不适用 - 这是正确的技术选型

### 0.10 TreeNode 遍历重复实现

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/ui/FileListTreeHandler.java`

**问题代码**:
```java
// 自定义树遍历
TreeUtil.treeNodeTraverser(root).preOrderDfsTraversal().forEach(n -> { ... });

// FileListTree 中也有类似代码
TreeUtil.treeNodeTraverser(root).postOrderDfsTraversal().toList();
```

**问题分析**:
- 使用 IntelliJ 提供的 `TreeUtil.treeNodeTraverser`
- 但在不同地方重复编写遍历逻辑
- 可以提取为统一的树遍历工具方法

**解决方案**:
```java
// 新建: ui/TreeNodeUtils.java
public class TreeNodeUtils {
    public static List<ChangesBrowserNode<?>> preOrderTraversal(TreeNode root) {
        return TreeUtil.treeNodeTraverser(root)
            .preOrderDfsTraversal()
            .map(n -> (ChangesBrowserNode<?>) n)
            .toList();
    }
    
    public static List<ChangesBrowserNode<?>> postOrderTraversal(TreeNode root) {
        return TreeUtil.treeNodeTraverser(root)
            .postOrderDfsTraversal()
            .map(n -> (ChangesBrowserNode<?>) n)
            .toList();
    }
}
```

**优先级**: P3 - 代码重复，非紧急

### 0.11 自行实现汇总

| 位置 | 实现内容 | 原因 | 可替换 | 优先级 |
|------|----------|------|--------|--------|
| `CommonUtils.java:77-90` | 文件递归收集 | 特殊过滤逻辑 | 部分 | P2 |
| `CommonUtils.java:310-330` | ASM 内部类解析 | PSI 不包含字节码 | 否 | 不适用 |
| `FileListTreeHandler.java` | 树遍历 | 重复实现 | 是 | P3 |
| `FileListTree.java:302-340` | 目录节点展开 | 复杂逻辑 | 否 | 不适用 |

---

## P0 综合: 快速修复清单

| 编号 | 问题 | 文件 | 优先级 | 工作量 |
|------|------|------|--------|--------|
| 0.1 | 废弃 API 警告 | SettingDialog.java:245 | P0 | 0.5h |
| 0.2 | 内部 API 重写 | FileListDialog.java | P0 | 1h |
| 0.3.1 | Opcodes 反射 | CommonUtils.java:55 | P0 | 0.5h |
| 0.3.2 | ChangesTree 反射 | FileListDialog.java:71,84 | P0 | 4h |
| 0.4 | Dialog 继承 workaround | FileListDialog.java:38 | P1 | 8h |
| 0.5 | 版本兼容代码 | MessagesUtils.java:174 | P1 | 1h |
| 0.6 | 双方法签名兼容 | FileListDialog.java:267 | P1 | 1h |

---

## P1: 核心功能重构

### 1.1 提取 FileCollector 职责

**当前问题**: `ExportPacker.pack()` 方法过长 (~60行)，职责不单一

**重构方案**:
```java
// 新建: src/main/java/org/yanhuang/plugins/intellij/exportjar/core/FileCollector.java
public class FileCollector {
    private final Project project;
    private final Set<ExportOptions> options;

    public JarFileSet collect(VirtualFile[] files) { }
    public JarFileSet addDirectoryEntries(JarFileSet jarSet) { }
}
```

**涉及的代码迁移**:
- `ExportPacker.collectExportVirtualFile()` → `FileCollector`
- `ExportPacker.addDirectoryEntries()` → `FileCollector`
- `ExportPacker.collectExportFile()` → `FileCollector`
- `CommonUtils.collectExportFilesNest()` → 保留但考虑合并

### 1.2 提取 JarWriter 职责

**当前问题**: `CommonUtils.createNewJar()` 混合了 JAR 创建和日志记录

**重构方案**:
```java
// 新建: src/main/java/org/yanhuang/plugins/intellij/exportjar/core/JarWriter.java
public class JarWriter {
    public void write(Path jarFile, JarFileSet fileSet) { }
    private void writeEntry(JarOutputStream jos, Path file, String entryName) { }
    private JarEntry createDirectoryEntry(String path) { }
}
```

### 1.3 线程模型统一

**当前问题**: 代码中存在多种线程执行模式混用

**问题代码**:
- `CommonUtils.runInBgtWithReadLockAndWait()` - 混合 EDT/后台检测
- `CommonUtils.backgroundRunWithoutLock()` - 后台任务包装
- `ExportPacker.finished()` - 编译回调线程
- `SettingDialog.onOK()` - 直接后台运行

**重构方案**:
```java
// 新建: src/main/java/org/yanhuang/plugins/intellij/exportjar/core/TaskExecutors.java
public class TaskExecutors {
    // 统一的后台读锁执行器
    public static <T> T readLock(Callable<T> task, Project project);

    // 统一的后台无锁执行器（用于长时间操作）
    public static void background(String title, Project project, Runnable task);

    // 统一的在 EDT 执行器
    public static void edt(Runnable task);
}
```

### 1.4 异常处理统一

**当前问题**: 异常处理不一致，部分直接 throw RuntimeException

**重构方案**:
```java
// 新建: src/main/java/org/yanhuang/plugins/intellij/exportjar/core/ExportException.java
public class ExportException extends RuntimeException {
    private final ErrorNotification notification;

    public ExportException(String message, ErrorNotification notification) { }
    public ExportException(String message, Throwable cause, ErrorNotification notification) { }
}

public interface ErrorNotification {
    void notify(Project project, String title, String message);
}
```

---

## P2: UI/Dialog 重构

### 2.1 SettingDialog 拆分

**当前问题**: `SettingDialog` (~416行) 过大，混合了 UI、业务逻辑和数据持久化

**重构方案**:
```
src/main/java/org/yanhuang/plugins/intellij/exportjar/ui/
├── SettingDialog.java          # 仅保留 DialogWrapper 框架
├── SettingPanelController.java # 用户交互逻辑
├── ExportOptionsPanel.java     # 选项面板组件
└── JarOutputPanel.java         # JAR 输出路径面板组件
```

### 2.2 FileListDialog 拆分

**当前问题**: `FileListDialog` (~16211行) 是最大的单文件

**重构方案**:
```
src/main/java/org/yanhuang/plugins/intellij/exportjar/ui/filelist/
├── FileListDialog.java              # DialogWrapper 框架
├── FileListTree.java                # TreeModel + TreeRenderer
├── FileListSelectionModel.java      # 选择状态管理
├── IncludeExcludeFilter.java        # 包含/排除过滤器
└── FileListChangeListener.java      # 变更监听器接口
```

### 2.3 TemplateEventHandler 拆分

**当前问题**: `TemplateEventHandler` (~15830行) 职责过多

**重构方案**:
```
src/main/java/org/yanhuang/plugins/intellij/exportjar/template/
├── TemplateManager.java        # 模板 CRUD 操作
├── TemplateEventHandler.java   # UI 事件处理（大幅精简）
└── TemplateSerializer.java     # 序列化逻辑
```

### 2.4 UI 组件工厂化

**当前问题**: UI 创建逻辑分散在各个类中

**重构方案**:
```java
// 新建: src/main/java/org/yanhuang/plugins/intellij/exportjar/ui/ComponentFactory.java
public class ComponentFactory {
    public static JPanel createTitledPanel(String title, JComponent content);
    public static JBSplitter createHorizontalSplitter(float ratio);
    public static TitledSeparator createSeparator(String title, JComponent forComponent);
}
```

---

## P3: 代码清理 (TODO 处理)

### 3.1 从 README.md TODO 列表整理

| 状态 | TODO 项 | 优先级 | 备注 |
|------|---------|--------|------|
| ~~[OK]~~ | ~~support inner and anonymous class export~~ | - | 已完成 |
| ~~[OK]~~ | ~~support large batch classes export~~ | - | 已完成 |
| ~~[OK]~~ | ~~support multi module export~~ | - | 已完成 |
| ~~[OK]~~ | ~~fix the issue: export all when select resource folder~~ | - | 已完成 |
| ~~[OK]~~ | ~~support messages log levels~~ | - | 已完成 |
| ~~[OK]~~ | ~~prompt when exporting jar exists~~ | - | 已完成 |
| ~~[OK]~~ | ~~show successfully complete hint~~ | - | 已完成 |
| ~~[OK]~~ | ~~select path textfield to list all selected history~~ | - | 已完成 |
| ~~[OK]~~ | ~~using sdk api to lookup nest class compiled files~~ | - | 已完成 |
| ~~[OK]~~ | ~~exclude test files~~ | - | 已完成 |
| ~~[OK]~~ | ~~when export whole module, export files out scope in sources?~~ | - | 已完成 |
| ~~[OK]~~ | ~~add export action to Build menu~~ | - | 已完成 |
| ~~[OK]~~ | ~~write document~~ | - | 已完成 |
| ~~[OK]~~ | ~~register in Build Menu~~ | - | 已完成 |
| ~~[OK]~~ | ~~register key-map and shortcut~~ | - | 已完成 |
| ~~[OK]~~ | ~~action can be perform from vcs menu as well as create patch~~ | - | 已完成 |
| P2 | action can be perform from history commit as well as create patch | 低 | VCS 历史视图支持 |
| P2 | action can be perform from local history dialog | 低 | 本地历史对话框支持 |
| P3 | show total export file(class,java, and others) count | 中 | 导出统计 |
| P3 | try to use com.intellij.openapi.vfs.VfsUtilCore.visitChildrenRecursively | 低 | 已部分使用 |
| ~~[OK]~~ | ~~Git menu miss export jar... item in 2020.3 community~~ | - | 已完成 |
| ~~[OK]~~ | ~~show selected file in export dialog~~ | - | 已完成 |
| ~~[OK]~~ | ~~use com.intellij.ide.actions.RevealFileAction to show file in system fold~~ | - | 已完成 |
| P3 | multi-language (国际化) | 高 | 准备国际化框架 |
| P3 | help docs | 中 | 用户帮助文档 |
| P2 | throw swing context event exception when trigger by first-keystroke | 中 | 快捷键注册问题 |
| P2 | button component mnemonic not working | 中 | 快捷键助记符修复 |
| ~~[OK]~~ | ~~remember dialog size~~ | - | 已完成 |
| P3 | reset dialog ui size | 低 | UI 重置功能 |
| P3 | plugin.xml item not working: add-to-group group-id="VcsGlobalGroup" | 低 | 已修复 |
| ~~[OK]~~ | ~~make unused FileListDialog.FileListTree to use for some tree select operation~~ | - | 已完成 |
| P3 | template name remove path and keep file name | 中 | 模板名称处理 |

### 3.2 代码中的 TODO

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/changes/ExportCommitSession.java`

**文件**: `src/main/java/org/yanhuang/plugins/intellij/exportjar/changes/ExportCommitExecutor.java`

### 3.3 清理建议

1. **移除废弃代码**: 检查是否有被注释掉的旧代码可以删除
2. **统一日志级别**: 确保所有日志使用 MessagesUtils 而非直接 System.out
3. **添加缺失的 @Override**: 检查方法重写是否都有 @Override 注解
4. **简化常数值**: 检查 Constants.java 中是否有未使用的常量

---

## P4: Kotlin 迁移 (长期目标)

### 迁移策略

**原则**: 仅在新编写或重大重构的模块中使用 Kotlin，不强制迁移现有 Java 代码

### 迁移优先级

| 模块 | 文件 | 原因 | 优先级 |
|------|------|------|--------|
| 工具类 | `CommonUtils.java` | 已有 Kotlin 辅助函数 | P3 |
| 模型类 | `model/` 目录 | 数据类适合 Kotlin | P3 |
| UI 辅助 | `UIFactory.java` | 工厂模式适合 Kotlin DSL | P3 |
| 测试代码 | `src/test/` | 测试框架已支持 Kotlin | P2 |

### 已存在 Kotlin 代码

```
src/main/kotlin/KVcsPad.kt
src/main/kotlin/org/yanhuang/plugins/intellij/exportjar/ui/WorkflowHelper.kt
```

### 建议的 Kotlin 迁移示例

```kotlin
// CommonUtils.kt - 扩展函数风格
fun Project.collectExportFiles(parent: VirtualFile): Set<VirtualFile> {
    // ...
}

// model/ExportOptions.kt - 数据类
data class ExportOptions(
    val exportJava: Boolean = true,
    val exportClass: Boolean = true,
    val exportTest: Boolean = false,
    val addDirectory: Boolean = false
)

// FileCollector.kt - 链式 API
class FileCollector(private val project: Project) {
    fun collect(files: Array<VirtualFile>): JarFileSet = ...
    fun addDirectoryEntries(): FileCollector = apply { ... }
}
```

---

## 执行路线图

> 执行进度更新 (2026-06-13)：P0/P1 已完成可安全验证的全部项；P2/P3 完成低风险项。
> 每项改动均通过 `./gradlew test` 验证，并单独提交。`buildPlugin` 成功，
> `verifyPlugin` 对 IC-251/252、IU-253/261/262 全部报告 **Compatible**（基线
> deprecated/internal API 计数无新增）。

### Phase 0: 反射与 API 修复 (1-2 天)
- [x] 修复 CommonUtils Opcodes 反射 → 改用 `Opcodes.ASM9` 常量 (commit b387617)
- [x] 修复废弃 API 警告 → `@SuppressWarnings("deprecation")` (commit 6b37936)
- [x] 简化 MessagesUtils 版本兼容代码 (P0-B 0.5, commit 1fae1ba)
- [x] 清理 FileListTree 双方法签名兼容 (P0-B 0.6, commit 73c7538)
- [x] 提取树遍历工具方法 (P0-C 0.10, commit 29bbf69)
- [ ] **[延后/高风险]** FileListDialog 反射替换 ChangesTree (0.3.2) 与内部 API 重写
      (0.2)：二者均需彻底移除 `extends SelectFilesDialog` 继承（见 0.4），属于核心
      文件树对话框的整体重写。`verifyPlugin` 将其判定为非阻塞（failureLevel=NONE，
      Compatible）。脱离继承会改变 include/exclude、分组、展开/折叠等行为，需在运行
      中的 IDE 手动验证，故本轮不盲目重构。

### Phase 1: 核心重构 (已完成)
- [x] 提取 FileCollector (1.1, commit c61811d)
- [x] 提取 JarWriter (1.2, commit 2649545)
- [x] 统一 TaskExecutors 线程工具 (1.3, commit f9f6378)
- [x] 统一异常处理 ExportJarException (1.4, commit 721b514)

### Phase 2: UI 重构 (部分完成)
- [x] UI 组件工厂化 (2.4, commit 43068fa)
- [ ] **[延后/高风险]** 拆分 SettingDialog (2.1) / FileListDialog (2.2) /
      TemplateEventHandler (2.3)：SettingDialog 通过 `.form`（GUI Designer）按字段名
      绑定组件，拆分会破坏表单绑定；FileListDialog 与 SelectFilesDialog 继承强耦合。
      这些 God-class 拆分需要 UI 回归测试，超出可自动验证范围。

### Phase 3: 清理 (已完成)
- [x] 移除 SettingDialog 调试代码 (3.3, commit 0e506ce)
- [x] 处理代码中 help-doc TODO 标记 (3.2, commit bca472c)
- [x] CHANGELOG 更新 (commit 110cfc6)
- [ ] README TODO 列表（功能性待办，见 3.1，属产品需求非重构）

### Phase 4 / Phase 5: 留作待办
- Kotlin 迁移、新增功能等长期项，按计划延后。

---

## 附录

### A. 当前代码统计

| 指标 | 数值 |
|------|------|
| Java 源文件 | ~35 个 |
| Java 代码行数 | ~4418 行 |
| Kotlin 源文件 | 2 个 |
| 测试文件 | 3 个 |
| 最大单文件 | FileListDialog.java (~410行, 含内部类 ~16211字符) |
| 最大类 | TemplateEventHandler.java (~15830行) |

### A.1 反射代码统计

| 文件 | 位置 | 类型 | 风险 |
|------|------|------|------|
| CommonUtils.java | L55 | Field.get() | 中 |
| FileListDialog.java | L71, L84 | setAccessible() | 高 |

### A.2 Workaround 代码统计

| 文件 | 行数 | 描述 |
|------|------|------|
| FileListDialog.java | ~40 | SelectFilesDialog 继承 workaround |
| MessagesUtils.java | ~20 | 版本兼容方法 |
| FileListTree.java | ~10 | 双方法签名兼容 |

### A.3 自行实现代码统计

| 文件 | 行数 | 描述 |
|------|------|------|
| CommonUtils.java | ~50 | 文件收集（可部分用 API 替代） |
| FileListTreeHandler.java | ~30 | 树遍历（重复） |

### B. 包结构

```
org.yanhuang.plugins.intellij.exportjar/
├── ExportJarAction.java          # 入口
├── ExportPacker.java             # 打包逻辑
├── HistoryData.java             # 历史数据结构
├── changes/                      # VCS 变更相关
│   ├── ExportLocalChangesAction.java
│   ├── ExportSelectLocalChangesAction.java
│   ├── ExportCommitSession.java
│   └── ExportCommitExecutor.java
├── model/                        # 数据模型
│   ├── ExportOptions.java
│   ├── ExportJarInfo.java
│   ├── SettingHistory.java
│   ├── SettingTemplate.java
│   ├── SettingSelectFile.java
│   └── UISizes.java
├── settings/                     # 设置持久化
│   └── HistoryDao.java
├── template/                     # 模板管理
│   └── TemplateExportActionGroup.java
├── ui/                          # UI 组件
│   ├── SettingDialog.java + .form
│   ├── FileListDialog.java
│   ├── LocalChangesSettingDialog.java
│   ├── UIFactory.java
│   ├── TemplateEventHandler.java
│   ├── FileListTreeHandler.java
│   ├── FileListActions.java
│   ├── FileListTreeCellRender.java
│   ├── FileListTreeGroupPolicyFactory.java
│   ├── LocalChangesDialogProvider.java
│   └── VcsHelper.java
└── utils/                        # 工具类
    ├── CommonUtils.java
    ├── MessagesUtils.java
    ├── Constants.java
    ├── SimpleFileLock.java
    └── UpgradeManager.java
```

### C. 依赖关系图

```
ExportJarAction
  └── UIFactory.createSettingDialog()
        └── SettingDialog
              ├── FileListDialog
              │     └── FileListTreeHandler
              ├── TemplateEventHandler
              │     └── HistoryDao
              └── HistoryDao
                    └── Constants

ExportLocalChangesAction
  └── UIFactory.createLocalChangesSettingDialog()
        └── LocalChangesSettingDialog
              └── SettingDialog (继承)

ExportPacker
  ├── CommonUtils
  ├── MessagesUtils
  └── Constants
```
