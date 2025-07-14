package me.ganto.keeping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.ui.theme.KeepingTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeepingTheme {
                HomeScreen()
            }
        }
    }
}

// 记账APP首页
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // 账单数据用可变状态保存
    var bills by remember { mutableStateOf(
        listOf(
            BillItem("早餐", "餐饮", -12.0, "包子豆浆", "08:30"),
            BillItem("工资", "收入", 5000.0, "6月工资", "09:00"),
            BillItem("地铁", "交通", -3.0, "上班", "08:50"),
            BillItem("水果", "购物", -20.0, "西瓜", "18:20"),
        )
    ) }
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记账本", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加账单")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 总览
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("本月收支", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val expense = bills.filter { it.amount < 0 }.sumOf { it.amount }
                        val income = bills.filter { it.amount > 0 }.sumOf { it.amount }
                        Text("支出：", color = Color.Gray)
                        Text("¥${-expense}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("收入：", color = Color.Gray)
                        Text("¥$income", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            // 账单列表
            Text("今日账单", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(bills) { bill ->
                    BillRow(bill)
                }
            }
        }
        // 新增账单弹窗
        if (showDialog) {
            AddBillDialog(
                onAdd = { newBill ->
                    bills = listOf(newBill) + bills // 新账单置顶
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }
    }
}

// 新增账单弹窗
@Composable
fun AddBillDialog(onAdd: (BillItem) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增账单") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类，如餐饮/收入/交通") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额，支出为负，收入为正") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && category.isNotBlank() && amt != 0.0) {
                        onAdd(BillItem(title, category, amt, remark, now))
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 账单数据类
data class BillItem(val title: String, val category: String, val amount: Double, val remark: String, val time: String)

// 单条账单展示
@Composable
fun BillRow(bill: BillItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类图标（可扩展）
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = when (bill.category) {
                        "收入" -> Color(0xFF4CAF50)
                        "餐饮" -> Color(0xFFFF9800)
                        "交通" -> Color(0xFF2196F3)
                        "购物" -> Color(0xFF9C27B0)
                        else -> Color.LightGray
                    },
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(bill.category.take(1), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(bill.title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(bill.remark, color = Color.Gray, fontSize = 13.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (bill.amount > 0) "+" else "") + "¥" + bill.amount,
                color = if (bill.amount > 0) Color(0xFF4CAF50) else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(bill.time, color = Color.Gray, fontSize = 12.sp)
        }
    }
}