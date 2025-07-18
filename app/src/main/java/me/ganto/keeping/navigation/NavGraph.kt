@file:OptIn(ExperimentalMaterial3Api::class)
package me.ganto.keeping.navigation

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.ganto.keeping.feature.bill.AddBillScreen
import me.ganto.keeping.theme.KeepingTheme
import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.feature.bill.StatisticsScreen
import me.ganto.keeping.feature.bill.BillHomeScreen
import me.ganto.keeping.feature.settings.SettingsScreen
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import me.ganto.keeping.feature.my.MyScreen
import me.ganto.keeping.core.data.BackupManager
import me.ganto.keeping.feature.feedback.FeedbackScreen
import me.ganto.keeping.feature.test.TestScreen
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import kotlinx.coroutines.launch

const val ROUTE_MAIN = "main"
const val ROUTE_ADD_BILL = "addBill"
const val ROUTE_FEEDBACK = "feedback"
const val ROUTE_TEST = "test"

@Composable
fun NavGraph(
    bills: List<BillItem>,
    saveBills: (List<BillItem>) -> Unit,
    isDark: Boolean,
    saveDarkMode: (Boolean) -> Unit,
    sortBy: String,
    saveSortBy: (String) -> Unit,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    expensePayTypes: List<String>,
    incomePayTypes: List<String>,
    navIndex: Int,
    setNavIndex: (Int) -> Unit,
    showAddDialog: Boolean,
    setShowAddDialog: (Boolean) -> Unit,
    backupManager: BackupManager,
    collectSettingsData: () -> Map<String, String>
) {
    val navController = rememberNavController()
    // Add state for currentYearMonth as Pair<Int, Int>
    val today = java.util.Calendar.getInstance()
    val currentYearMonth = rememberSaveable { mutableStateOf(Pair(today.get(java.util.Calendar.YEAR), today.get(java.util.Calendar.MONTH) + 1)) }
    val onYearMonthChange: (Pair<Int, Int>) -> Unit = { currentYearMonth.value = it }
    // 新增：日期选择弹窗状态
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    KeepingTheme(darkTheme = isDark) {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var lastBackPress by remember { mutableStateOf(0L) }
        // 首页返回时弹提示，再次返回才退出
        BackHandler(enabled = true) {
            if (navIndex != 0) {
                setNavIndex(0)
            } else {
                val now = System.currentTimeMillis()
                if (now - lastBackPress < 2000) {
                    // 2秒内再次返回，退出应用
                    android.os.Process.killProcess(android.os.Process.myPid())
                } else {
                    lastBackPress = now
                    scope.launch {
                        snackbarHostState.showSnackbar("再按一次退出Keeping")
                    }
                }
            }
        }
        // 日期选择弹窗（全局放在Scaffold外层）
        if (showDatePicker) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse("${currentYearMonth.value.first}-${currentYearMonth.value.second.toString().padStart(2, '0')}-01")?.time
                } catch (e: Exception) { null }
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                            onYearMonthChange(Pair(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1))
                        }
                        showDatePicker = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                },
                content = { DatePicker(state = pickerState) }
            )
        }
        Box(Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = ROUTE_MAIN) {
                composable(ROUTE_MAIN) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    when (navIndex) {
                                        0 -> Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            IconButton(onClick = {
                                                val (y, m) = currentYearMonth.value
                                                onYearMonthChange(if (m == 1) Pair(y - 1, 12) else Pair(y, m - 1))
                                            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上个月") }
                                            Text(
                                                text = "${currentYearMonth.value.first}年${currentYearMonth.value.second}月",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                modifier = Modifier.clickable { showDatePicker = true },
                                                textAlign = TextAlign.Center
                                            )
                                            IconButton(onClick = {
                                                val (y, m) = currentYearMonth.value
                                                onYearMonthChange(if (m == 12) Pair(y + 1, 1) else Pair(y, m + 1))
                                            }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下个月") }
                                        }
                                        1 -> Text("统计", fontWeight = FontWeight.Bold)
                                        2 -> Text("设置", fontWeight = FontWeight.Bold)
                                        3 -> Text("我的", fontWeight = FontWeight.Bold)
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
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .padding(end = 20.dp)
                                            ) {
                                                Text(if (sortBy == "create") "创建时间" else "账单日期", fontSize = 14.sp)
                                            }
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = when (navIndex) {
                                        0 -> MaterialTheme.colorScheme.primaryContainer
                                        1 -> MaterialTheme.colorScheme.tertiaryContainer
                                        2 -> MaterialTheme.colorScheme.secondaryContainer
                                        3 -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(selected = navIndex == 0, onClick = { setNavIndex(0) }, icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "账单") }, label = { Text("账单") })
                                NavigationBarItem(selected = navIndex == 1, onClick = { setNavIndex(1) }, icon = { Icon(Icons.Filled.BarChart, contentDescription = "统计") }, label = { Text("统计") })
                                NavigationBarItem(selected = false, onClick = { navController.navigate(ROUTE_ADD_BILL) }, icon = { Icon(Icons.Filled.Add, contentDescription = "新增账单") }, label = { Text("新增") })
                                NavigationBarItem(selected = navIndex == 2, onClick = { setNavIndex(2) }, icon = { Icon(Icons.Filled.Settings, contentDescription = "设置") }, label = { Text("设置") })
                                NavigationBarItem(selected = navIndex == 3, onClick = { setNavIndex(3) }, icon = { Icon(Icons.Filled.Person, contentDescription = "我的") }, label = { Text("我的") })
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)) {
                            when (navIndex) {
                                0 -> BillHomeScreen(
                                    bills = bills,
                                    onEdit = { updated -> saveBills(bills.map { if (it.id == updated.id) updated else it }) },
                                    onDelete = { del -> saveBills(bills.filter { it.id != del.id }) },
                                    sortBy = sortBy,
                                    expenseCategories = expenseCategories,
                                    incomeCategories = incomeCategories,
                                    expensePayTypes = expensePayTypes,
                                    incomePayTypes = incomePayTypes,
                                    currentYearMonth = currentYearMonth.value,
                                    onYearMonthChange = onYearMonthChange,
                                    onSortChange = { saveSortBy(if (sortBy == "create") "date" else "create") }
                                )
                                1 -> StatisticsScreen(bills = bills)
                                2 -> SettingsScreen(
                                    isDark = isDark, 
                                    onDarkChange = { saveDarkMode(it) },
                                    backupManager = backupManager,
                                    collectSettingsData = collectSettingsData,
                                    bills = bills,
                                    saveBills = saveBills
                                )
                                3 -> MyScreen(isDark = isDark, onDarkChange = { saveDarkMode(it) }, navController = navController)
                            }
                            if (showAddDialog) {
                                // 保留原有弹窗编辑功能
                                AddBillScreen(
                                    onAdd = {
                                        saveBills(listOf(it) + bills)
                                        setShowAddDialog(false)
                                        setNavIndex(0)
                                    },
                                    onBack = { setShowAddDialog(false) },
                                    expenseCategories = expenseCategories,
                                    incomeCategories = incomeCategories,
                                    expensePayTypes = expensePayTypes,
                                    incomePayTypes = incomePayTypes
                                )
                            }
                        }
                    }
                }
                composable(ROUTE_ADD_BILL) {
                    AddBillScreen(
                        onAdd = {
                            saveBills(listOf(it) + bills)
                            navController.popBackStack()
                            setNavIndex(0)
                        },
                        onBack = { navController.popBackStack() },
                        expenseCategories = expenseCategories,
                        incomeCategories = incomeCategories,
                        expensePayTypes = expensePayTypes,
                        incomePayTypes = incomePayTypes
                    )
                }
                composable(ROUTE_FEEDBACK) {
                    FeedbackScreen(onBack = { navController.popBackStack() })
                }
                composable(ROUTE_TEST) {
                    TestScreen(onBack = { navController.popBackStack() })
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
        }
    }
} 