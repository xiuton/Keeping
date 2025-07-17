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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillChartScreen(bills: List<BillItem>) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        BillBarChart(bills)
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("每日收支趋势", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(8.dp)
                .then(Modifier.widthIn(min = 240.dp, max = 420.dp))
            ) {
                val groupCount = incomeValues.size
                val groupWidth = size.width / groupCount.coerceAtLeast(1)
                val barWidth = groupWidth / 3f
                for (idx in 0 until groupCount) {
                    // 支出柱（红色）
                    val expense = expenseValues[idx]
                    val expenseLeft = idx * groupWidth + barWidth * 0.5f
                    val expenseTop = size.height * (1f - (expense / max).toFloat())
                    drawRect(
                        color = Color(0xFFF44336),
                        topLeft = Offset(expenseLeft, expenseTop),
                        size = Size(barWidth, size.height - expenseTop)
                    )
                    // 收入柱（绿色）
                    val income = incomeValues[idx]
                    val incomeLeft = idx * groupWidth + barWidth * 1.5f
                    val incomeTop = size.height * (1f - (income / max).toFloat())
                    drawRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(incomeLeft, incomeTop),
                        size = Size(barWidth, size.height - incomeTop)
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            sortedDays.forEachIndexed { idx, day ->
                Text(day.takeLast(2) + "日", fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
            }
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
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