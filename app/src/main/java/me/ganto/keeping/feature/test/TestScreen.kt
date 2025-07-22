package me.ganto.keeping.feature.test

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.material3.Slider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(onBack: () -> Unit) {
    var counter by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(false) }
    val snackbarHostState = SnackbarHostState()
    var progress by remember { mutableStateOf(0.3f) }
    val listData = remember { (1..20).map { "测试项 $it" } }
    var selectedRadio by remember { mutableStateOf(0) }
    var selectedChecks by remember { mutableStateOf(setOf<Int>()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val composeUrl = "https://developer.android.com/jetpack/compose"
    var switchState by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0.5f) }
    var stepperValue by remember { mutableStateOf(1) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedMenu by remember { mutableStateOf("A") }
    var chipSelected by remember { mutableStateOf(false) }
    val kotlinUrl = "https://kotlinlang.org/docs/home.html"
    
    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Compose 官方文档: $composeUrl",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(composeUrl))
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Kotlin 官方文档: $kotlinUrl",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kotlinUrl))
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("这里是测试页面内容", style = MaterialTheme.typography.titleLarge)
                }
            }
            item {
                Button(onClick = { counter++ }) { Text("计数器: $counter") }
            }
            item {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("输入框演示") },
                    singleLine = true
                )
            }
            item {
                Button(onClick = { showDialog = true }) { Text("弹窗演示") }
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("弹窗标题") },
                        text = { Text("这是一个弹窗内容演示。") },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) { Text("确定") }
                        }
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("深色模式", fontSize = 16.sp)
                    Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                }
            }
            item {
                Text("图片加载演示", fontWeight = FontWeight.Bold)
                AsyncImage(
                    model = "https://picsum.photos/200/200",
                    contentDescription = "网络图片",
                    modifier = Modifier.size(100.dp)
                )
            }
            item {
                Text("进度条演示", fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { progress = (progress + 0.1f).coerceAtMost(1f) }) { Text("增加") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { progress = (progress - 0.1f).coerceAtLeast(0f) }) { Text("减少") }
                }
            }
            item {
                Text("列表演示", fontWeight = FontWeight.Bold)
                Box(Modifier.height(120.dp).fillMaxWidth()) {
                    LazyColumn {
                        items(listData) { item ->
                            Text(item, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
            item {
                Text("单选演示", fontWeight = FontWeight.Bold)
                Column {
                    (0..2).forEach { i ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = selectedRadio == i, onClick = { selectedRadio = i }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedRadio == i, onClick = { selectedRadio = i })
                            Text("选项 $i")
                        }
                    }
                }
            }
            item {
                Text("多选演示", fontWeight = FontWeight.Bold)
                Column {
                    (0..2).forEach { i ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = selectedChecks.contains(i),
                                    onValueChange = {
                                        selectedChecks = if (it) selectedChecks + i else selectedChecks - i
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selectedChecks.contains(i), onCheckedChange = null)
                            Text("多选 $i")
                        }
                    }
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("开关演示", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = switchState, onCheckedChange = { switchState = it })
                }
            }
            item {
                Text("滑块演示: ${(sliderValue * 100).toInt()}")
                Slider(value = sliderValue, onValueChange = { sliderValue = it })
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("步进器演示: $stepperValue", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { if (stepperValue > 0) stepperValue-- }) { Text("-") }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { stepperValue++ }) { Text("+") }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("下拉菜单演示: $selectedMenu", fontSize = 16.sp)
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("A") }, onClick = { selectedMenu = "A"; menuExpanded = false })
                        DropdownMenuItem(text = { Text("B") }, onClick = { selectedMenu = "B"; menuExpanded = false })
                        DropdownMenuItem(text = { Text("C") }, onClick = { selectedMenu = "C"; menuExpanded = false })
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = {}, label = { Text("AssistChip") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = chipSelected, onClick = { chipSelected = !chipSelected }, label = { Text("FilterChip") })
                    Spacer(Modifier.width(8.dp))
                    InputChip(selected = chipSelected, onClick = { chipSelected = !chipSelected }, label = { Text("InputChip") })
                }
            }
            item {
                Text("卡片演示", fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("这是一个Card演示", fontSize = 16.sp)
                        Text("可以放任意内容", fontSize = 14.sp)
                    }
                }
            }
            item {
                // Snackbar演示
                Button(onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("这是一个Snackbar演示")
                    }
                }) {
                    Text("Snackbar演示")
                }
            }
        }
    }
} 