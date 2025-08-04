package me.ganto.keeping.core.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 统一错误处理工具类
 */
object ErrorHandler {
    
    /**
     * 通用异常处理器
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        // 可以在这里添加崩溃上报逻辑
    }
    
    /**
     * 显示Toast消息的统一方法
     */
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * 安全执行函数，捕获异常并显示错误信息
     */
    suspend fun <T> safeExecute(
        context: Context,
        operation: suspend () -> T,
        errorMessage: String = "操作失败，请重试"
    ): Result<T> {
        return try {
            val result = withContext(Dispatchers.IO) {
                operation()
            }
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                showToast(context, errorMessage)
            }
            Result.failure(e)
        }
    }
    
    /**
     * 网络错误处理
     */
    fun handleNetworkError(context: Context, error: Throwable) {
        val message = when {
            error.message?.contains("timeout") == true -> "网络超时，请检查网络连接"
            error.message?.contains("404") == true -> "请求的资源不存在"
            error.message?.contains("500") == true -> "服务器错误，请稍后重试"
            else -> "网络连接失败，请检查网络设置"
        }
        showToast(context, message, Toast.LENGTH_LONG)
    }
    
    /**
     * 数据验证错误处理
     */
    fun handleValidationError(context: Context, field: String) {
        val message = when (field) {
            "amount" -> "请输入有效的金额"
            "category" -> "请选择分类"
            "date" -> "请选择有效日期"
            else -> "请检查输入信息"
        }
        showToast(context, message)
    }
    
    /**
     * 数据验证错误处理（带自定义消息）
     */
    fun handleValidationErrorMessage(context: Context, errorMessage: String) {
        showToast(context, errorMessage)
    }
    
    /**
     * 文件操作错误处理
     */
    fun handleFileError(context: Context, error: Throwable) {
        val message = when {
            error.message?.contains("permission") == true -> "没有文件访问权限"
            error.message?.contains("not found") == true -> "文件不存在"
            error.message?.contains("disk full") == true -> "存储空间不足"
            else -> "文件操作失败，请重试"
        }
        showToast(context, message, Toast.LENGTH_LONG)
    }
} 