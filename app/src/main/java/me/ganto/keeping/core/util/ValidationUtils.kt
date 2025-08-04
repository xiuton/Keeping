package me.ganto.keeping.core.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据验证工具类
 */
object ValidationUtils {
    
    /**
     * 验证金额输入
     */
    fun validateAmount(amount: String): ValidationResult {
        return when {
            amount.isBlank() -> ValidationResult(false, "金额不能为空")
            amount.toDoubleOrNull() == null -> ValidationResult(false, "请输入有效的金额")
            amount.toDoubleOrNull()!! <= 0 -> ValidationResult(false, "金额必须大于0")
            amount.toDoubleOrNull()!! > 999999999 -> ValidationResult(false, "金额过大")
            else -> ValidationResult(true)
        }
    }
    
    /**
     * 验证分类选择
     */
    fun validateCategory(category: String): ValidationResult {
        return when {
            category.isBlank() -> ValidationResult(false, "请选择分类")
            else -> ValidationResult(true)
        }
    }
    
    /**
     * 验证日期格式
     */
    fun validateDate(dateString: String): ValidationResult {
        return try {
            val date = DateUtils.parseDate(dateString)
            if (date == null) {
                return ValidationResult(false, "日期格式无效")
            }
            
            val calendar = Calendar.getInstance()
            calendar.time = date
            
            // 检查日期是否合理
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val inputYear = calendar.get(Calendar.YEAR)
            
            when {
                inputYear < 2020 -> ValidationResult(false, "日期不能早于2020年")
                inputYear > currentYear + 1 -> ValidationResult(false, "日期不能晚于明年")
                else -> ValidationResult(true)
            }
        } catch (e: Exception) {
            ValidationResult(false, "日期格式无效")
        }
    }
    
    /**
     * 验证备注长度
     */
    fun validateRemark(remark: String): ValidationResult {
        return when {
            remark.length > 100 -> ValidationResult(false, "备注不能超过100个字符")
            else -> ValidationResult(true)
        }
    }
    
    /**
     * 验证账单数据完整性
     */
    fun validateBillItem(
        category: String,
        amount: String,
        date: String,
        remark: String
    ): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()
        
        results.add(validateCategory(category))
        results.add(validateAmount(amount))
        results.add(validateDate(date))
        results.add(validateRemark(remark))
        
        return results
    }
    
    /**
     * 格式化金额输入，去除多余的小数位
     */
    fun formatAmountInput(input: String): String {
        return input.replace(Regex("[^\\d.]"), "").let { filtered ->
            val parts = filtered.split(".")
            when {
                parts.size <= 2 -> {
                    if (parts[0].startsWith("0") && parts[0].length > 1 && !parts[0].startsWith("0.")) {
                        parts[0].trimStart('0').ifEmpty { "0" } + if (parts.size == 2) ".${parts[1]}" else ""
                    } else {
                        filtered
                    }
                }
                else -> parts[0] + "." + parts[1]
            }
        }
    }
}

/**
 * 验证结果数据类
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
) 