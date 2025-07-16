@file:OptIn(ExperimentalLayoutApi::class)
package me.ganto.keeping

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import me.ganto.keeping.theme.KeepingTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import me.ganto.keeping.theme.CategoryGreen
import me.ganto.keeping.theme.CategoryRed
import me.ganto.keeping.theme.CategoryOrange
import me.ganto.keeping.theme.CategoryBlue
import me.ganto.keeping.theme.CategoryPurple
import me.ganto.keeping.theme.CategoryTeal
import me.ganto.keeping.theme.CategoryGrey
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import kotlin.math.abs
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.ganto.keeping.navigation.NavGraph
import me.ganto.keeping.navigation.ROUTE_MAIN
import me.ganto.keeping.navigation.ROUTE_ADD_BILL
import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.core.data.PREF_KEY_EXP_CAT
import me.ganto.keeping.core.data.PREF_KEY_INC_CAT
import me.ganto.keeping.core.data.PREF_KEY_EXP_PAY
import me.ganto.keeping.core.data.PREF_KEY_INC_PAY
import me.ganto.keeping.core.data.dataStore

// DataStore扩展
val BILLS_KEY = stringPreferencesKey("bills_json")
val gson = Gson()
val PREF_KEY_DARK = booleanPreferencesKey("is_dark")
val PREF_KEY_SORT = stringPreferencesKey("sort_by")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 这里直接调用 NavGraph，传递所需参数
            val context = LocalContext.current
            var bills by remember { mutableStateOf<List<BillItem>>(emptyList()) }
            var isLoaded by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            var navIndex by rememberSaveable { mutableStateOf(0) }
            var showAddDialog by remember { mutableStateOf(false) }
            var isDark by remember { mutableStateOf(false) }
            var isDarkLoaded by remember { mutableStateOf(false) }
            var sortBy by remember { mutableStateOf("create") }
            var sortLoaded by remember { mutableStateOf(false) }
            val expenseCategories by context.dataStore.data
                .map { it[PREF_KEY_EXP_CAT]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他") }
                .collectAsState(initial = listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他"))
            val incomeCategories by context.dataStore.data
                .map { it[PREF_KEY_INC_CAT]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: listOf("工资", "转账", "理财", "其他") }
                .collectAsState(initial = listOf("工资", "转账", "理财", "其他"))
            val expensePayTypes by context.dataStore.data
                .map { it[PREF_KEY_EXP_PAY]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: listOf("支付宝", "微信", "现金", "银行卡", "其他") }
                .collectAsState(initial = listOf("支付宝", "微信", "现金", "银行卡", "其他"))
            val incomePayTypes by context.dataStore.data
                .map { it[PREF_KEY_INC_PAY]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: listOf("工资", "转账", "理财", "其他") }
                .collectAsState(initial = listOf("工资", "转账", "理财", "其他"))
            val catPayLoaded = expenseCategories.isNotEmpty() && incomeCategories.isNotEmpty() && expensePayTypes.isNotEmpty() && incomePayTypes.isNotEmpty()
            LaunchedEffect(Unit) {
                val json = context.dataStore.data.map { it[BILLS_KEY] ?: "" }.first()
                bills = if (json.isNotBlank()) {
                    try {
                        gson.fromJson(json, object : TypeToken<List<BillItem>>() {}.type)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()
                isLoaded = true
            }
            LaunchedEffect(Unit) {
                val dark = context.dataStore.data.map { it[PREF_KEY_DARK] ?: false }.first()
                isDark = dark
                isDarkLoaded = true
            }
            LaunchedEffect(Unit) {
                val sort = context.dataStore.data.map { it[PREF_KEY_SORT] ?: "create" }.first()
                sortBy = sort
                sortLoaded = true
            }
            fun saveBills(newBills: List<BillItem>) {
                bills = newBills
                scope.launch(Dispatchers.IO) {
                    context.dataStore.edit { prefs ->
                        prefs[BILLS_KEY] = gson.toJson(newBills)
                    }
                }
            }
            fun saveDarkMode(dark: Boolean) {
                isDark = dark
                scope.launch(Dispatchers.IO) {
                    context.dataStore.edit { prefs ->
                        prefs[PREF_KEY_DARK] = dark
                    }
                }
            }
            fun saveSortBy(sort: String) {
                sortBy = sort
                scope.launch(Dispatchers.IO) {
                    context.dataStore.edit { prefs ->
                        prefs[PREF_KEY_SORT] = sort
                    }
                }
            }
            if (!isLoaded || !isDarkLoaded || !sortLoaded || !catPayLoaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@setContent
            }
            NavGraph(
                bills = bills,
                saveBills = ::saveBills,
                isDark = isDark,
                saveDarkMode = ::saveDarkMode,
                sortBy = sortBy,
                saveSortBy = ::saveSortBy,
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories,
                expensePayTypes = expensePayTypes,
                incomePayTypes = incomePayTypes,
                navIndex = navIndex,
                setNavIndex = { navIndex = it },
                showAddDialog = showAddDialog,
                setShowAddDialog = { showAddDialog = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(isDark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expenseCategories by remember { mutableStateOf(listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他")) }
    var incomeCategories by remember { mutableStateOf(listOf("工资", "转账", "理财", "其他")) }
    var expensePayTypes by remember { mutableStateOf(listOf("支付宝", "微信", "现金", "银行卡", "其他")) }
    var incomePayTypes by remember { mutableStateOf(listOf("工资", "转账", "理财", "其他")) }
    var newExpenseCategory by remember { mutableStateOf("") }
    var newIncomeCategory by remember { mutableStateOf("") }
    var newExpensePayType by remember { mutableStateOf("") }
    var newIncomePayType by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("支出") }
    val colorScheme = MaterialTheme.colorScheme
    var confirmDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    // 加载
    LaunchedEffect(Unit) {
        val data = context.dataStore.data.first()
        data[PREF_KEY_EXP_CAT]?.let {
            expenseCategories = try { gson.fromJson(it, object: TypeToken<List<String>>(){}.type) } catch (_:Exception) { expenseCategories }
        }
        data[PREF_KEY_INC_CAT]?.let {
            incomeCategories = try { gson.fromJson(it, object: TypeToken<List<String>>(){}.type) } catch (_:Exception) { incomeCategories }
        }
        data[PREF_KEY_EXP_PAY]?.let {
            expensePayTypes = try { gson.fromJson(it, object: TypeToken<List<String>>(){}.type) } catch (_:Exception) { expensePayTypes }
        }
        data[PREF_KEY_INC_PAY]?.let {
            incomePayTypes = try { gson.fromJson(it, object: TypeToken<List<String>>(){}.type) } catch (_:Exception) { incomePayTypes }
        }
        loaded = true
    }
    // 保存
    fun saveAll() {
        scope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs[PREF_KEY_EXP_CAT] = gson.toJson(expenseCategories)
                prefs[PREF_KEY_INC_CAT] = gson.toJson(incomeCategories)
                prefs[PREF_KEY_EXP_PAY] = gson.toJson(expensePayTypes)
                prefs[PREF_KEY_INC_PAY] = gson.toJson(incomePayTypes)
            }
        }
    }
    fun addExpenseCategory(cat: String) {
        expenseCategories = expenseCategories + cat; saveAll()
    }
    fun removeExpenseCategory(cat: String) {
        expenseCategories = expenseCategories - cat; saveAll()
    }
    fun addIncomeCategory(cat: String) {
        incomeCategories = incomeCategories + cat; saveAll()
    }
    fun removeIncomeCategory(cat: String) {
        incomeCategories = incomeCategories - cat; saveAll()
    }
    fun addExpensePayType(pt: String) {
        expensePayTypes = expensePayTypes + pt; saveAll()
    }
    fun removeExpensePayType(pt: String) {
        expensePayTypes = expensePayTypes - pt; saveAll()
    }
    fun addIncomePayType(pt: String) {
        incomePayTypes = incomePayTypes + pt; saveAll()
    }
    fun removeIncomePayType(pt: String) {
        incomePayTypes = incomePayTypes - pt; saveAll()
    }
    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        // 你可以继续自定义其他参数
    )
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
        if (type == "支出") {
            Text("支出分类", fontWeight = FontWeight.SemiBold)
            // 支出分类
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                expenseCategories.forEachIndexed { idx, cat ->
                    var editing by remember { mutableStateOf(false) }
                    var editText by remember { mutableStateOf(cat) }
                    Box(
                        Modifier
                            .background(colorScheme.secondary, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (editing) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (editText.isNotBlank() && editText != cat && !expenseCategories.contains(editText)) {
                                                expenseCategories = expenseCategories.toMutableList().also { it[idx] = editText }
                                                saveAll()
                                            }
                                            editing = false
                                        }
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    colors = textFieldColors,
                                    modifier = Modifier.widthIn(min = 60.dp, max = 120.dp).height(32.dp),
                                    shape = MaterialTheme.shapes.small,
                                )
                            } else {
                                Text(cat, fontSize = 13.sp, color = colorScheme.onSecondary, modifier = Modifier.clickable { editing = true })
                            }
                            IconButton(onClick = { confirmDelete = "expense_cat" to cat }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = colorScheme.onSecondary)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = newExpenseCategory,
                    onValueChange = { newExpenseCategory = it },
                    label = { Text("新增支出分类") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = textFieldColors
                )
                Button(
                    onClick = {
                        if (newExpenseCategory.isNotBlank() && !expenseCategories.contains(newExpenseCategory)) {
                            addExpenseCategory(newExpenseCategory)
                            newExpenseCategory = ""
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text("添加", fontSize = 13.sp) }
            }
            Text("支出方式", fontWeight = FontWeight.SemiBold)
            // 支出方式
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                expensePayTypes.forEachIndexed { idx, pt ->
                    var editing by remember { mutableStateOf(false) }
                    var editText by remember { mutableStateOf(pt) }
                    Box(
                        Modifier
                            .background(colorScheme.secondary, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (editing) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (editText.isNotBlank() && editText != pt && !expensePayTypes.contains(editText)) {
                                                expensePayTypes = expensePayTypes.toMutableList().also { it[idx] = editText }
                                                saveAll()
                                            }
                                            editing = false
                                        }
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    colors = textFieldColors,
                                    modifier = Modifier.widthIn(min = 60.dp, max = 120.dp).height(32.dp),
                                    shape = MaterialTheme.shapes.small,
                                )
                            } else {
                                Text(pt, fontSize = 13.sp, color = colorScheme.onSecondary, modifier = Modifier.clickable { editing = true })
                            }
                            IconButton(onClick = { confirmDelete = "expense_pay" to pt }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = colorScheme.onSecondary)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = newExpensePayType,
                    onValueChange = { newExpensePayType = it },
                    label = { Text("新增支出方式") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = textFieldColors
                )
                Button(
                    onClick = {
                        if (newExpensePayType.isNotBlank() && !expensePayTypes.contains(newExpensePayType)) {
                            addExpensePayType(newExpensePayType)
                            newExpensePayType = ""
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text("添加", fontSize = 13.sp) }
            }
        } else {
            Text("收入分类", fontWeight = FontWeight.SemiBold)
            // 收入分类
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                incomeCategories.forEachIndexed { idx, cat ->
                    var editing by remember { mutableStateOf(false) }
                    var editText by remember { mutableStateOf(cat) }
                    Box(
                        Modifier
                            .background(colorScheme.secondary, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (editing) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (editText.isNotBlank() && editText != cat && !incomeCategories.contains(editText)) {
                                                incomeCategories = incomeCategories.toMutableList().also { it[idx] = editText }
                                                saveAll()
                                            }
                                            editing = false
                                        }
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    colors = textFieldColors,
                                    modifier = Modifier.widthIn(min = 60.dp, max = 120.dp).height(32.dp),
                                    shape = MaterialTheme.shapes.small,
                                )
                            } else {
                                Text(cat, fontSize = 13.sp, color = colorScheme.onSecondary, modifier = Modifier.clickable { editing = true })
                            }
                            IconButton(onClick = { confirmDelete = "income_cat" to cat }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = colorScheme.onSecondary)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = newIncomeCategory,
                    onValueChange = { newIncomeCategory = it },
                    label = { Text("新增收入分类") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = textFieldColors
                )
                Button(
                    onClick = {
                        if (newIncomeCategory.isNotBlank() && !incomeCategories.contains(newIncomeCategory)) {
                            addIncomeCategory(newIncomeCategory)
                            newIncomeCategory = ""
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text("添加", fontSize = 13.sp) }
            }
            Text("收入方式", fontWeight = FontWeight.SemiBold)
            // 收入方式
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                incomePayTypes.forEachIndexed { idx, pt ->
                    var editing by remember { mutableStateOf(false) }
                    var editText by remember { mutableStateOf(pt) }
                    Box(
                        Modifier
                            .background(colorScheme.secondary, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (editing) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (editText.isNotBlank() && editText != pt && !incomePayTypes.contains(editText)) {
                                                incomePayTypes = incomePayTypes.toMutableList().also { it[idx] = editText }
                                                saveAll()
                                            }
                                            editing = false
                                        }
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    colors = textFieldColors,
                                    modifier = Modifier.widthIn(min = 60.dp, max = 120.dp).height(32.dp),
                                    shape = MaterialTheme.shapes.small,
                                )
                            } else {
                                Text(pt, fontSize = 13.sp, color = colorScheme.onSecondary, modifier = Modifier.clickable { editing = true })
                            }
                            IconButton(onClick = { confirmDelete = "income_pay" to pt }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = colorScheme.onSecondary)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = newIncomePayType,
                    onValueChange = { newIncomePayType = it },
                    label = { Text("新增收入方式") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = textFieldColors
                )
                Button(
                    onClick = {
                        if (newIncomePayType.isNotBlank() && !incomePayTypes.contains(newIncomePayType)) {
                            addIncomePayType(newIncomePayType)
                            newIncomePayType = ""
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text("添加", fontSize = 13.sp) }
            }
        }
    }
    // 二次确认弹窗
    confirmDelete?.let { pair ->
        val delType = pair.first
        val value = pair.second
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("确认删除？") },
            text = { Text("确定要删除“$value”吗？") },
            confirmButton = {
                TextButton(onClick = {
                    when (delType) {
                        "expense_cat" -> removeExpenseCategory(value)
                        "expense_pay" -> removeExpensePayType(value)
                        "income_cat" -> removeIncomeCategory(value)
                        "income_pay" -> removeIncomePayType(value)
                    }
                    confirmDelete = null
                }) { Text("确定", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillScreen(
    onAdd: (BillItem) -> Unit,
    onBack: () -> Unit,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    expensePayTypes: List<String>,
    incomePayTypes: List<String>
) {
    var tabIndex by remember { mutableStateOf(0) } // 0: 支出, 1: 收入
    val type = if (tabIndex == 0) "支出" else "收入"
    val categories = if (type == "支出") expenseCategories else incomeCategories
    val payTypes = if (type == "支出") expensePayTypes else incomePayTypes

    var category by remember { mutableStateOf(categories[0]) }
    var payType by remember { mutableStateOf(payTypes[0]) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var payTypeExpanded by remember { mutableStateOf(false) }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
    )
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("新增账单") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) { Text("支出") }
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) { Text("收入") }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = textFieldColors,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp
                )
            )
            SuperDropdownField(
                value = category,
                label = "分类",
                expanded = expanded,
                onExpandedChange = { expanded = it },
                items = categories,
                onItemSelected = { category = it },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("金额") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = textFieldColors,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp
                )
            )
            SuperDropdownField(
                value = payType,
                label = if (type == "支出") "支付方式" else "收入方式",
                expanded = payTypeExpanded,
                onExpandedChange = { payTypeExpanded = it },
                items = payTypes,
                onItemSelected = { payType = it },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                label = { Text("备注") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = textFieldColors,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("日期：${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)}")
                Spacer(Modifier.width(8.dp))
                Button(onClick = { showDatePicker = true }) { Text("选择日期") }
            }
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && category.isNotBlank() && amt > 0.0) {
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                        val realAmount = if (type == "支出") -amt else amt
                        onAdd(
                            BillItem(
                                title = title,
                                category = category,
                                amount = realAmount,
                                remark = remark,
                                time = "$dateStr $timeStr",
                                payType = payType
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("保存") }
        }
        if (showDatePicker) {
            val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.time)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { date = Date(it) }
                        showDatePicker = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                },
                content = { DatePicker(state = pickerState) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillApp() {
    val context = LocalContext.current
    var bills by remember { mutableStateOf<List<BillItem>>(emptyList()) }
    var isLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var navIndex by rememberSaveable { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isDark by remember { mutableStateOf(false) }
    var isDarkLoaded by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("create") }
    var sortLoaded by remember { mutableStateOf(false) }

    // 分类/方式，实时监听 DataStore
    val expenseCategories by context.dataStore.data
        .map { it[PREF_KEY_EXP_CAT]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他") }
        .collectAsState(initial = listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他"))
    val incomeCategories by context.dataStore.data
        .map { it[PREF_KEY_INC_CAT]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: listOf("工资", "转账", "理财", "其他") }
        .collectAsState(initial = listOf("工资", "转账", "理财", "其他"))
    val expensePayTypes by context.dataStore.data
        .map { it[PREF_KEY_EXP_PAY]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: listOf("支付宝", "微信", "现金", "银行卡", "其他") }
        .collectAsState(initial = listOf("支付宝", "微信", "现金", "银行卡", "其他"))
    val incomePayTypes by context.dataStore.data
        .map { it[PREF_KEY_INC_PAY]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: listOf("工资", "转账", "理财", "其他") }
        .collectAsState(initial = listOf("工资", "转账", "理财", "其他"))
    val catPayLoaded = expenseCategories.isNotEmpty() && incomeCategories.isNotEmpty() && expensePayTypes.isNotEmpty() && incomePayTypes.isNotEmpty()

    val today = remember { Calendar.getInstance() }
    var currentYearMonth by remember {
        mutableStateOf(Pair(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1))
    }

    // 加载账单数据
    LaunchedEffect(Unit) {
        val json = context.dataStore.data.map { it[BILLS_KEY] ?: "" }.first()
        bills = if (json.isNotBlank()) {
            try {
                gson.fromJson(json, object : TypeToken<List<BillItem>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
        isLoaded = true
    }
    // 加载深色模式
    LaunchedEffect(Unit) {
        val dark = context.dataStore.data.map { it[PREF_KEY_DARK] ?: false }.first()
        isDark = dark
        isDarkLoaded = true
    }
    // 加载排序方式
    LaunchedEffect(Unit) {
        val sort = context.dataStore.data.map { it[PREF_KEY_SORT] ?: "create" }.first()
        sortBy = sort
        sortLoaded = true
    }

    fun saveBills(newBills: List<BillItem>) {
        bills = newBills
        scope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs[BILLS_KEY] = gson.toJson(newBills)
            }
        }
    }
    fun saveDarkMode(dark: Boolean) {
        isDark = dark
        scope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs[PREF_KEY_DARK] = dark
            }
        }
    }
    fun saveSortBy(sort: String) {
        sortBy = sort
        scope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs[PREF_KEY_SORT] = sort
            }
        }
    }

    if (!isLoaded || !isDarkLoaded || !sortLoaded || !catPayLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val filteredBills = remember(bills, currentYearMonth) {
        bills.filter { bill ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val cal = Calendar.getInstance()
            try {
                cal.time = sdf.parse(bill.time) ?: Date(0)
            } catch (e: Exception) {
                cal.time = Date(0)
            }
            cal.get(Calendar.YEAR) == currentYearMonth.first && (cal.get(Calendar.MONTH) + 1) == currentYearMonth.second
        }
    }

    val navController = rememberNavController()
    KeepingTheme(darkTheme = isDark) {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        when (navIndex) {
                            0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val (y, m) = currentYearMonth
                                    currentYearMonth = if (m == 1) Pair(y - 1, 12) else Pair(y, m - 1)
                                }) { Icon(Icons.Filled.ArrowBack, contentDescription = "上个月") }
                                Text("${currentYearMonth.first}年${currentYearMonth.second}月", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                IconButton(onClick = {
                                    val (y, m) = currentYearMonth
                                    currentYearMonth = if (m == 12) Pair(y + 1, 1) else Pair(y, m + 1)
                                }) { Icon(Icons.Filled.ArrowForward, contentDescription = "下个月") }
                            }
                            1 -> Text("收支趋势图表", fontWeight = FontWeight.Bold)
                            2 -> Text("设置", fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        if (navIndex == 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("排序:", fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Button(
                                    onClick = { saveSortBy(if (sortBy == "create") "date" else "create") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) { Text(if (sortBy == "create") "创建时间" else "账单日期", fontSize = 14.sp) }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = navIndex == 0, onClick = { navIndex = 0 }, icon = { Icon(Icons.Filled.List, contentDescription = "账单") }, label = { Text("账单") })
                            NavigationBarItem(selected = false, onClick = { navController.navigate("addBill") }, icon = { Icon(Icons.Filled.Add, contentDescription = "新增账单") }, label = { Text("新增") })
                    NavigationBarItem(selected = navIndex == 1, onClick = { navIndex = 1 }, icon = { Icon(Icons.Filled.BarChart, contentDescription = "图表") }, label = { Text("图表") })
                    NavigationBarItem(selected = navIndex == 2, onClick = { navIndex = 2 }, icon = { Icon(Icons.Filled.Settings, contentDescription = "设置") }, label = { Text("设置") })
                }
            }
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)) {
                when (navIndex) {
                    0 -> BillHomeScreen(
                        bills = filteredBills,
                        onEdit = { updated -> saveBills(bills.map { if (it.id == updated.id) updated else it }) },
                        onDelete = { del -> saveBills(bills.filter { it.id != del.id }) },
                        sortBy = sortBy,
                        expenseCategories = expenseCategories,
                        incomeCategories = incomeCategories,
                        expensePayTypes = expensePayTypes,
                        incomePayTypes = incomePayTypes
                    )
                    1 -> BillChartScreen(bills = filteredBills)
                    2 -> SettingsScreen(isDark = isDark, onDarkChange = { saveDarkMode(it) })
                }
                if (showAddDialog) {
                            // 保留原有弹窗编辑功能
                    AddBillDialog(
                        onAdd = {
                            saveBills(listOf(it) + bills)
                            showAddDialog = false
                            navIndex = 0
                        },
                        onDismiss = { showAddDialog = false },
                        expenseCategories = expenseCategories,
                        incomeCategories = incomeCategories,
                        expensePayTypes = expensePayTypes,
                        incomePayTypes = incomePayTypes
                    )
                }
                    }
                }
            }
            composable("addBill") {
                AddBillScreen(
                    onAdd = {
                        saveBills(listOf(it) + bills)
                        navController.popBackStack()
                        navIndex = 0
                    },
                    onBack = { navController.popBackStack() },
                    expenseCategories = expenseCategories,
                    incomeCategories = incomeCategories,
                    expensePayTypes = expensePayTypes,
                    incomePayTypes = incomePayTypes
                )
            }
        }
    }
}

// 新增：图表页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillChartScreen(bills: List<BillItem>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        BillBarChart(bills)
    }
}

@Composable
fun BillHomeScreen(
    bills: List<BillItem>,
    onEdit: (BillItem) -> Unit,
    onDelete: (BillItem) -> Unit,
    sortBy: String,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    expensePayTypes: List<String>,
    incomePayTypes: List<String>
) {
    var editBill by remember { mutableStateOf<BillItem?>(null) }
    val listState = rememberLazyListState()
    val prevCount = remember { mutableStateOf(bills.size) }
    val sortedBills = remember(bills, sortBy) {
        when (sortBy) {
            "create" -> bills.sortedByDescending { it.createTime }
            "date" -> bills.sortedByDescending {
                try { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.time)?.time ?: 0L } catch (e: Exception) { 0L }
            }
            else -> bills
        }
    }
    LaunchedEffect(bills.size) {
        if (bills.isNotEmpty() && bills.size > prevCount.value) {
            listState.scrollToItem(0)
        }
        prevCount.value = bills.size
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                val expense = bills.filter { it.amount < 0 }.sumOf { it.amount }
                val income = bills.filter { it.amount > 0 }.sumOf { it.amount }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("支出：", color = Color.Gray)
                    Text("¥${-expense}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("收入：", color = Color.Gray)
                    Text("¥$income", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        Text("账单列表", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(sortedBills, key = { it.id }) { bill ->
                BillRow(
                    bill = bill,
                    onDelete = { onDelete(bill) },
                    onEdit = { editBill = bill }
                )
            }
        }
    }
    if (editBill != null) {
        AddBillDialog(
            bill = editBill,
            onAdd = {
                onEdit(it)
                editBill = null
            },
            onDismiss = { editBill = null },
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            expensePayTypes = expensePayTypes,
            incomePayTypes = incomePayTypes
        )
    }
}

// 自定义高度和内容对齐完全一致的下拉输入框
@Composable
fun SuperDropdownField(
    value: String,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val shape = MaterialTheme.shapes.medium
    val labelStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp)

    Box(modifier = modifier) {
        Column {
            Text(
                text = label,
                style = labelStyle,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, borderColor, shape)
                    .clip(shape)
                    .clickable { onExpandedChange(!expanded) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = textStyle,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "展开",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

// 新增/编辑账单弹窗，支持传入初始bill
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillDialog(
    bill: BillItem? = null,
    onAdd: (BillItem) -> Unit,
    onDismiss: () -> Unit,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    expensePayTypes: List<String>,
    incomePayTypes: List<String>
) {
    // 判断初始类型
    val initialType = if ((bill?.amount ?: 0.0) >= 0) "收入" else "支出"
    var type by remember { mutableStateOf(initialType) }
    // 分类和方式下拉内容根据类型切换
    val categories = if (type == "支出") expenseCategories else incomeCategories
    val payTypes = if (type == "支出") expensePayTypes else incomePayTypes
    // 分类和方式初始值
    var category by remember { mutableStateOf(
        bill?.category?.takeIf { it in categories } ?: categories[0]
    ) }
    var payType by remember { mutableStateOf(
        bill?.payType?.takeIf { it in payTypes } ?: payTypes[0]
    ) }
    var expanded by remember { mutableStateOf(false) }
    var payTypeExpanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(bill?.title ?: "") }
    var amount by remember { mutableStateOf(bill?.amount?.let { abs(it).toString() } ?: "") }
    var remark by remember { mutableStateOf(bill?.remark ?: "") }
    // 日期选择
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    var date by remember {
        mutableStateOf(
            bill?.let {
                try {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.time) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
            } ?: Date()
        )
    }
    // 类型切换时重置分类和方式
    LaunchedEffect(type) {
        category = categories[0]
        payType = payTypes[0]
    }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        // 你可以继续自定义其他参数
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(if (bill == null) "新增账单" else "编辑账单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = textFieldColors,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Start,
                        lineHeight = 20.sp
                    )
                )
                // 分类下拉选择
                SuperDropdownField(
                        value = category,
                    label = "分类",
                        expanded = expanded,
                    onExpandedChange = { expanded = it },
                    items = categories,
                    onItemSelected = { category = it },
                    modifier = Modifier.fillMaxWidth()
                )
                // 收入/支出单选
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("类型：")
                    RadioButton(
                        selected = type == "支出",
                        onClick = { type = "支出" }
                    )
                    Text("支出")
                    RadioButton(
                        selected = type == "收入",
                        onClick = { type = "收入" }
                    )
                    Text("收入")
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = textFieldColors,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Start,
                        lineHeight = 20.sp
                    )
                )
                // 方式下拉选择
                SuperDropdownField(
                        value = payType,
                    label = if (type == "支出") "支付方式" else "收入方式",
                        expanded = payTypeExpanded,
                    onExpandedChange = { payTypeExpanded = it },
                    items = payTypes,
                    onItemSelected = { payType = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = textFieldColors,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Start,
                        lineHeight = 20.sp
                    )
                )
                // 日期选择按钮
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("日期：${dateFormat.format(date)}", modifier = Modifier.clickable { showDatePicker = true })
                    TextButton(onClick = { showDatePicker = true }, shape = MaterialTheme.shapes.medium, modifier = Modifier.height(40.dp)) { Text("选择日期") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && category.isNotBlank() && amt > 0.0) {
                        val timeStr = timeFormat.format(date)
                        val dateStr = dateFormat.format(date)
                        val realAmount = if (type == "支出") -amt else amt
                        onAdd(
                            BillItem(
                                title = title,
                                category = category,
                                amount = realAmount,
                                remark = remark,
                                time = "$dateStr $timeStr",
                                payType = payType,
                                createTime = bill?.createTime ?: System.currentTimeMillis(),
                                id = bill?.id ?: UUID.randomUUID().toString()
                            )
                        )
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(48.dp)
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = MaterialTheme.shapes.medium, modifier = Modifier.height(48.dp)) { Text("取消") }
        }
    )
    // 日期选择弹窗
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Date(it)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
            content = {
                DatePicker(state = pickerState)
            }
        )
    }
}

