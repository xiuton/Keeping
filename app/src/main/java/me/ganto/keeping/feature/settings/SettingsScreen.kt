package me.ganto.keeping.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import me.ganto.keeping.core.data.dataStore
import me.ganto.keeping.core.data.PREF_KEY_EXP_CAT
import me.ganto.keeping.core.data.PREF_KEY_INC_CAT
import me.ganto.keeping.core.data.PREF_KEY_EXP_PAY
import me.ganto.keeping.core.data.PREF_KEY_INC_PAY
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.BorderStroke
import me.ganto.keeping.core.ui.ContentLoading
import me.ganto.keeping.core.data.DefaultValues
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }

    val data by context.dataStore.data.collectAsState(initial = emptyPreferences())

    val expenseCategories = remember(data) {
        data[PREF_KEY_EXP_CAT]?.let {
            try {
                gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) {
                DefaultValues.EXPENSE_CATEGORIES
            }
        } ?: DefaultValues.EXPENSE_CATEGORIES
    }
    val incomeCategories = remember(data) {
        data[PREF_KEY_INC_CAT]?.let {
            try {
                gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) {
                DefaultValues.INCOME_CATEGORIES
            }
        } ?: DefaultValues.INCOME_CATEGORIES
    }
    val expensePayTypes = remember(data) {
        data[PREF_KEY_EXP_PAY]?.let {
            try {
                gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) {
                DefaultValues.EXPENSE_PAY_TYPES
            }
        } ?: DefaultValues.EXPENSE_PAY_TYPES
    }
    val incomePayTypes = remember(data) {
        data[PREF_KEY_INC_PAY]?.let {
            try {
                gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) {
                DefaultValues.INCOME_PAY_TYPES
            }
        } ?: DefaultValues.INCOME_PAY_TYPES
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
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
        Card(
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
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
                val chunkedCategories = currentCategories.chunked(4)
                chunkedCategories.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                        rowItems.forEach { category ->
                        Card(
                            modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .defaultMinSize(minHeight = 90.dp)
                                .clickable {
                                    showEditDialog = "category" to category
                                    editText = category
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = category,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val idx = currentCategories.indexOf(category)
                                                if (idx > 0) {
                                                    val newList = currentCategories.toMutableList()
                                                    newList.removeAt(idx)
                                                    newList.add(idx - 1, category)
                                                    saveCategories(newList)
                                                }
                                            },
                                            enabled = currentCategories.indexOf(category) > 0,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowUpward,
                                                contentDescription = "上移",
                                                modifier = Modifier.size(18.dp),
                                                tint = if (currentCategories.indexOf(category) > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val idx = currentCategories.indexOf(category)
                                                if (idx < currentCategories.size - 1) {
                                                    val newList = currentCategories.toMutableList()
                                                    newList.removeAt(idx)
                                                    newList.add(idx + 1, category)
                                                    saveCategories(newList)
                                                }
                                            },
                                            enabled = currentCategories.indexOf(category) < currentCategories.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDownward,
                                                contentDescription = "下移",
                                                modifier = Modifier.size(18.dp),
                                                tint = if (currentCategories.indexOf(category) < currentCategories.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                if (currentCategories.size > 1) {
                                        Spacer(Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                    IconButton(
                                        onClick = { showDeleteDialog = "category" to category },
                                                modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除",
                                                    modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                                }
                            }
                        }
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Card(
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
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
                val chunkedPayTypes = currentPayTypes.chunked(4)
                chunkedPayTypes.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                        rowItems.forEach { payType ->
                        Card(
                            modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .defaultMinSize(minHeight = 90.dp)
                                .clickable {
                                    showEditDialog = "payType" to payType
                                    editText = payType
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = payType,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val idx = currentPayTypes.indexOf(payType)
                                                if (idx > 0) {
                                                    val newList = currentPayTypes.toMutableList()
                                                    newList.removeAt(idx)
                                                    newList.add(idx - 1, payType)
                                                    savePayTypes(newList)
                                                }
                                            },
                                            enabled = currentPayTypes.indexOf(payType) > 0,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowUpward,
                                                contentDescription = "上移",
                                                modifier = Modifier.size(18.dp),
                                                tint = if (currentPayTypes.indexOf(payType) > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val idx = currentPayTypes.indexOf(payType)
                                                if (idx < currentPayTypes.size - 1) {
                                                    val newList = currentPayTypes.toMutableList()
                                                    newList.removeAt(idx)
                                                    newList.add(idx + 1, payType)
                                                    savePayTypes(newList)
                                                }
                                            },
                                            enabled = currentPayTypes.indexOf(payType) < currentPayTypes.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDownward,
                                                contentDescription = "下移",
                                                modifier = Modifier.size(18.dp),
                                                tint = if (currentPayTypes.indexOf(payType) < currentPayTypes.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                if (currentPayTypes.size > 1) {
                                        Spacer(Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                    IconButton(
                                        onClick = { showDeleteDialog = "payType" to payType },
                                                modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除",
                                                    modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                                }
                            }
                        }
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
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
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
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