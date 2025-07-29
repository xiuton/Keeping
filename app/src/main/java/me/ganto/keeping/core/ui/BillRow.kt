package me.ganto.keeping.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ganto.keeping.core.model.BillItem

@Composable
fun BillRow(
    bill: BillItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    val categoryColor = when (bill.category) {
        "收入" -> MaterialTheme.colorScheme.primary
        "餐饮" -> MaterialTheme.colorScheme.primary
        "交通" -> MaterialTheme.colorScheme.primary
        "购物" -> MaterialTheme.colorScheme.primary
        "娱乐" -> MaterialTheme.colorScheme.primary
        "医疗" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
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
                    color = if (bill.amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    bill.time,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
} 