// 账单数据类，增加唯一id
data class BillItem(
    val title: String,
    val category: String,
    val amount: Double,
    val remark: String,
    val time: String, // 用户选择的账单日期
    val payType: String = "支付宝", // 支付方式
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val id: String = UUID.randomUUID().toString()
)

// 单条账单展示，增加删除和编辑
@Composable
fun BillRow(
    bill: BillItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    val categoryColor = when (bill.category) {
        "收入" -> CategoryGreen
        "餐饮" -> CategoryOrange
        "交通" -> CategoryBlue
        "购物" -> CategoryPurple
        "娱乐" -> CategoryTeal
        "医疗" -> CategoryRed
        else -> CategoryGrey
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp)
            .clickable { onEdit() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类色块
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = categoryColor,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(bill.category.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(bill.title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(bill.remark, color = Color.Gray, fontSize = 13.sp)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                (if (bill.amount > 0) "+" else "") + "¥" + bill.amount,
                color = if (bill.amount > 0) CategoryGreen else CategoryRed,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(bill.time, color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(20.dp), tint = CategoryRed)
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text("确认删除？") },
            text = { Text("确定要删除该账单吗？") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }, shape = MaterialTheme.shapes.medium, modifier = Modifier.height(40.dp)) { Text("确定", color = CategoryRed) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }, shape = MaterialTheme.shapes.medium, modifier = Modifier.height(40.dp)) { Text("取消") }
            }
        )
    }
}

