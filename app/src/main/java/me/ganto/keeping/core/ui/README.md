# LoadingStates 组件使用指南

## 概述

`LoadingStates.kt` 文件包含了一系列用于优化用户体验的加载状态组件。这些组件提供了统一的加载、空状态、错误状态和骨架屏显示。

## 组件列表

### 1. FullScreenLoading
全屏加载状态，适用于应用启动或重要数据加载时。

```kotlin
@Composable
fun FullScreenLoading(
    message: String = "加载中..."
)
```

**使用场景：**
- 应用启动时的数据初始化
- 重要页面切换时的加载
- 需要阻止用户交互的长时间操作

**示例：**
```kotlin
if (!isLoaded) {
    FullScreenLoading(message = "正在加载应用数据...")
    return@setContent
}
```

### 2. ContentLoading
内容区域加载状态，适用于局部内容加载。

```kotlin
@Composable
fun ContentLoading(
    message: String = "加载中...",
    modifier: Modifier = Modifier
)
```

**使用场景：**
- 列表数据加载
- 表单提交
- 局部内容更新

**示例：**
```kotlin
if (isLoading) {
    ContentLoading(
        message = "正在处理数据...",
        modifier = Modifier.padding(16.dp)
    )
}
```

### 3. EmptyState
空状态组件，当没有数据时显示友好的提示。

```kotlin
@Composable
fun EmptyState(
    title: String = "暂无数据",
    message: String = "这里还没有任何内容",
    modifier: Modifier = Modifier
)
```

**使用场景：**
- 空列表显示
- 搜索结果为空
- 首次使用引导

**示例：**
```kotlin
if (filteredBills.isEmpty()) {
    EmptyState(
        title = if (searchQuery.isNotBlank()) "未找到相关账单" else "暂无账单",
        message = if (searchQuery.isNotBlank()) "尝试调整搜索关键词" else "这里还没有任何账单"
    )
}
```

### 4. ErrorState
错误状态组件，显示错误信息并提供重试选项。

```kotlin
@Composable
fun ErrorState(
    title: String = "加载失败",
    message: String = "请检查网络连接后重试",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

**使用场景：**
- 网络请求失败
- 数据加载错误
- 需要用户重试的操作

**示例：**
```kotlin
if (hasError) {
    ErrorState(
        title = "加载失败",
        message = "无法获取账单数据，请检查网络连接",
        onRetry = { loadBills() }
    )
}
```

### 5. SkeletonLoading
骨架屏加载组件，提供内容结构的预览。

```kotlin
@Composable
fun SkeletonLoading(
    modifier: Modifier = Modifier
)
```

**使用场景：**
- 列表内容加载
- 卡片内容加载
- 需要展示内容结构的场景

**示例：**
```kotlin
if (isLoading) {
    SkeletonLoading(
        modifier = Modifier.fillMaxWidth()
    )
} else {
    // 显示实际内容
}
```

## 最佳实践

### 1. 选择合适的组件
- **FullScreenLoading**: 用于全屏阻塞式加载
- **ContentLoading**: 用于局部内容加载
- **EmptyState**: 用于无数据状态
- **ErrorState**: 用于错误状态，提供重试机制
- **SkeletonLoading**: 用于内容结构预览

### 2. 提供有意义的提示信息
```kotlin
// 好的示例
FullScreenLoading(message = "正在同步账单数据...")

// 不好的示例
FullScreenLoading(message = "加载中...")
```

### 3. 合理使用重试机制
```kotlin
ErrorState(
    title = "同步失败",
    message = "无法连接到服务器，请检查网络后重试",
    onRetry = { syncData() }
)
```

### 4. 结合使用多个组件
```kotlin
when {
    isLoading -> SkeletonLoading()
    hasError -> ErrorState(onRetry = { loadData() })
    data.isEmpty() -> EmptyState(title = "暂无数据")
    else -> ContentList(data = data)
}
```

## 在项目中的使用

### MainActivity.kt
- 使用 `FullScreenLoading` 显示应用启动加载

### SettingsScreen.kt
- 使用 `ContentLoading` 显示备份/恢复操作进度
- 使用 `SkeletonLoading` 显示备份文件列表加载状态

### MyScreen.kt
- 使用 `ContentLoading` 显示设置加载状态

### BillHomeScreen.kt
- 使用 `EmptyState` 显示空列表状态

### AllBillsScreen.kt
- 使用 `EmptyState` 显示空列表状态

## 主题适配

所有组件都使用 Material Design 3 主题，会自动适配深色/浅色模式。

## 性能考虑

- 组件使用 `remember` 优化重绘性能
- 骨架屏使用轻量级的占位符元素
- 避免在加载状态中进行复杂计算

## 扩展建议

可以根据项目需要扩展更多专用组件：
- `NetworkErrorState`: 专门处理网络错误
- `PermissionErrorState`: 专门处理权限错误
- `CustomSkeletonLoading`: 针对特定内容的自定义骨架屏 