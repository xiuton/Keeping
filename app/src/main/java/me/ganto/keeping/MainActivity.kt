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
import me.ganto.keeping.feature.bill.AddBillScreen

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
            KeepingTheme(darkTheme = isDark, dynamicColor = true) {
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
}