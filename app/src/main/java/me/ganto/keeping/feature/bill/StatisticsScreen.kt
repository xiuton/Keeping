package me.ganto.keeping.feature.bill

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.core.util.MoneyUtils
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import java.util.Calendar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.CircleShape
// Vico图表库导入
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.FloatEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    bills: List<BillItem>,
    year: Int,
    month: Int,
    onYearMonthChange: (Pair<Int, Int>) -> Unit
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    var tempYear by remember { mutableStateOf(year) }
    var tempMonth by remember { mutableStateOf(month) }

    // 过滤出所选年月的账单
    val monthBills = remember(bills, year, month) {
        bills.filter {
            val parts = it.time.split(" ")[0].split("-")
            parts.size == 3 &&
            parts[0].toIntOrNull() == year &&
            parts[1].toIntOrNull() == month
        }
    }
    val totalIncome = monthBills.filter { it.amount > 0 }.sumOf { it.amount }
    val totalExpense = monthBills.filter { it.amount < 0 }.sumOf { it.amount }
    val balance = MoneyUtils.calculateBalance(totalIncome, totalExpense)
    val expenseByCategory = monthBills.filter { it.amount < 0 }.groupBy { it.category }.mapValues { it.value.sumOf { b -> -b.amount } }
    val incomeByCategory = monthBills.filter { it.amount > 0 }.groupBy { it.category }.mapValues { it.value.sumOf { b -> b.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 颜色指示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
            Spacer(Modifier.width(4.dp))
            Text("收入", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.error, shape = CircleShape)
            )
            Spacer(Modifier.width(4.dp))
            Text("支出", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // 柱状图
        Box(modifier = Modifier.border(1.dp, Color.Red, shape = RoundedCornerShape(8.dp))) {
            VicoBarChart(monthBills, year, month, chartHeight = 200.dp)
        }
        // 每日收支趋势标题
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("每日收支趋势", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        // 分类统计
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "支出分类占比",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
        )
        if (expenseByCategory.isNotEmpty()) {
            CategoryStatList(expenseByCategory, isExpense = true)
        } else {
            Text(
                "本月暂无支出",
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
            )
        }
        Text(
            "收入分类占比",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(top = 12.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
        )
        if (incomeByCategory.isNotEmpty()) {
            CategoryStatList(incomeByCategory, isExpense = false)
        } else {
            Text(
                "本月暂无收入",
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
            )
        }
    }
    // 年月选择弹窗
    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text("选择年月", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 年份选择
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("年份:", fontWeight = FontWeight.Medium, modifier = Modifier.width(60.dp))
                        IconButton(onClick = { tempYear-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一年")
                        }
                        Text(
                            text = tempYear.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = { tempYear++ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一年")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // 月份选择
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("月份:", fontWeight = FontWeight.Medium, modifier = Modifier.width(60.dp))
                        IconButton(onClick = { tempMonth = if (tempMonth == 1) 12 else tempMonth - 1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上个月")
                        }
                        Text(
                            text = tempMonth.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = { tempMonth = if (tempMonth == 12) 1 else tempMonth + 1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下个月")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onYearMonthChange(Pair(tempYear, tempMonth))
                    showMonthPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showMonthPicker = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun CategoryStatList(categoryMap: Map<String, Double>, isExpense: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
    ) {
        categoryMap.entries.sortedByDescending { it.value }.forEach { (cat, amt) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(cat, fontWeight = FontWeight.Medium)
                Text("¥${MoneyUtils.formatMoney(amt)}", color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun VicoBarChart(bills: List<BillItem>, year: Int, month: Int, chartHeight: Dp) {
    // 处理数据
    val dayMap = mutableMapOf<Int, Pair<Double, Double>>()
    bills.forEach {
        val parts = it.time.split(" ")[0].split("-")
        if (parts.size == 3) {
            val billYear = parts[0].toIntOrNull()
            val billMonth = parts[1].toIntOrNull()
            val billDay = parts[2].toIntOrNull()
            if (billYear == year && billMonth == month && billDay != null) {
                val (income, expense) = dayMap[billDay] ?: (0.0 to 0.0)
                if (it.amount > 0) {
                    dayMap[billDay] = (income + it.amount to expense)
                } else {
                    dayMap[billDay] = (income to expense + -it.amount)
                }
            }
        }
    }
    
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.YEAR, year)
    calendar.set(java.util.Calendar.MONTH, month - 1)
    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
    val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    
    // 创建收入数据
    val incomeData = mutableListOf<Pair<Number, Number>>()
    val expenseData = mutableListOf<Pair<Number, Number>>()
    
    for (day in 1..daysInMonth) {
        val (income, expense) = dayMap[day] ?: (0.0 to 0.0)
        incomeData.add(Pair(day, income))
        expenseData.add(Pair(day, expense))
    }
    
    // 使用LazyRow实现更好的柱状图
    LazyRow(
        modifier = Modifier
            .height(chartHeight + 40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items((1..daysInMonth).toList()) { day ->
            val (income, expense) = dayMap[day] ?: (0.0 to 0.0)
            val maxValue = maxOf(income, expense)
            val maxHeight = chartHeight.value - 20.dp.value
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.width(20.dp)
            ) {
                // 柱状图区域
                Row(
                    modifier = Modifier
                        .height(chartHeight)
                        .width(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 收入柱
                    if (income > 0) {
                        val incomeHeight = if (maxValue > 0) (maxHeight * (income / maxValue)).toFloat().coerceAtLeast(4f) else 4f
                        Box(
                            modifier = Modifier
                                .height(incomeHeight.dp)
                                .width(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    
                    // 支出柱
                    if (expense > 0) {
                        val expenseHeight = if (maxValue > 0) (maxHeight * (expense / maxValue)).toFloat().coerceAtLeast(4f) else 4f
                        Box(
                            modifier = Modifier
                                .height(expenseHeight.dp)
                                .width(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
                
                // 日期标签
                Text(
                    text = day.toString(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
} 