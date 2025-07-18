package me.ganto.keeping.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import me.ganto.keeping.core.data.dataStore
import me.ganto.keeping.core.data.PREF_KEY_EXP_CAT
import me.ganto.keeping.core.data.PREF_KEY_INC_CAT
import me.ganto.keeping.core.data.PREF_KEY_EXP_PAY
import me.ganto.keeping.core.data.PREF_KEY_INC_PAY
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    isDark: Boolean,
    onDarkChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }

    val data by context.dataStore.data.collectAsState(initial = emptyPreferences())

    // 正确的默认值
    val defaultExpenseCategories = listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他")
    val defaultIncomeCategories = listOf("工资", "转账", "理财", "其他")
    val defaultExpensePayTypes = listOf("支付宝", "微信", "现金", "银行卡", "其他")
    val defaultIncomePayTypes = listOf("工资", "转账", "理财", "其他")

    val expenseCategories = remember(data) {
        data[PREF_KEY_EXP_CAT]?.let {
            try {
                gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) {
                defaultExpenseCategories
            }
        } ?: defaultExpenseCategories
    }
    val incomeCategories = remember(data) {
        data[PREF_KEY_INC_CAT]?.let {
            try {
                gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) {
                defaultIncomeCategories
            }
        } ?: defaultIncomeCategories
    }
    val expensePayTypes = remember(data) {
        data[PREF_KEY_EXP_PAY]?.let {
            try {
                gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) {
                defaultExpensePayTypes
            }
        } ?: defaultExpensePayTypes
    }
    val incomePayTypes = remember(data) {
        data[PREF_KEY_INC_PAY]?.let {
            try {
                gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) {
                defaultIncomePayTypes
            }
        } ?: defaultIncomePayTypes
    }

    var selectedType by remember { mutableStateOf("支出") }
    var newCategory by remember { mutableStateOf("") }
    var newPayType by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showEditDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // type, oldName
    var editText by remember { mutableStateOf("") }

    val currentCategories = if (selectedType == "支出") expenseCategories else incomeCategories
    val currentPayTypes = if (selectedType == "支出") expensePayTypes else incomePayTypes

    fun saveCategories(categories: List<String>) {
        scope.launch {
            context.dataStore.edit { prefs ->
                if (selectedType == "支出") {
                    prefs[PREF_KEY_EXP_CAT] = gson.toJson(categories)
                } else {
                    prefs[PREF_KEY_INC_CAT] = gson.toJson(categories)
                }
            }
        }
    }
    fun savePayTypes(payTypes: List<String>) {
        scope.launch {
            context.dataStore.edit { prefs ->
                if (selectedType == "支出") {
                    prefs[PREF_KEY_EXP_PAY] = gson.toJson(payTypes)
                } else {
                    prefs[PREF_KEY_INC_PAY] = gson.toJson(payTypes)
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 移除深色模式切换卡片
        // Card {
        //     Row(
        //         modifier = Modifier
        //             .fillMaxWidth()
        //             .padding(16.dp),
        //         verticalAlignment = Alignment.CenterVertically,
        //         horizontalArrangement = Arrangement.SpaceBetween
        //     ) {
        //     Text("深色模式", fontSize = 16.sp)
        //     Switch(checked = isDark, onCheckedChange = onDarkChange)
        //     }
        // }
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("管理类型", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedType == "支出",
                        onClick = { selectedType = "支出" }
                    )
            Text("支出", fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = selectedType == "收入",
                        onClick = { selectedType = "收入" }
                    )
            Text("收入", fontSize = 15.sp)
                }
            }
        }
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
        Text("分类管理", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
        Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    OutlinedTextField(
        value = newCategory,
        onValueChange = { newCategory = it },
        label = { Text("添加分类") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.weight(1f),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
    )
    Button(
        onClick = {
            val trimmed = newCategory.trim()
            if (trimmed.isNotEmpty() && !currentCategories.contains(trimmed)) {
                scope.launch {
                    saveCategories(currentCategories + trimmed)
                    newCategory = ""
                }
            }
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.height(56.dp)
    ) {
        Text("添加")
    }
}
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentCategories.forEach { category ->
                        val selected = false // 设置页不需要选中高亮
                        Card(
                            modifier = Modifier
                                .widthIn(min = 64.dp, max = 120.dp)
                                .clickable {
                                    showEditDialog = "category" to category
                                    editText = category
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer // 使用浅色背景
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = category,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (currentCategories.size > 1) {
                                    IconButton(
                                        onClick = { showDeleteDialog = "category" to category },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (selectedType == "支出") "支付方式管理" else "收入方式管理",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
        Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    OutlinedTextField(
        value = newPayType,
        onValueChange = { newPayType = it },
        label = { Text("添加方式") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.weight(1f),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
    )
    Button(
        onClick = {
            val trimmed = newPayType.trim()
            if (trimmed.isNotEmpty() && !currentPayTypes.contains(trimmed)) {
                scope.launch {
                    savePayTypes(currentPayTypes + trimmed)
                    newPayType = ""
                }
            }
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.height(56.dp)
    ) {
        Text("添加")
    }
}
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentPayTypes.forEach { payType ->
                        val selected = false // 设置页不需要选中高亮
                        Card(
                            modifier = Modifier
                                .widthIn(min = 64.dp, max = 120.dp)
                                .clickable {
                                    showEditDialog = "payType" to payType
                                    editText = payType
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer // 使用浅色背景
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = payType,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (currentPayTypes.size > 1) {
                                    IconButton(
                                        onClick = { showDeleteDialog = "payType" to payType },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    showDeleteDialog?.let { (type, name) ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除\"$name\"吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                            scope.launch {
                            when (type) {
                                "category" -> if (currentCategories.size > 1) {
                                    saveCategories(currentCategories.filter { it != name })
                                }
                                "payType" -> if (currentPayTypes.size > 1) {
                                    savePayTypes(currentPayTypes.filter { it != name })
                                }
                            }
                            showDeleteDialog = null // 删除后立即关闭对话框
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
    showEditDialog?.let { (type, oldName) ->
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("编辑${if (type == "category") "分类" else "方式"}") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editText.trim()
                        if (trimmed.isNotEmpty()) {
                            scope.launch {
                                if (type == "category") {
                                    if (trimmed != oldName && !currentCategories.contains(trimmed)) {
                                        saveCategories(currentCategories.map { if (it == oldName) trimmed else it })
                                    }
                                } else {
                                    if (trimmed != oldName && !currentPayTypes.contains(trimmed)) {
                                        savePayTypes(currentPayTypes.map { if (it == oldName) trimmed else it })
                                    }
                                }
                                showEditDialog = null
                            }
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text("取消") }
            }
        )
    }
} 