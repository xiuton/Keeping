package me.ganto.keeping.feature.bill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.core.model.BillItem
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.PrimaryTabRow

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
                    }
                )
                PrimaryTabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) { Text("支出") }
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) { Text("收入") }
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
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
            Spacer(modifier = Modifier.height(12.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
            // 金额输入框
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
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
                )
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