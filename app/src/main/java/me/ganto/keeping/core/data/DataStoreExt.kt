package me.ganto.keeping.core.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// 默认值常量 - 统一管理所有默认配置
object DefaultValues {
    val EXPENSE_CATEGORIES = listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他")
    val EXPENSE_PAY_TYPES = listOf("支付宝", "微信", "现金", "银行卡", "其他")
    val INCOME_CATEGORIES = listOf("工资", "买卖", "理财", "其他")
    val INCOME_PAY_TYPES = listOf("银行卡", "支付宝", "微信", "其他")
}

val Context.dataStore by preferencesDataStore(name = "bills")
val PREF_KEY_EXP_CAT = stringPreferencesKey("expense_categories")
val PREF_KEY_INC_CAT = stringPreferencesKey("income_categories")
val PREF_KEY_EXP_PAY = stringPreferencesKey("expense_pay_types")
val PREF_KEY_INC_PAY = stringPreferencesKey("income_pay_types")
// 可根据需要添加更多 key 