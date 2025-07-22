package me.ganto.keeping.feature.bill

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import me.ganto.keeping.core.model.BillItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllBillsScreen(
    allBills: List<BillItem>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部账单") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(16.dp)) {
            LazyColumn(contentPadding = padding) {
                items(allBills) { bill ->
                    BillRow(
                        bill = bill,
                        onEdit = { /* TODO: 可弹出预览 */ },
                        onDelete = { /* TODO: 删除 */ }
                    )
                }
            }
        }
    }
} 