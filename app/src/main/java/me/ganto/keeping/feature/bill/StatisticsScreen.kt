package me.ganto.keeping.feature.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.core.model.BillItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(bills: List<BillItem>) {
    val scrollState = rememberScrollState()
    val monthBills = bills // 假设传入的已按月过滤
    val totalIncome = monthBills.filter { it.amount > 0 }.sumOf { it.amount }
    val totalExpense = monthBills.filter { it.amount < 0 }.sumOf { it.amount }
    val balance = totalIncome + totalExpense
    val expenseByCategory = monthBills.filter { it.amount < 0 }.groupBy { it.category }.mapValues { it.value.sumOf { b -> -b.amount } }
    val incomeByCategory = monthBills.filter { it.amount > 0 }.groupBy { it.category }.mapValues { it.value.sumOf { b -> b.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 每日收支趋势
        BillBarChart(monthBills)
        // 每日收支趋势标题，移动到图表下方居中
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
fun BillBarChart(bills: List<BillItem>) {
    // 聚合每日收入和支出
    val dayMap = mutableMapOf<String, Pair<Double, Double>>() // date -> (income, expense)
    bills.forEach {
        val day = it.time.substring(0, 10)
        val (income, expense) = dayMap[day] ?: (0.0 to 0.0)
        if (it.amount > 0) {
            dayMap[day] = (income + it.amount to expense)
        } else {
            dayMap[day] = (income to expense + -it.amount)
        }
    }
    val sortedDays = dayMap.keys.sorted()
    val incomeValues = sortedDays.map { dayMap[it]?.first ?: 0.0 }
    val expenseValues = sortedDays.map { dayMap[it]?.second ?: 0.0 }
    val max = (incomeValues + expenseValues).maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val groupCount = sortedDays.size
    val barMinWidth = 48.dp // 每天最小宽度
    val chartWidth = if (groupCount > 6) barMinWidth * groupCount else Modifier.fillMaxWidth()
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val groupGapPx = with(LocalDensity.current) { 12.dp.toPx() }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (groupCount > 6) {
                val scrollState = rememberScrollState()
                Row(Modifier.horizontalScroll(scrollState)) {
                    Canvas(modifier = Modifier
                        .width(barMinWidth * groupCount)
                        .height(180.dp)
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 16.dp, top = 8.dp)
                    ) {
                        val groupWidth = (size.width - groupGapPx * (groupCount - 1)) / groupCount
                        val barWidth = groupWidth / 2f
                        for (idx in 0 until groupCount) {
                            val groupStart = idx * (groupWidth + groupGapPx)
                            // 支出柱（红色）
                            val expense = expenseValues[idx]
                            val expenseLeft = groupStart
                            val expenseTop = size.height * (1f - (expense / max).toFloat())
                            drawRect(
                                color = Color(0xFFF44336),
                                topLeft = Offset(expenseLeft, expenseTop),
                                size = Size(barWidth, size.height - expenseTop)
                            )
                            // 支出数值
                            if (expense > 0) {
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.parseColor("#F44336")
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        textSize = 24f
                                        isFakeBoldText = true
                                    }
                                    canvas.nativeCanvas.drawText(
                                        "%.0f".format(expense),
                                        expenseLeft + barWidth / 2,
                                        expenseTop - 8f,
                                        paint
                                    )
                                }
                            }
                            // 收入柱（绿色）
                            val income = incomeValues[idx]
                            val incomeLeft = groupStart + barWidth
                            val incomeTop = size.height * (1f - (income / max).toFloat())
                            drawRect(
                                color = Color(0xFF4CAF50),
                                topLeft = Offset(incomeLeft, incomeTop),
                                size = Size(barWidth, size.height - incomeTop)
                            )
                            // 收入数值
                            if (income > 0) {
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.parseColor("#4CAF50")
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        textSize = 24f
                                        isFakeBoldText = true
                                    }
                                    canvas.nativeCanvas.drawText(
                                        "%.0f".format(income),
                                        incomeLeft + barWidth / 2,
                                        incomeTop - 8f,
                                        paint
                                    )
                                }
                            }
                        }
                        // x轴日期标签
                        for (idx in 0 until groupCount) {
                            val label = sortedDays[idx].takeLast(2) + "日"
                            val x = idx * groupWidth + groupWidth / 2
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    textSize = 28f
                                }
                                canvas.nativeCanvas.drawText(
                                    label,
                                    x,
                                    size.height + 38f, // 向下多10像素
                                    paint
                                )
                            }
                        }
                    }
                }
            } else {
                Canvas(modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp, top = 8.dp)
                ) {
                    val groupWidth = (size.width - groupGapPx * (groupCount - 1)) / groupCount
                    val barWidth = groupWidth / 2f
                    for (idx in 0 until groupCount) {
                        val groupStart = idx * (groupWidth + groupGapPx)
                        // 支出柱（红色）
                        val expense = expenseValues[idx]
                        val expenseLeft = groupStart
                        val expenseTop = size.height * (1f - (expense / max).toFloat())
                        drawRect(
                            color = Color(0xFFF44336),
                            topLeft = Offset(expenseLeft, expenseTop),
                            size = Size(barWidth, size.height - expenseTop)
                        )
                        // 支出数值
                        if (expense > 0) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#F44336")
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    textSize = 24f
                                    isFakeBoldText = true
                                }
                                canvas.nativeCanvas.drawText(
                                    "%.0f".format(expense),
                                    expenseLeft + barWidth / 2,
                                    expenseTop - 8f,
                                    paint
                                )
                            }
                        }
                        // 收入柱（绿色）
                        val income = incomeValues[idx]
                        val incomeLeft = groupStart + barWidth
                        val incomeTop = size.height * (1f - (income / max).toFloat())
                        drawRect(
                            color = Color(0xFF4CAF50),
                            topLeft = Offset(incomeLeft, incomeTop),
                            size = Size(barWidth, size.height - incomeTop)
                        )
                        // 收入数值
                        if (income > 0) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#4CAF50")
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    textSize = 24f
                                    isFakeBoldText = true
                                }
                                canvas.nativeCanvas.drawText(
                                    "%.0f".format(income),
                                    incomeLeft + barWidth / 2,
                                    incomeTop - 8f,
                                    paint
                                )
                            }
                        }
                    }
                    // x轴日期标签
                    for (idx in 0 until groupCount) {
                        val label = sortedDays[idx].takeLast(2) + "日"
                        val x = idx * groupWidth + groupWidth / 2
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 28f
                            }
                            canvas.nativeCanvas.drawText(
                                label,
                                x,
                                size.height + 38f, // 向下多10像素
                                paint
                            )
                        }
                    }
                }
            }
        }
        // 图例
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Box(Modifier.size(12.dp).background(Color(0xFFF44336), shape = MaterialTheme.shapes.small))
            Spacer(Modifier.width(4.dp))
            Text("支出", fontSize = 12.sp)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(12.dp).background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.small))
            Spacer(Modifier.width(4.dp))
            Text("收入", fontSize = 12.sp)
        }
    }
} 