@Composable
fun BillBarChart(bills: List<BillItem>) {
    // 聚合每日收入和支出
    val dayMap = mutableMapOf<String, Pair<Double, Double>>() // date -> (income, expense)
    bills.forEach {
        val day = it.time.substring(0, 10)
        val (income, expense) = dayMap[day] ?: (0.0 to 0.0)
        if (it.amount > 0) {
            dayMap[day] = (income + it.amount to expense)
        } else {
            dayMap[day] = (income to expense + -it.amount)
        }
    }
    val sortedDays = dayMap.keys.sorted()
    val incomeValues = sortedDays.map { dayMap[it]?.first ?: 0.0 }
    val expenseValues = sortedDays.map { dayMap[it]?.second ?: 0.0 }
    val max = (incomeValues + expenseValues).maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("每日收支趋势", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(8.dp)
                .then(Modifier.widthIn(min = 240.dp, max = 420.dp))
            ) {
                val groupCount = incomeValues.size
                val groupWidth = size.width / groupCount.coerceAtLeast(1)
                val barWidth = groupWidth / 3f
                for (idx in 0 until groupCount) {
                    // 支出柱（红色）
                    val expense = expenseValues[idx]
                    val expenseLeft = idx * groupWidth + barWidth * 0.5f
                    val expenseTop = size.height * (1f - (expense / max).toFloat())
                    drawRect(
                        color = Color(0xFFF44336),
                        topLeft = Offset(expenseLeft, expenseTop),
                        size = Size(barWidth, size.height - expenseTop)
                    )
                    // 收入柱（绿色）
                    val income = incomeValues[idx]
                    val incomeLeft = idx * groupWidth + barWidth * 1.5f
                    val incomeTop = size.height * (1f - (income / max).toFloat())
                    drawRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(incomeLeft, incomeTop),
                        size = Size(barWidth, size.height - incomeTop)
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            sortedDays.forEachIndexed { idx, day ->
                Text(day.takeLast(2) + "日", fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
            }
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            Box(Modifier.size(12.dp).background(Color(0xFFF44336), shape = MaterialTheme.shapes.small))
            Spacer(Modifier.width(4.dp))
            Text("支出", fontSize = 12.sp)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(12.dp).background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.small))
            Spacer(Modifier.width(4.dp))
            Text("收入", fontSize = 12.sp)
        }
    }
}