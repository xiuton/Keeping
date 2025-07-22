@file:OptIn(ExperimentalLayoutApi::class)
package me.ganto.keeping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import me.ganto.keeping.theme.KeepingTheme
import me.ganto.keeping.navigation.NavGraph
import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.core.data.PREF_KEY_EXP_CAT
import me.ganto.keeping.core.data.PREF_KEY_INC_CAT
import me.ganto.keeping.core.data.PREF_KEY_EXP_PAY
import me.ganto.keeping.core.data.PREF_KEY_INC_PAY
import me.ganto.keeping.core.data.dataStore
import me.ganto.keeping.core.data.BackupManager
import me.ganto.keeping.core.data.DefaultValues
import androidx.core.view.WindowCompat

// DataStore扩展
val BILLS_KEY = stringPreferencesKey("bills_json")
val gson = Gson()
val PREF_KEY_DARK = booleanPreferencesKey("is_dark")
val PREF_KEY_SORT = stringPreferencesKey("sort_by")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 允许内容延伸到系统栏下方
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
            val backupManager = remember { BackupManager(context) }
            val expenseCategories by context.dataStore.data
                .map { it[PREF_KEY_EXP_CAT]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: DefaultValues.EXPENSE_CATEGORIES }
                .collectAsState(initial = DefaultValues.EXPENSE_CATEGORIES)
            val incomeCategories by context.dataStore.data
                .map { it[PREF_KEY_INC_CAT]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: DefaultValues.INCOME_CATEGORIES }
                .collectAsState(initial = DefaultValues.INCOME_CATEGORIES)
            val expensePayTypes by context.dataStore.data
                .map { it[PREF_KEY_EXP_PAY]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: DefaultValues.EXPENSE_PAY_TYPES }
                .collectAsState(initial = DefaultValues.EXPENSE_PAY_TYPES)
            val incomePayTypes by context.dataStore.data
                .map { it[PREF_KEY_INC_PAY]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: DefaultValues.INCOME_PAY_TYPES }
                .collectAsState(initial = DefaultValues.INCOME_PAY_TYPES)
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
            
            // 收集所有设置数据用于备份
            fun collectSettingsData(): Map<String, String> {
                return mapOf(
                    "expense_categories" to gson.toJson(expenseCategories),
                    "income_categories" to gson.toJson(incomeCategories),
                    "expense_pay_types" to gson.toJson(expensePayTypes),
                    "income_pay_types" to gson.toJson(incomePayTypes),
                    "is_dark" to isDark.toString(),
                    "sort_by" to sortBy
                )
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
                    setShowAddDialog = { showAddDialog = it },
                    backupManager = backupManager,
                    collectSettingsData = ::collectSettingsData
                )
            }
        }
    }
}