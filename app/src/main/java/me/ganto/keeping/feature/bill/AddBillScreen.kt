package me.ganto.keeping.feature.bill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.core.model.BillItem
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.animateContentSize
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.compose.ui.platform.LocalContext
import me.ganto.keeping.core.data.dataStore
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import me.ganto.keeping.core.util.ValidationUtils
import me.ganto.keeping.core.util.ErrorHandler
import me.ganto.keeping.core.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBillScreen(
    onAdd: (BillItem) -> Unit,
    onBack: () -> Unit,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    expensePayTypes: List<String>,
    incomePayTypes: List<String>
) {
    val context = LocalContext.current
    val TAB_INDEX_KEY = intPreferencesKey("add_bill_tab_index")
    val EXP_CAT_KEY = stringPreferencesKey("add_bill_expense_category")
    val INC_CAT_KEY = stringPreferencesKey("add_bill_income_category")
    val EXP_PAY_KEY = stringPreferencesKey("add_bill_expense_paytype")
    val INC_PAY_KEY = stringPreferencesKey("add_bill_income_paytype")
    val scope = rememberCoroutineScope()
    var tabIndex by remember { mutableStateOf(0) }
    // 恢复上次tabIndex
    LaunchedEffect(Unit) {
        val saved = context.dataStore.data.map { it[TAB_INDEX_KEY] ?: 0 }.first()
        tabIndex = saved
    }
    // 只更新本地状态
    fun selectTab(index: Int) {
        tabIndex = index
    }
    val type = if (tabIndex == 0) "支出" else "收入"
    // 记忆分类和方式
    var expenseCategory by remember { mutableStateOf(expenseCategories.getOrNull(0) ?: "") }
    var incomeCategory by remember { mutableStateOf(incomeCategories.getOrNull(0) ?: "") }
    var expensePayType by remember { mutableStateOf(expensePayTypes.getOrNull(0) ?: "") }
    var incomePayType by remember { mutableStateOf(incomePayTypes.getOrNull(0) ?: "") }
    // 恢复上次选择
    LaunchedEffect(expenseCategories) {
        val saved = context.dataStore.data.map { it[EXP_CAT_KEY] }.first()
        if (!saved.isNullOrBlank() && expenseCategories.contains(saved)) expenseCategory = saved
    }
    LaunchedEffect(incomeCategories) {
        val saved = context.dataStore.data.map { it[INC_CAT_KEY] }.first()
        if (!saved.isNullOrBlank() && incomeCategories.contains(saved)) incomeCategory = saved
    }
    LaunchedEffect(expensePayTypes) {
        val saved = context.dataStore.data.map { it[EXP_PAY_KEY] }.first()
        if (!saved.isNullOrBlank() && expensePayTypes.contains(saved)) expensePayType = saved
    }
    LaunchedEffect(incomePayTypes) {
        val saved = context.dataStore.data.map { it[INC_PAY_KEY] }.first()
        if (!saved.isNullOrBlank() && incomePayTypes.contains(saved)) incomePayType = saved
    }
    // 只更新本地状态
    fun selectExpenseCategory(cat: String) { expenseCategory = cat }
    fun selectIncomeCategory(cat: String) { incomeCategory = cat }
    fun selectExpensePayType(pay: String) { expensePayType = pay }
    fun selectIncomePayType(pay: String) { incomePayType = pay }
    val category = if (type == "支出") expenseCategory else incomeCategory
    val payType = if (type == "支出") expensePayType else incomePayType
    val categories = if (type == "支出") expenseCategories else incomeCategories
    val payTypes = if (type == "支出") expensePayTypes else incomePayTypes
    var remark by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var payTypeExpanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectTab(0) }
                    Box(
                        modifier = tabModifier
                            .background(if (tabIndex == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .animateContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "支出",
                            color = if (tabIndex == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (tabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    val tabModifier2 = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectTab(1) }
                    Box(
                        modifier = tabModifier2
                            .background(if (tabIndex == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .animateContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "收入",
                            color = if (tabIndex == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (tabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 分类选择平铺
            Text("分类", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.chunked(4).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { item ->
                            val selected = item == category
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.8f)
                                    .clickable {
                                        if (type == "支出") selectExpenseCategory(item) else selectIncomeCategory(item)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        // 如果这一行不足4个，用空的Spacer填充
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            // 方式选择平铺
            Spacer(modifier = Modifier.height(16.dp))
            Text("方式", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                payTypes.chunked(4).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { item ->
                            val selected = item == payType
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.8f)
                                    .clickable {
                                        if (type == "支出") selectExpensePayType(item) else selectIncomePayType(item)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        // 如果这一行不足4个，用空的Spacer填充
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            // 金额输入框
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    // 使用ValidationUtils的格式化函数
                    amount = ValidationUtils.formatAmountInput(input)
                },
                label = { Text("金额") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                ),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            // 备注输入框
            OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                label = { Text("备注") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                ),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    fontSize = 15.sp
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "日期：",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                IconButton(onClick = {
                    // 向前一天
                    val cal = Calendar.getInstance().apply { time = date }
                    cal.add(Calendar.DAY_OF_MONTH, -1)
                    date = cal.time
                }) {
                                                Icon(Icons.Filled.ArrowBack, contentDescription = "前一天")
                }
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date),
                    modifier = Modifier
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                IconButton(onClick = {
                    // 向后一天
                    val cal = Calendar.getInstance().apply { time = date }
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    date = cal.time
                }) {
                                                Icon(Icons.Filled.ArrowForward, contentDescription = "后一天")
                }
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
            Button(
                onClick = {
                    // 使用ValidationUtils进行数据验证
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                    val validationResults = ValidationUtils.validateBillItem(
                        category = category,
                        amount = amount,
                        date = dateStr,
                        remark = remark
                    )
                    
                    val hasError = validationResults.any { !it.isValid }
                    if (hasError) {
                        // 显示第一个错误信息
                        val firstError = validationResults.first { !it.isValid }
                        scope.launch {
                            ErrorHandler.handleValidationErrorMessage(context, firstError.errorMessage)
                        }
                        return@Button
                    }
                    
                    val amt = MoneyUtils.safeParseDouble(amount)
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                    val realAmount = if (type == "支出") -amt else amt
                    
                    // 保存tabIndex、分类、方式到DataStore
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[TAB_INDEX_KEY] = tabIndex
                            if (type == "支出") {
                                prefs[EXP_CAT_KEY] = expenseCategory
                                prefs[EXP_PAY_KEY] = expensePayType
                            } else {
                                prefs[INC_CAT_KEY] = incomeCategory
                                prefs[INC_PAY_KEY] = incomePayType
                            }
                        }
                    }
                    onAdd(
                        BillItem(
                            category = category,
                            amount = realAmount,
                            remark = remark,
                            time = "$dateStr $timeStr",
                            payType = payType
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("保存") }
        }
    }
} 