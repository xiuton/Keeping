package me.ganto.keeping.feature.bill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.core.model.BillItem
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.FlowRow
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
    var tabIndex by remember { mutableStateOf(0) } // 0: 支出, 1: 收入
    val type = if (tabIndex == 0) "支出" else "收入"
    val categories = if (type == "支出") expenseCategories else incomeCategories
    val payTypes = if (type == "支出") expensePayTypes else incomePayTypes

    var expenseCategory by remember { mutableStateOf(expenseCategories.getOrNull(0) ?: "") }
    var incomeCategory by remember { mutableStateOf(incomeCategories.getOrNull(0) ?: "") }
    var expensePayType by remember { mutableStateOf(expensePayTypes.getOrNull(0) ?: "") }
    var incomePayType by remember { mutableStateOf(incomePayTypes.getOrNull(0) ?: "") }
    val category = if (type == "支出") expenseCategory else incomeCategory
    val payType = if (type == "支出") expensePayType else incomePayType
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { tabIndex = 0 }
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
                        .clickable { tabIndex = 1 }
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
                            .clickable {
                                if (type == "支出") expenseCategory = item else incomeCategory = item
                            },
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
            // 方式选择平铺
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
                            .clickable {
                                if (type == "支出") expensePayType = item else incomePayType = item
                            },
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
            // 金额输入框
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
                Text("日期：${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)}")
                Spacer(Modifier.width(8.dp))
                Button(onClick = { showDatePicker = true }) { Text("选择日期") }
            }
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (category.isNotBlank() && amt > 0.0) {
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                        val realAmount = if (type == "支出") -amt else amt
                        onAdd(
                            BillItem(
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