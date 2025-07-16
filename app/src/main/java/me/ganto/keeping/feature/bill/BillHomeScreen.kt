package me.ganto.keeping.feature.bill

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
import kotlin.math.abs

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