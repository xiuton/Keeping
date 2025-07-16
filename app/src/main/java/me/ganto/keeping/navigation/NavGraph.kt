@file:OptIn(ExperimentalMaterial3Api::class)
package me.ganto.keeping.navigation

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.ganto.keeping.feature.bill.AddBillScreen
import me.ganto.keeping.theme.KeepingTheme
import me.ganto.keeping.theme.CategoryGreen
import me.ganto.keeping.theme.CategoryRed
import me.ganto.keeping.theme.CategoryOrange
import me.ganto.keeping.theme.CategoryBlue
import me.ganto.keeping.theme.CategoryPurple
import me.ganto.keeping.theme.CategoryTeal
import me.ganto.keeping.theme.CategoryGrey
import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.feature.bill.BillChartScreen
import me.ganto.keeping.feature.bill.BillHomeScreen
import me.ganto.keeping.feature.settings.SettingsScreen
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

const val ROUTE_MAIN = "main"
const val ROUTE_ADD_BILL = "addBill"

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
    setShowAddDialog: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    KeepingTheme(darkTheme = isDark) {
        NavHost(navController = navController, startDestination = ROUTE_MAIN) {
            composable(ROUTE_MAIN) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                when (navIndex) {
                                    0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            // 这里需要 currentYearMonth 逻辑，可通过参数传递
                                        }) { Icon(Icons.Filled.ArrowBack, contentDescription = "上个月") }
                                        Text("账单", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                        IconButton(onClick = {
                                            // 这里需要 currentYearMonth 逻辑，可通过参数传递
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
                            NavigationBarItem(selected = navIndex == 0, onClick = { setNavIndex(0) }, icon = { Icon(Icons.Filled.List, contentDescription = "账单") }, label = { Text("账单") })
                            NavigationBarItem(selected = false, onClick = { navController.navigate(ROUTE_ADD_BILL) }, icon = { Icon(Icons.Filled.Add, contentDescription = "新增账单") }, label = { Text("新增") })
                            NavigationBarItem(selected = navIndex == 1, onClick = { setNavIndex(1) }, icon = { Icon(Icons.Filled.BarChart, contentDescription = "图表") }, label = { Text("图表") })
                            NavigationBarItem(selected = navIndex == 2, onClick = { setNavIndex(2) }, icon = { Icon(Icons.Filled.Settings, contentDescription = "设置") }, label = { Text("设置") })
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
                                incomePayTypes = incomePayTypes
                            )
                            1 -> BillChartScreen(bills = bills)
                            2 -> SettingsScreen(isDark = isDark, onDarkChange = { saveDarkMode(it) })
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
        }
    }
} 