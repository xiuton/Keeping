package me.ganto.keeping.core.util

import androidx.compose.runtime.*
import kotlinx.coroutines.*

/**
 * 性能优化工具类
 */
object PerformanceUtils {
    
    /**
     * 防抖函数，用于搜索等频繁操作
     */
    fun debounce(
        delayMillis: Long = 300L,
        scope: CoroutineScope,
        action: () -> Unit
    ): () -> Unit {
        var job: Job? = null
        return {
            job?.cancel()
            job = scope.launch {
                delay(delayMillis)
                action()
            }
        }
    }
    
    /**
     * 节流函数，用于滚动等高频事件
     */
    fun throttle(
        delayMillis: Long = 100L,
        scope: CoroutineScope,
        action: () -> Unit
    ): () -> Unit {
        var lastExecutionTime = 0L
        return {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastExecutionTime >= delayMillis) {
                lastExecutionTime = currentTime
                scope.launch {
                    action()
                }
            }
        }
    }
    
    /**
     * 记忆化函数，用于缓存计算结果
     * 注意：此函数需要在@Composable函数中调用
     */
    @Composable
    fun <T> rememberCalculation(
        key: Any,
        calculation: () -> T
    ): T {
        return remember(key) { calculation() }
    }
    
    /**
     * 延迟初始化，用于大对象的延迟创建
     */
    fun <T> lazyInit(initializer: () -> T): Lazy<T> {
        return lazy(LazyThreadSafetyMode.NONE, initializer)
    }
} 