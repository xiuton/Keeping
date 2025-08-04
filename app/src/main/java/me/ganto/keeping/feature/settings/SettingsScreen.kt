package me.ganto.keeping.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 主题设置
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("主题设置", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = themeMode == "light",
                            onClick = { onThemeModeChange("light") }
                        )
                        Text("浅色", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = themeMode == "dark",
                            onClick = { onThemeModeChange("dark") }
                        )
                        Text("深色", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = themeMode == "auto",
                            onClick = { onThemeModeChange("auto") }
                        )
                        Text("跟随系统", fontSize = 15.sp)
                    }
                }
            }

            // 应用信息
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("应用信息", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("版本", fontSize = 15.sp)
                        Text("1.0.0", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 通知设置
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("通知设置", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Notifications, contentDescription = "通知", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("记账提醒", fontSize = 15.sp)
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = { /* TODO: 实现通知开关 */ }
                        )
                    }
                }
            }

            // 数据管理
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("数据管理", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Backup, contentDescription = "备份", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("数据备份", fontSize = 15.sp)
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = "跳转")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Delete, contentDescription = "清除", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("清除数据", fontSize = 15.sp)
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = "跳转")
                    }
                }
            }

            // 应用信息
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("应用信息", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("版本", fontSize = 15.sp)
                        Text("1.0.0", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("开发者", fontSize = 15.sp)
                        Text("Ganto", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 关于
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("关于", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Feedback, contentDescription = "反馈", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("意见反馈", fontSize = 15.sp)
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = "跳转")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BugReport, contentDescription = "报告", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("问题报告", fontSize = 15.sp)
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = "跳转")
                    }
                }
            }
        }
    }
} 