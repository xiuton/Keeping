package me.ganto.keeping.feature.bill

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.core.ui.SuperDropdownField
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
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

    var category by remember { mutableStateOf(categories[0]) }
    var payType by remember { mutableStateOf(payTypes[0]) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var payTypeExpanded by remember { mutableStateOf(false) }
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
                    }
                )
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) { Text("支出") }
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) { Text("收入") }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = textFieldColors,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp
                )
            )
            SuperDropdownField(
                value = category,
                label = "分类",
                expanded = expanded,
                onExpandedChange = { expanded = it },
                items = categories,
                onItemSelected = { category = it },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("金额") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = textFieldColors,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp
                )
            )
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
                colors = textFieldColors,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp
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
                    if (title.isNotBlank() && category.isNotBlank() && amt > 0.0) {
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                        val realAmount = if (type == "支出") -amt else amt
                        onAdd(
                            BillItem(
                                title = title,
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