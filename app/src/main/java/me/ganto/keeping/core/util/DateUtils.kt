package me.ganto.keeping.core.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期工具类，统一管理日期格式化
 */
object DateUtils {
    
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val DATE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val DATE_TIME_SECONDS_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    /**
     * 格式化日期为 yyyy-MM-dd 格式
     */
    fun formatDate(date: Date): String {
        return DATE_FORMAT.format(date)
    }
    
    /**
     * 格式化日期为 yyyy-MM-dd HH:mm 格式
     */
    fun formatDateTime(date: Date): String {
        return DATE_TIME_FORMAT.format(date)
    }
    
    /**
     * 格式化日期为 yyyy-MM-dd HH:mm:ss 格式
     */
    fun formatDateTimeWithSeconds(date: Date): String {
        return DATE_TIME_SECONDS_FORMAT.format(date)
    }
    
    /**
     * 格式化时间为 HH:mm 格式
     */
    fun formatTime(date: Date): String {
        return TIME_FORMAT.format(date)
    }
    
    /**
     * 解析日期字符串
     */
    fun parseDate(dateString: String): Date? {
        return try {
            DATE_FORMAT.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 解析日期时间字符串
     */
    fun parseDateTime(dateTimeString: String): Date? {
        return try {
            DATE_TIME_FORMAT.parse(dateTimeString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取当前日期字符串
     */
    fun getCurrentDateString(): String {
        return formatDate(Date())
    }
    
    /**
     * 获取当前时间字符串
     */
    fun getCurrentTimeString(): String {
        return formatTime(Date())
    }
    
    /**
     * 获取当前日期时间字符串
     */
    fun getCurrentDateTimeString(): String {
        return formatDateTime(Date())
    }
} 