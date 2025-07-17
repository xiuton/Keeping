package me.ganto.keeping.core.model

import java.util.UUID

// 账单数据类，增加唯一id
data class BillItem(
    val category: String,
    val amount: Double,
    val remark: String,
    val time: String, // 用户选择的账单日期
    val payType: String = "支付宝", // 支付方式
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val id: String = UUID.randomUUID().toString()
) 