package me.ganto.keeping.feature.bill

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.theme.CategoryBlue
import me.ganto.keeping.theme.CategoryGreen
import me.ganto.keeping.theme.CategoryGrey
import me.ganto.keeping.theme.CategoryOrange
import me.ganto.keeping.theme.CategoryPurple
import me.ganto.keeping.theme.CategoryRed
import me.ganto.keeping.theme.CategoryTeal
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BillHomeScreen(
    bills: List<BillItem>,
    onEdit: (BillItem) -> Unit,
    onDelete: (BillItem) -> Unit,
    sortBy: String,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    expensePayTypes: List<String>,
    incomePayTypes: List<String>,
    currentYearMonth: Pair<Int, Int>,
    onYearMonthChange: (Pair<Int, Int>) -> Unit,
    onSortChange: () -> Unit
) {
    var editBill by remember { mutableStateOf<BillItem?>(null) }
    var filterDate by remember { mutableStateOf<String?>(null) } // 当前筛选的日期，null为整月
    var listState: LazyListState by remember(filterDate) { mutableStateOf(LazyListState()) }
    val prevCount = remember { mutableStateOf(bills.size) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    val today = Calendar.getInstance()
    var lastSelectedDay by remember { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    // 切换月份时自动重置天切换
    LaunchedEffect(currentYearMonth) {
        filterDate = null
        lastSelectedDay = today.get(Calendar.DAY_OF_MONTH)
    }

    LaunchedEffect(filterDate) {
        val day = filterDate?.substring(8, 10)?.toIntOrNull()
        if (day != null) lastSelectedDay = day
    }

    val sortedBills = remember(bills, sortBy) {
        when (sortBy) {
            "create" -> bills.sortedByDescending { it.createTime }
            "date" -> bills.sortedByDescending {
                try { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.time)?.time ?: 0L } catch (e: Exception) { 0L }
            }
            else -> bills
        }
    }
    // 新增：根据当前年月和筛选日过滤账单
    val filteredBills = sortedBills.filter {
        val year = it.time.substring(0, 4).toIntOrNull()
        val month = it.time.substring(5, 7).toIntOrNull()
        val day = it.time.substring(8, 10).toIntOrNull()
        val matchMonth = year == currentYearMonth.first && month == currentYearMonth.second
        if (filterDate == null) {
            matchMonth
        } else {
            val filterDay = filterDate!!.substring(8, 10).toIntOrNull()
            matchMonth && day == filterDay
        }
    }
    LaunchedEffect(bills.size) {
        if (bills.isNotEmpty() && bills.size > prevCount.value) {
            listState.scrollToItem(0)
        }
        prevCount.value = bills.size
    }
    // 不再需要LaunchedEffect重置滚动，直接重建listState即可
    val scrollState = rememberScrollState()

    if (showDatePicker) {
        val today = Calendar.getInstance()
        val initialDate = filterDate?.let {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)?.time
            } catch (e: Exception) { null }
        } ?: today.timeInMillis
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        val y = cal.get(Calendar.YEAR)
                        val m = cal.get(Calendar.MONTH) + 1
                        val d = cal.get(Calendar.DAY_OF_MONTH)
                        filterDate = String.format("%04d-%02d-%02d", y, m, d)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 统计数据定义，确保作用域正确
        val expense = filteredBills.filter { it.amount < 0 }.sumOf { it.amount }
        val income = filteredBills.filter { it.amount > 0 }.sumOf { it.amount }
        // 新统计卡片区域
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(Modifier.padding(vertical = 24.dp, horizontal = 20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 支出
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.error, shape = CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("支出", color = MaterialTheme.colorScheme.error, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "¥${-expense}",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    }
                    // 收入
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, shape = CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("收入", color = MaterialTheme.colorScheme.tertiary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "¥$income",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    }
                }
                // 结余
                Spacer(Modifier.height(18.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("结余", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    Text(
                        text = "¥${income + expense}",
                        color = if (income + expense >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                }
            }
        }
        // 2. 标题与天切换同行，左右排布
        val year = currentYearMonth.first
        val month = currentYearMonth.second
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("账单列表", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            // 整月按钮，选中时主色边框+主色字体+加粗
            TextButton(
                onClick = {
                    if (filterDate == null) {
                        filterDate = String.format("%04d-%02d-%02d", year, month, lastSelectedDay)
                    } else {
                        filterDate = null
                    }
                },
                enabled = true,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = if (filterDate == null) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Text(
                    "整月",
                    fontWeight = if (filterDate == null) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // 日切换控件
            Row(verticalAlignment = Alignment.CenterVertically) {
                val calendar = remember(year, month) {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                // 左按钮
                IconButton(
                    enabled = lastSelectedDay > 1,
                    onClick = {
                        if (lastSelectedDay > 1) {
                            lastSelectedDay -= 1
                            filterDate = String.format("%04d-%02d-%02d", year, month, lastSelectedDay)
                        }
                    }
                ) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "前一天", modifier = Modifier.rotate(90f))
                }
                // 日切换按钮，始终高亮，内容为lastSelectedDay
                TextButton(
                    onClick = {
                        showDayPicker = true // 弹出日期选择器
                    },
                    enabled = true
                ) {
                    Text(
                        String.format("%02d-%02d", month, lastSelectedDay),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // 右按钮
                IconButton(
                    enabled = lastSelectedDay < maxDay,
                    onClick = {
                        if (lastSelectedDay < maxDay) {
                            lastSelectedDay += 1
                            filterDate = String.format("%04d-%02d-%02d", year, month, lastSelectedDay)
                        }
                    }
                ) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "后一天", modifier = Modifier.rotate(-90f))
                }
                // 日期选择器弹窗
                if (showDayPicker) {
                    val initialDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, lastSelectedDay)
                    }.timeInMillis
                    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
                    DatePickerDialog(
                        onDismissRequest = { showDayPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                pickerState.selectedDateMillis?.let { millis ->
                                    val cal = Calendar.getInstance().apply { timeInMillis = millis }
                                    val y = cal.get(Calendar.YEAR)
                                    val m = cal.get(Calendar.MONTH) + 1
                                    val d = cal.get(Calendar.DAY_OF_MONTH)
                                    lastSelectedDay = d
                                    filterDate = String.format("%04d-%02d-%02d", y, m, d)
                                }
                                showDayPicker = false
                            }) { Text("确定") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDayPicker = false }) { Text("取消") }
                        },
                        content = { DatePicker(state = pickerState) }
                    )
                }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(filteredBills, key = { _, bill -> bill.id }) { index, bill ->
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：分类色块+分类名
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(categoryColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    bill.category.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 备注（上，字体大）
                if (bill.remark.isNotBlank()) {
                    Text(
                        bill.remark,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                }
                // 分类和方式（下，字体小）
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        bill.category,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        bill.payType,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    (if (bill.amount > 0) "+" else "") + "¥" + String.format("%.2f", bill.amount),
                    color = if (bill.amount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    bill.time,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = { showConfirm = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text("确认删除？") },
            text = { Text("确定要删除该账单吗？") },
            confirmButton = {
                TextButton(
                    onClick = { showConfirm = false; onDelete() },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(40.dp)
                ) { Text("确定", color = CategoryRed) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirm = false },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(40.dp)
                ) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val initialType = if ((bill?.amount ?: 0.0) >= 0) "收入" else "支出"
    var type by remember { mutableStateOf(initialType) }
    val categories = if (type == "支出") expenseCategories else incomeCategories
    val payTypes = if (type == "支出") expensePayTypes else incomePayTypes
    var category by remember { mutableStateOf(bill?.category?.takeIf { it in categories } ?: categories[0]) }
    var payType by remember { mutableStateOf(bill?.payType?.takeIf { it in payTypes } ?: payTypes[0]) }
    var amount by remember { mutableStateOf(bill?.amount?.let { kotlin.math.abs(it).toString() } ?: "") }
    var remark by remember { mutableStateOf(bill?.remark ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    var date by remember {
        mutableStateOf(
            bill?.let {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).parse(it.time) ?: java.util.Date()
                } catch (e: Exception) {
                    java.util.Date()
                }
            } ?: java.util.Date()
        )
    }
    LaunchedEffect(type) {
        if (category !in categories) category = categories[0]
        if (payType !in payTypes) payType = payTypes[0]
    }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(if (bill == null) "新增账单" else "编辑账单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 分类选择
                Text("分类", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { item ->
                        val selected = item == category
                        Card(
                            modifier = Modifier
                                .widthIn(min = 64.dp, max = 120.dp)
                                .clickable { category = item },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(if (selected) 4.dp else 0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                // 方式选择
                Text("方式", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    payTypes.forEach { item ->
                        val selected = item == payType
                        Card(
                            modifier = Modifier
                                .widthIn(min = 64.dp, max = 120.dp)
                                .clickable { payType = item },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(if (selected) 4.dp else 0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                // 金额输入
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        // 只允许数字和小数点，且只允许一个小数点
                        val filtered = input.replace(Regex("[^\\d.]"), "")
                        val parts = filtered.split('.')
                        val newValue = when {
                            parts.size <= 2 -> {
                                // 不能以多个0开头
                                if (parts[0].startsWith("0") && parts[0].length > 1 && !parts[0].startsWith("0.")) {
                                    parts[0].trimStart('0').ifEmpty { "0" } + if (parts.size == 2) ".${parts[1]}" else ""
                                } else {
                                    filtered
                                }
                            }
                            else -> parts[0] + "." + parts[1]
                        }
                        amount = newValue
                    },
                    label = { Text("金额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Start
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                // 备注输入
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Start
                    )
                )
                // 日期选择
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("日期：${dateFormat.format(date)}")
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { showDatePicker = true }) { Text("选择日期") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (category.isNotBlank() && amt > 0.0) {
                        val timeStr = timeFormat.format(date)
                        val dateStr = dateFormat.format(date)
                        val realAmount = if (type == "支出") -amt else amt
                        onAdd(
                            BillItem(
                                category = category,
                                amount = realAmount,
                                remark = remark,
                                time = "$dateStr $timeStr",
                                payType = payType,
                                createTime = bill?.createTime ?: System.currentTimeMillis(),
                                id = bill?.id ?: java.util.UUID.randomUUID().toString()
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
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = java.util.Date(it) }
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