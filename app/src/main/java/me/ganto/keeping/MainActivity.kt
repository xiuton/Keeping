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
import me.ganto.keeping.ui.theme.KeepingTheme
import androidx.compose.material3.MaterialTheme

// DataStore扩展
val Context.dataStore by preferencesDataStore(name = "bills")
val BILLS_KEY = stringPreferencesKey("bills_json")
val gson = Gson()
val PREF_KEY_DARK = booleanPreferencesKey("is_dark")
val PREF_KEY_SORT = stringPreferencesKey("sort_by")
val PREF_KEY_EXP_CAT = stringPreferencesKey("expense_categories")
val PREF_KEY_INC_CAT = stringPreferencesKey("income_categories")
val PREF_KEY_EXP_PAY = stringPreferencesKey("expense_pay_types")
val PREF_KEY_INC_PAY = stringPreferencesKey("income_pay_types")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeepingTheme {
                BillApp()
            }
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
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("设置", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("深色模式", fontSize = 16.sp)
            Spacer(Modifier.width(16.dp))
            Switch(checked = isDark, onCheckedChange = onDarkChange)
        }
        Spacer(Modifier.height(24.dp))
        // 类型单选框
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("类型：", fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            RadioButton(selected = type == "支出", onClick = { type = "支出" })
            Text("支出", fontSize = 15.sp)
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = type == "收入", onClick = { type = "收入" })
            Text("收入", fontSize = 15.sp)
        }
        Spacer(Modifier.height(16.dp))
        if (type == "支出") {
            Text("支出分类", fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                expenseCategories.forEach { cat ->
                    Box(
                        Modifier
                            .background(colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat, fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                            IconButton(onClick = { confirmDelete = "expense_cat" to cat }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newExpenseCategory, onValueChange = { newExpenseCategory = it }, label = { Text("新增支出分类") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                Button(onClick = {
                    if (newExpenseCategory.isNotBlank() && !expenseCategories.contains(newExpenseCategory)) {
                        addExpenseCategory(newExpenseCategory)
                        newExpenseCategory = ""
                    }
                }, modifier = Modifier.height(32.dp)) { Text("添加", fontSize = 13.sp) }
            }
            Spacer(Modifier.height(12.dp))
            Text("支出方式", fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                expensePayTypes.forEach { pt ->
                    Box(
                        Modifier
                            .background(colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pt, fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                            IconButton(onClick = { removeExpensePayType(pt) }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newExpensePayType, onValueChange = { newExpensePayType = it }, label = { Text("新增支出方式") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                Button(onClick = {
                    if (newExpensePayType.isNotBlank() && !expensePayTypes.contains(newExpensePayType)) {
                        addExpensePayType(newExpensePayType)
                        newExpensePayType = ""
                    }
                }, modifier = Modifier.height(32.dp)) { Text("添加", fontSize = 13.sp) }
            }
        } else {
            Text("收入分类", fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                incomeCategories.forEach { cat ->
                    Box(
                        Modifier
                            .background(colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat, fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                            IconButton(onClick = { removeIncomeCategory(cat) }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newIncomeCategory, onValueChange = { newIncomeCategory = it }, label = { Text("新增收入分类") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                Button(onClick = {
                    if (newIncomeCategory.isNotBlank() && !incomeCategories.contains(newIncomeCategory)) {
                        addIncomeCategory(newIncomeCategory)
                        newIncomeCategory = ""
                    }
                }, modifier = Modifier.height(32.dp)) { Text("添加", fontSize = 13.sp) }
            }
            Spacer(Modifier.height(12.dp))
            Text("收入方式", fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                incomePayTypes.forEach { pt ->
                    Box(
                        Modifier
                            .background(colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pt, fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                            IconButton(onClick = { removeIncomePayType(pt) }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newIncomePayType, onValueChange = { newIncomePayType = it }, label = { Text("新增收入方式") }, singleLine = true, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp))
                Button(onClick = {
                    if (newIncomePayType.isNotBlank() && !incomePayTypes.contains(newIncomePayType)) {
                        addIncomePayType(newIncomePayType)
                        newIncomePayType = ""
                    }
                }, modifier = Modifier.height(32.dp)) { Text("添加", fontSize = 13.sp) }
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

    KeepingTheme(darkTheme = isDark) {
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
                    NavigationBarItem(selected = false, onClick = { showAddDialog = true }, icon = { Icon(Icons.Filled.Add, contentDescription = "新增账单") }, label = { Text("新增") })
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
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                val expense = bills.filter { it.amount < 0 }.sumOf { it.amount }
                val income = bills.filter { it.amount > 0 }.sumOf { it.amount }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("支出：", color = Color.Gray)
                    Text("¥${-expense}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("收入：", color = Color.Gray)
                    Text("¥$income", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        Text("账单列表", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
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
    var amount by remember { mutableStateOf(bill?.amount?.let { kotlin.math.abs(it).toString() } ?: "") }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bill == null) "新增账单" else "编辑账单") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 分类下拉选择
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("分类") },
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "展开分类")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    category = it
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 收入/支出单选
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("类型：")
                    Spacer(Modifier.width(8.dp))
                    RadioButton(
                        selected = type == "支出",
                        onClick = { type = "支出" }
                    )
                    Text("支出")
                    Spacer(Modifier.width(8.dp))
                    RadioButton(
                        selected = type == "收入",
                        onClick = { type = "收入" }
                    )
                    Text("收入")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 方式下拉选择
                Box {
                    OutlinedTextField(
                        value = payType,
                        onValueChange = {},
                        label = { Text(if (type == "支出") "支付方式" else "收入方式") },
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { payTypeExpanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "展开方式")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { payTypeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = payTypeExpanded,
                        onDismissRequest = { payTypeExpanded = false }
                    ) {
                        payTypes.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    payType = it
                                    payTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 日期选择按钮
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("日期：${dateFormat.format(date)}", modifier = Modifier.clickable { showDatePicker = true })
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showDatePicker = true }) { Text("选择日期") }
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
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
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
                androidx.compose.material3.DatePicker(state = pickerState)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onEdit() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类图标（可扩展）
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = when (bill.category) {
                        "收入" -> Color(0xFF4CAF50)
                        "餐饮" -> Color(0xFFFF9800)
                        "交通" -> Color(0xFF2196F3)
                        "购物" -> Color(0xFF9C27B0)
                        "娱乐" -> Color(0xFF00BCD4)
                        "医疗" -> Color(0xFF8BC34A)
                        else -> Color.LightGray
                    },
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(bill.category.take(1), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(bill.title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(bill.remark, color = Color.Gray, fontSize = 13.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (bill.amount > 0) "+" else "") + "¥" + bill.amount,
                color = if (bill.amount > 0) Color(0xFF4CAF50) else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(bill.time, color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = { showConfirm = true }) {
            Text("删除", color = Color.Red)
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("确认删除？") },
            text = { Text("确定要删除该账单吗？") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) { Text("确定", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
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