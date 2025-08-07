package me.ganto.keeping.core.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.core.util.ErrorHandler
import me.ganto.keeping.core.util.MoneyUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * MainActivity的ViewModel，负责管理应用的主要状态和数据
 */
class MainViewModel : ViewModel() {
    
    // 账单数据
    var bills by mutableStateOf<List<BillItem>>(emptyList())
        private set
    
    // 加载状态
    var isLoaded by mutableStateOf(false)
        private set
    
    // 主题模式
    var themeMode by mutableStateOf("auto")
        private set
    
    var themeModeLoaded by mutableStateOf(false)
        private set
    
    // 排序方式
    var sortBy by mutableStateOf("create")
        private set
    
    var sortLoaded by mutableStateOf(false)
        private set
    
    // 分类和支付方式数据
    var expenseCategories by mutableStateOf(DefaultValues.EXPENSE_CATEGORIES)
        private set
    
    var incomeCategories by mutableStateOf(DefaultValues.INCOME_CATEGORIES)
        private set
    
    var expensePayTypes by mutableStateOf(DefaultValues.EXPENSE_PAY_TYPES)
        private set
    
    var incomePayTypes by mutableStateOf(DefaultValues.INCOME_PAY_TYPES)
        private set
    
    // 预算数据
    var budget by mutableStateOf(0.0)
        private set
    
    private val gson = Gson()
    
    /**
     * 加载所有数据
     */
    fun loadAllData(context: Context) {
        viewModelScope.launch {
            loadBills(context)
            loadThemeMode(context)
            loadSortBy(context)
            loadCategories(context)
            loadBudget(context)
        }
    }
    
    /**
     * 加载账单数据
     */
    private suspend fun loadBills(context: Context) {
        ErrorHandler.safeExecute(
            context = context,
            operation = {
                val json = context.dataStore.data.map { it[BILLS_KEY] ?: "" }.first()
                bills = if (json.isNotBlank()) {
                    try {
                        gson.fromJson(json, object : TypeToken<List<BillItem>>() {}.type)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()
                isLoaded = true
            },
            errorMessage = "加载账单数据失败"
        )
    }
    
    /**
     * 加载主题模式
     */
    private suspend fun loadThemeMode(context: Context) {
        ErrorHandler.safeExecute(
            context = context,
            operation = {
                val mode = context.dataStore.data.map { it[PREF_KEY_THEME_MODE] ?: "auto" }.first()
                themeMode = mode
                themeModeLoaded = true
            },
            errorMessage = "加载主题设置失败"
        )
    }
    
    /**
     * 加载排序设置
     */
    private suspend fun loadSortBy(context: Context) {
        ErrorHandler.safeExecute(
            context = context,
            operation = {
                val sort = context.dataStore.data.map { it[PREF_KEY_SORT] ?: "create" }.first()
                sortBy = sort
                sortLoaded = true
            },
            errorMessage = "加载排序设置失败"
        )
    }
    
    /**
     * 加载分类和支付方式数据
     */
    private suspend fun loadCategories(context: Context) {
        ErrorHandler.safeExecute(
            context = context,
            operation = {
                expenseCategories = context.dataStore.data
                    .map { it[PREF_KEY_EXP_CAT]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: DefaultValues.EXPENSE_CATEGORIES }
                    .first()
                
                incomeCategories = context.dataStore.data
                    .map { it[PREF_KEY_INC_CAT]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: DefaultValues.INCOME_CATEGORIES }
                    .first()
                
                expensePayTypes = context.dataStore.data
                    .map { it[PREF_KEY_EXP_PAY]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: DefaultValues.EXPENSE_PAY_TYPES }
                    .first()
                
                incomePayTypes = context.dataStore.data
                    .map { it[PREF_KEY_INC_PAY]?.let { json -> gson.fromJson(json, object: TypeToken<List<String>>(){}.type) } ?: DefaultValues.INCOME_PAY_TYPES }
                    .first()
            },
            errorMessage = "加载分类数据失败"
        )
    }

    /**
     * 加载预算
     */
    fun loadBudget(context: Context) {
        viewModelScope.launch {
            val value = context.dataStore.data.map { it[budgetKey] ?: "0.0" }.first()
            budget = value.toDoubleOrNull() ?: 0.0
        }
    }

    /**
     * 保存预算
     */
    fun saveBudget(context: Context, value: Double) {
        budget = value
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs[budgetKey] = value.toString()
            }
        }
    }
    
    /**
     * 保存账单数据
     */
    fun saveBills(context: Context, newBills: List<BillItem>) {
        bills = newBills
        viewModelScope.launch(Dispatchers.IO) {
            ErrorHandler.safeExecute(
                context = context,
                operation = {
                    context.dataStore.edit { prefs ->
                        prefs[BILLS_KEY] = gson.toJson(newBills)
                    }
                },
                errorMessage = "保存账单失败，请重试"
            )
        }
    }
    
    /**
     * 保存主题模式
     */
    fun saveThemeMode(context: Context, mode: String) {
        themeMode = mode
        viewModelScope.launch(Dispatchers.IO) {
            ErrorHandler.safeExecute(
                context = context,
                operation = {
                    context.dataStore.edit { prefs ->
                        prefs[PREF_KEY_THEME_MODE] = mode
                    }
                },
                errorMessage = "保存主题模式设置失败，请重试"
            )
        }
    }
    
    /**
     * 保存排序设置
     */
    fun saveSortBy(context: Context, sort: String) {
        sortBy = sort
        viewModelScope.launch(Dispatchers.IO) {
            ErrorHandler.safeExecute(
                context = context,
                operation = {
                    context.dataStore.edit { prefs ->
                        prefs[PREF_KEY_SORT] = sort
                    }
                },
                errorMessage = "保存排序设置失败"
            )
        }
    }
    
    /**
     * 收集设置数据用于备份
     */
    fun collectSettingsData(): Map<String, String> {
        return mapOf(
            "expense_categories" to gson.toJson(expenseCategories),
            "income_categories" to gson.toJson(incomeCategories),
            "expense_pay_types" to gson.toJson(expensePayTypes),
            "income_pay_types" to gson.toJson(incomePayTypes),
            "theme_mode" to themeMode,
            "sort_by" to sortBy,
            "budget" to budget.toString()
        )
    }

    companion object {
        val budgetKey = stringPreferencesKey("budget")
    }
} 