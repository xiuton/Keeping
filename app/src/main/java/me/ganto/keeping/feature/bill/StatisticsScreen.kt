package me.ganto.keeping.feature.bill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.core.model.BillItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    val balance = totalIncome + totalExpense
    val expenseByCategory = monthBills.filter { it.amount < 0 }.groupBy { it.category }.mapValues { it.value.sumOf { b -> -b.amount } }
    val incomeByCategory = monthBills.filter { it.amount > 0 }.groupBy { it.category }.mapValues { it.value.sumOf { b -> b.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 颜色指示
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF4CAF50), shape = CircleShape)
            )
            Spacer(Modifier.width(4.dp))
            Text("收入", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFFF44336), shape = CircleShape)
            )
            Spacer(Modifier.width(4.dp))
            Text("支出", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 柱状图
        Box {
            LazyBarChart(monthBills, year, month, chartHeight = 200.dp)
        }
        // 每日收支趋势标题
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("每日收支趋势", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        // 分类统计
        Text("支出分类占比", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        if (expenseByCategory.isNotEmpty()) {
            CategoryStatList(expenseByCategory, isExpense = true)
        } else {
            Text("本月暂无支出", color = MaterialTheme.colorScheme.outline)
        }
        Text("收入分类占比", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        if (incomeByCategory.isNotEmpty()) {
            CategoryStatList(incomeByCategory, isExpense = false)
        } else {
            Text("本月暂无收入", color = MaterialTheme.colorScheme.outline)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categoryMap.entries.sortedByDescending { it.value }.forEach { (cat, amt) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(cat, fontWeight = FontWeight.Medium)
                Text("¥%.2f".format(amt), color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun LazyBarChart(bills: List<BillItem>, year: Int, month: Int, chartHeight: Dp) {
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
    val incomeValues = (1..daysInMonth).map { dayMap[it]?.first ?: 0.0 }
    val expenseValues = (1..daysInMonth).map { dayMap[it]?.second ?: 0.0 }
    val max = (incomeValues + expenseValues).maxOrNull()?.takeIf { it > 0 } ?: 1.0

    LazyRow(
        modifier = Modifier.height(chartHeight + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        items((1..daysInMonth).toList()) { idx ->
            val income = incomeValues[idx - 1]
            val expense = expenseValues[idx - 1]

            val expenseStr = expense.toInt().toString()
            val incomeStr = income.toInt().toString()
            val barWidth = 15.dp // 统一设置固定宽度

            val expenseHeight = if (expense > 0) (chartHeight * (expense / max).toFloat()).coerceAtLeast(4.dp) else chartHeight
            val incomeHeight = if (income > 0) (chartHeight * (income / max).toFloat()).coerceAtLeast(4.dp) else chartHeight

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // 柱状图区域
                Row(
                    modifier = Modifier.height(chartHeight), // 固定柱状图区域高度
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val textHeightThreshold = 30.dp
                    val maxHeight = maxOf(incomeHeight, expenseHeight)
                    val balanceHeight = kotlin.math.abs(incomeHeight.value - expenseHeight.value).dp
                    val isIncomeHigher = incomeHeight > expenseHeight

                    // Canvas for Income Pillar
                    Box(modifier = Modifier.height(maxHeight)) {
                        // Bar Layer
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (income > 0) {
                                if (!isIncomeHigher && expense > 0) {
                                    // 结余柱
                                    Box(
                                        modifier = Modifier
                                            .height(balanceHeight)
                                            .width(barWidth)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF44336).copy(alpha = 0.7f))
                                    )
                                }
                                // 收入柱
                                Box(
                                    modifier = Modifier
                                        .height(incomeHeight)
                                        .width(barWidth)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF4CAF50))
                                )
                            } else {
                                // 占位符
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(barWidth)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEEEEEE).copy(alpha = 0.4f))
                                )
                            }
                        }

                        // Text Overlay Layer
                        if (income > 0) {
                            if (incomeHeight >= textHeightThreshold) {
                                // Text inside bar
                                Text(
                                    text = incomeStr,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .rotate(-90f),
                                    textDecoration = TextDecoration.Underline
                                )
                            } else {
                                // Text on top of bar
                                Text(
                                    text = incomeStr,
                                color = Color(0xFF4CAF50),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = -(incomeHeight + 4.dp))
                                        .rotate(-45f),
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                            // Balance Text
                            if (!isIncomeHigher && expense > 0) {
                                if (balanceHeight >= textHeightThreshold) {
                                    Text(
                                        text = "-${balanceHeight.value.toInt()}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .rotate(-90f),
                                        textDecoration = TextDecoration.Underline
                                    )
                                } else {
                                    Text(
                                        text = "-${balanceHeight.value.toInt()}",
                                        color = Color(0xFFF44336),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = -(balanceHeight + 4.dp))
                                            .rotate(-45f),
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                            }
                        }
                    }

                    // Canvas for Expense Pillar
                    Box(modifier = Modifier.height(maxHeight)) {
                        // Bar Layer
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (expense > 0) {
                                if (isIncomeHigher && income > 0) {
                                    // 结余柱
                                    Box(
                                        modifier = Modifier
                                            .height(balanceHeight)
                                            .width(barWidth)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.7f))
                                    )
                                }
                                // 支出柱
                                Box(
                                    modifier = Modifier
                                        .height(expenseHeight)
                                        .width(barWidth)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF44336))
                                )
                            } else {
                                // 占位符
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(barWidth)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFBDBDBD).copy(alpha = 0.1f))
                                )
                            }
                        }

                        // Text Overlay Layer
                        if (expense > 0) {
                            if (expenseHeight >= textHeightThreshold) {
                                // Text inside bar
                                Text(
                                    text = expenseStr,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .rotate(-90f),
                                    textDecoration = TextDecoration.Underline
                                )
                            } else {
                                // Text on top of bar
                                Text(
                                    text = expenseStr,
                                    color = Color(0xFFF44336),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = -(expenseHeight + 4.dp))
                                        .rotate(-45f),
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                            // Balance Text
                            if (isIncomeHigher && income > 0) {
                                if (balanceHeight >= textHeightThreshold) {
                                    Text(
                                        text = "+${balanceHeight.value.toInt()}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .rotate(-90f),
                                        textDecoration = TextDecoration.Underline
                                    )
                                } else {
                                    Text(
                                        text = "+${balanceHeight.value.toInt()}",
                            color = Color(0xFF4CAF50),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = -(balanceHeight + 4.dp))
                                            .rotate(-45f),
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                // 天数（底部对齐）
                Text(
                    "$idx",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
} 