package me.ganto.keeping.feature.settings

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import me.ganto.keeping.core.data.dataStore
import me.ganto.keeping.core.data.PREF_KEY_EXP_CAT
import me.ganto.keeping.core.data.PREF_KEY_INC_CAT
import me.ganto.keeping.core.data.PREF_KEY_EXP_PAY
import me.ganto.keeping.core.data.PREF_KEY_INC_PAY
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(isDark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val data by context.dataStore.data.collectAsState(initial = emptyPreferences())

    val expenseCategories = remember(data) {
        data[PREF_KEY_EXP_CAT]?.let {
            try { Gson().fromJson(it, object: TypeToken<List<String>>(){}.type) } catch (_:Exception) { listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他") }
        } ?: listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他")
    }
    val incomeCategories = remember(data) {
        data[PREF_KEY_INC_CAT]?.let {
            try { Gson().fromJson(it, object: TypeToken<List<String>>(){}.type) } catch (_:Exception) { listOf("工资", "转账", "理财", "其他") }
        } ?: listOf("工资", "转账", "理财", "其他")
    }
    val expensePayTypes = remember(data) {
        data[PREF_KEY_EXP_PAY]?.let {
            try { Gson().fromJson(it, object: TypeToken<List<String>>(){}.type) } catch (_:Exception) { listOf("支付宝", "微信", "现金", "银行卡", "其他") }
        } ?: listOf("支付宝", "微信", "现金", "银行卡", "其他")
    }
    val incomePayTypes = remember(data) {
        data[PREF_KEY_INC_PAY]?.let {
            try { Gson().fromJson(it, object: TypeToken<List<String>>(){}.type) } catch (_:Exception) { listOf("工资", "转账", "理财", "其他") }
        } ?: listOf("工资", "转账", "理财", "其他")
    }
    var newExpenseCategory by remember { mutableStateOf("") }
    var newIncomeCategory by remember { mutableStateOf("") }
    var newExpensePayType by remember { mutableStateOf("") }
    var newIncomePayType by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("支出") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("设置", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("深色模式", fontSize = 16.sp)
            Switch(checked = isDark, onCheckedChange = onDarkChange)
        }
        // 类型单选框
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("类型：", fontSize = 15.sp)
            RadioButton(selected = type == "支出", onClick = { type = "支出" })
            Text("支出", fontSize = 15.sp)
            RadioButton(selected = type == "收入", onClick = { type = "收入" })
            Text("收入", fontSize = 15.sp)
        }
        // 分类管理
        Text("分类管理", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        val categories = if (type == "支出") expenseCategories else incomeCategories
        val newCategory = if (type == "支出") newExpenseCategory else newIncomeCategory
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCategory,
                onValueChange = {
                    if (type == "支出") newExpenseCategory = it else newIncomeCategory = it
                },
                label = { Text("新分类") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (newCategory.isNotBlank() && newCategory !in categories) {
                    if (type == "支出") {
                        val updated = expenseCategories + newCategory
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[PREF_KEY_EXP_CAT] = Gson().toJson(updated)
                            }
                        }
                        newExpenseCategory = ""
                    } else {
                        val updated = incomeCategories + newCategory
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[PREF_KEY_INC_CAT] = Gson().toJson(updated)
                            }
                        }
                        newIncomeCategory = ""
                    }
                }
            }, modifier = Modifier.padding(start = 8.dp)) { Text("添加") }
        }
        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
            items(categories.size) { index ->
                val cat = categories[index]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cat, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        if (type == "支出") {
                            val updated = expenseCategories - cat
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[PREF_KEY_EXP_CAT] = Gson().toJson(updated)
                                }
                            }
                        } else {
                            val updated = incomeCategories - cat
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[PREF_KEY_INC_CAT] = Gson().toJson(updated)
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "删除")
                    }
                }
            }
        }
        // 支付/收入方式管理
        Text(if (type == "支出") "支付方式管理" else "收入方式管理", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        val payTypes = if (type == "支出") expensePayTypes else incomePayTypes
        val newPayType = if (type == "支出") newExpensePayType else newIncomePayType
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newPayType,
                onValueChange = {
                    if (type == "支出") newExpensePayType = it else newIncomePayType = it
                },
                label = { Text("新方式") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (newPayType.isNotBlank() && newPayType !in payTypes) {
                    if (type == "支出") {
                        val updated = expensePayTypes + newPayType
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[PREF_KEY_EXP_PAY] = Gson().toJson(updated)
                            }
                        }
                        newExpensePayType = ""
                    } else {
                        val updated = incomePayTypes + newPayType
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[PREF_KEY_INC_PAY] = Gson().toJson(updated)
                            }
                        }
                        newIncomePayType = ""
                    }
                }
            }, modifier = Modifier.padding(start = 8.dp)) { Text("添加") }
        }
        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
            items(payTypes.size) { index ->
                val pt = payTypes[index]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pt, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        if (type == "支出") {
                            val updated = expensePayTypes - pt
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[PREF_KEY_EXP_PAY] = Gson().toJson(updated)
                                }
                            }
                        } else {
                            val updated = incomePayTypes - pt
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[PREF_KEY_INC_PAY] = Gson().toJson(updated)
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "删除")
                    }
                }
            }
        }
    }
} 