package me.ganto.keeping.feature.bill

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf

import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.core.ui.SearchBar
import me.ganto.keeping.core.ui.EmptyState
import me.ganto.keeping.core.ui.BillRow
import me.ganto.keeping.core.ui.ErrorState
import me.ganto.keeping.core.ui.SkeletonLoading
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllBillsScreen(
    allBills: List<BillItem>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var lastClickTime by remember { mutableStateOf(0L) }
    var enabled by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部账单") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            enabled = false
                            onBack()
                        },
                        enabled = enabled
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 常驻搜索栏
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* 搜索逻辑在filteredBills中处理 */ },
                placeholder = "搜索全部账单...",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
            )
            
            val filteredBills by remember(searchQuery, allBills) {
                derivedStateOf {
                    if (searchQuery.isNotBlank()) {
                        allBills.filter {
                            it.category.contains(searchQuery, ignoreCase = true) ||
                            it.remark.contains(searchQuery, ignoreCase = true) ||
                            it.payType.contains(searchQuery, ignoreCase = true) ||
                            it.time.contains(searchQuery, ignoreCase = true)
                        }
                    } else {
                        allBills
                    }
                }
            }
            
            // 调试信息
            LaunchedEffect(filteredBills.size) {
                println("AllBillsScreen: filteredBills size = ${filteredBills.size}")
            }
            
            if (filteredBills.isEmpty()) {
                EmptyState(
                    title = if (searchQuery.isNotBlank()) "未找到相关账单" else "暂无账单",
                    message = if (searchQuery.isNotBlank()) "尝试调整搜索关键词" else "这里还没有任何账单",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredBills.forEach { bill ->
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
} 