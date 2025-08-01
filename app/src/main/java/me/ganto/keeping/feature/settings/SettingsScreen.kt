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
import me.ganto.keeping.core.data.BackupManager
import me.ganto.keeping.core.data.BackupFileInfo
import me.ganto.keeping.core.model.BillItem
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.foundation.layout.FlowRow
import me.ganto.keeping.core.data.DefaultValues
import android.net.Uri
import java.io.InputStream
import androidx.compose.foundation.BorderStroke
import me.ganto.keeping.core.ui.ContentLoading
import me.ganto.keeping.core.ui.SkeletonLoading


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    backupManager: BackupManager,
    collectSettingsData: () -> Map<String, String>,
    bills: List<BillItem>,
    saveBills: (List<BillItem>) -> Unit
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
    
    // 备份相关状态
    var backupFiles by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf<BackupFileInfo?>(null) }
    var showDeleteBackupDialog by remember { mutableStateOf<BackupFileInfo?>(null) }
    var backupDescription by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf<String?>(null) }
    var showBackupPathDialog by remember { mutableStateOf(false) }
    var showImporting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var isLoadingBackupFiles by remember { mutableStateOf(true) }
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showMessage = "存储权限已授予"
        } else {
            showMessage = "需要存储权限才能创建外部备份"
        }
    }

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
    
    // 加载备份文件列表
    LaunchedEffect(Unit) {
        backupManager.getBackupFiles().onSuccess { files ->
            backupFiles = files
            isLoadingBackupFiles = false
        }.onFailure {
            isLoadingBackupFiles = false
        }
    }
    
    // 备份相关函数
    fun createBackup() {
        // 检查权限
        val hasWritePermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasReadPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasWritePermission || !hasReadPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
            return
        }
        
        scope.launch {
            isLoading = true
            try {
                val settings = collectSettingsData()
                val result = backupManager.createBackup(bills, settings, backupDescription)
                result.onSuccess { fileName ->
                    showMessage = "备份创建成功: $fileName"
                    backupDescription = ""
                    showBackupDialog = false
                    // 刷新备份文件列表
                    backupManager.getBackupFiles().onSuccess { files ->
                        backupFiles = files
                    }
                }.onFailure { error ->
                    showMessage = "备份创建失败: ${error.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    fun restoreBackup(backupFile: BackupFileInfo) {
        scope.launch {
            isLoading = true
            try {
                val result = backupManager.restoreData(backupFile.fileName)
                result.onSuccess { (restoredBills, restoredSettings) ->
                    // 恢复账单数据
                    saveBills(restoredBills)
                    
                    // 恢复设置数据
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            restoredSettings.forEach { (key, value) ->
                                when (key) {
                                    "expense_categories" -> prefs[PREF_KEY_EXP_CAT] = value
                                    "income_categories" -> prefs[PREF_KEY_INC_CAT] = value
                                    "expense_pay_types" -> prefs[PREF_KEY_EXP_PAY] = value
                                    "income_pay_types" -> prefs[PREF_KEY_INC_PAY] = value
                                    "is_dark" -> prefs[booleanPreferencesKey("is_dark")] = value.toBoolean()
                                    "sort_by" -> prefs[stringPreferencesKey("sort_by")] = value
                                }
                            }
                        }
                    }
                    
                    showMessage = "数据恢复成功"
                    showRestoreDialog = null
                }.onFailure { error ->
                    showMessage = "数据恢复失败: ${error.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    fun deleteBackup(backupFile: BackupFileInfo) {
        scope.launch {
            isLoading = true
            try {
                val result = backupManager.deleteBackup(backupFile.fileName)
                result.onSuccess {
                    showMessage = "备份文件删除成功"
                    showDeleteBackupDialog = null
                    // 刷新备份文件列表
                    backupManager.getBackupFiles().onSuccess { files ->
                        backupFiles = files
                    }
                }.onFailure { error ->
                    showMessage = "删除失败: ${error.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                showImporting = true
                importError = null
                val result = backupManager.importBackupFromJsonUri(context, it)
                result.onSuccess { (restoredBills, restoredSettings) ->
                    saveBills(restoredBills)
                    // 恢复设置数据
                    context.dataStore.edit { prefs ->
                        restoredSettings.forEach { (key, value) ->
                            when (key) {
                                "expense_categories" -> prefs[PREF_KEY_EXP_CAT] = value
                                "income_categories" -> prefs[PREF_KEY_INC_CAT] = value
                                "expense_pay_types" -> prefs[PREF_KEY_EXP_PAY] = value
                                "income_pay_types" -> prefs[PREF_KEY_INC_PAY] = value
                                "is_dark" -> prefs[booleanPreferencesKey("is_dark")] = value.toBoolean()
                                "sort_by" -> prefs[stringPreferencesKey("sort_by")] = value
                            }
                        }
                    }
                    showMessage = "JSON恢复成功"
                }.onFailure { error ->
                    importError = error.message
                    showMessage = "JSON恢复失败: ${error.message}"
                }
                showImporting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 开屏模式设置
        Card(
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("开屏界面", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                // 开屏模式信息
                Text(
                    text = "当前模式：图片模式",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Text(
                    text = "使用固定的全屏启动图",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
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
        
        // 备份管理卡片
        Card(
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("数据备份", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showBackupPathDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "备份文件位置",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // 创建备份按钮
                Button(
                    onClick = { showBackupDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Backup, contentDescription = "创建备份")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("创建备份")
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 新增：从JSON文件恢复按钮
                OutlinedButton(
                    onClick = { importLauncher.launch("application/json") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && !showImporting
                ) {
                    Icon(Icons.Default.Restore, contentDescription = "从JSON恢复")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从JSON文件恢复")
                }
                if (showImporting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在导入JSON...", fontSize = 13.sp)
                    }
                }
                if (importError != null) {
                    Text(importError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 备份文件列表
                if (isLoadingBackupFiles) {
                    // 加载时显示骨架屏
                    Text("备份文件列表", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonLoading(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (backupFiles.isNotEmpty()) {
                    Text("备份文件列表", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        backupFiles.forEach { backupFile ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = backupFile.fileName,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (backupFile.description.isNotEmpty()) {
                                                Text(
                                                    text = backupFile.description,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "账单数量: ${backupFile.billsCount} | 文件大小: ${backupFile.fileSize / 1024}KB",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                                    .format(Date(backupFile.timestamp)),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { showRestoreDialog = backupFile },
                                                enabled = !isLoading
                                            ) {
                                                Icon(
                                                    Icons.Default.Restore,
                                                    contentDescription = "恢复",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { showDeleteBackupDialog = backupFile },
                                                enabled = !isLoading
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "删除",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "暂无备份文件",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
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
    
    // 创建备份对话框
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("创建备份") },
            text = {
                Column {
                    Text("将为当前数据创建备份文件")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupDescription,
                        onValueChange = { backupDescription = it },
                        label = { Text("备份描述（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { createBackup() },
                    enabled = !isLoading
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showBackupDialog = false
                        backupDescription = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 恢复备份对话框
    showRestoreDialog?.let { backupFile ->
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text("恢复数据") },
            text = { 
                Text("确定要恢复备份文件 \"${backupFile.fileName}\" 吗？\n当前数据将被覆盖。")
            },
            confirmButton = {
                TextButton(
                    onClick = { restoreBackup(backupFile) },
                    enabled = !isLoading
                ) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 删除备份对话框
    showDeleteBackupDialog?.let { backupFile ->
        AlertDialog(
            onDismissRequest = { showDeleteBackupDialog = null },
            title = { Text("删除备份") },
            text = { 
                Text("确定要删除备份文件 \"${backupFile.fileName}\" 吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = { deleteBackup(backupFile) },
                    enabled = !isLoading
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBackupDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 消息提示
    showMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { showMessage = null },
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { showMessage = null }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 加载指示器
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card {
                ContentLoading(
                    message = "正在处理数据...",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    if (showBackupPathDialog) {
        AlertDialog(
            onDismissRequest = { showBackupPathDialog = false },
            title = { Text("备份文件位置") },
            text = {
                Column {
                    val backupPaths = backupManager.getBackupPaths()
                    backupPaths.forEach { (key, path) ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "内部存储（仅本App可见）",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = path,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：备份文件仅保存在本App的私有目录（/Android/data/me.ganto.keeping/files/backups），部分文件管理器在Android 11及以上无法直接访问。如需导出请使用导出/分享功能。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupPathDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
} 