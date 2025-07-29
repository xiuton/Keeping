package me.ganto.keeping.core.util

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyUtils {
    
    /**
     * 格式化货币显示，保留2位小数
     */
    fun formatMoney(amount: Double): String {
        return BigDecimal(amount).setScale(2, RoundingMode.HALF_UP).toString()
    }
    
    /**
     * 安全的货币加法，避免浮点数精度问题
     */
    fun addMoney(amount1: Double, amount2: Double): Double {
        return BigDecimal(amount1).add(BigDecimal(amount2)).toDouble()
    }
    
    /**
     * 安全的货币减法，避免浮点数精度问题
     */
    fun subtractMoney(amount1: Double, amount2: Double): Double {
        return BigDecimal(amount1).subtract(BigDecimal(amount2)).toDouble()
    }
    
    /**
     * 安全的货币乘法，避免浮点数精度问题
     */
    fun multiplyMoney(amount: Double, multiplier: Double): Double {
        return BigDecimal(amount).multiply(BigDecimal(multiplier)).toDouble()
    }
    
    /**
     * 安全的货币除法，避免浮点数精度问题
     */
    fun divideMoney(amount: Double, divisor: Double): Double {
        return BigDecimal(amount).divide(BigDecimal(divisor), 2, RoundingMode.HALF_UP).toDouble()
    }
    
    /**
     * 计算结余（收入 + 支出），支出为负数
     */
    fun calculateBalance(income: Double, expense: Double): Double {
        return addMoney(income, expense)
    }
    
    /**
     * 格式化显示结余
     */
    fun formatBalance(income: Double, expense: Double): String {
        val balance = calculateBalance(income, expense)
        return formatMoney(balance)
    }
    
    /**
     * 格式化简化数字显示，使用k、w等符号
     */
    fun formatSimpleMoney(amount: Double): String {
        return when {
            amount >= 10000 -> {
                val wan = amount / 10000
                if (wan >= 10) {
                    "${wan.toInt()}w"
                } else {
                    "${String.format("%.1f", wan)}w"
                }
            }
            amount >= 1000 -> {
                val qian = amount / 1000
                if (qian >= 10) {
                    "${qian.toInt()}k"
                } else {
                    "${String.format("%.1f", qian)}k"
                }
            }
            amount >= 100 -> {
                amount.toInt().toString()
            }
            else -> {
                String.format("%.0f", amount)
            }
        }
    }
} 