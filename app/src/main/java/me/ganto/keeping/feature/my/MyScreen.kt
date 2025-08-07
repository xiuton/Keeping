package me.ganto.keeping.feature.my

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import me.ganto.keeping.R
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.ganto.keeping.core.data.dataStore
import androidx.core.content.FileProvider
import java.io.File
import com.yalantis.ucrop.UCrop
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import java.io.InputStream
import java.io.OutputStream
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.offset
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Feedback
import androidx.lifecycle.viewmodel.compose.viewModel
import me.ganto.keeping.core.data.MainViewModel
import me.ganto.keeping.core.util.MoneyUtils
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.AccountBalance

@Composable
fun MyScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    navController: NavController,
    innerPadding: PaddingValues,
    onStatisticsClick: () -> Unit = {}
) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 头像持久化
    val AVATAR_URI_KEY = stringPreferencesKey("avatar_uri")
    var avatarUri by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val uri = context.dataStore.data.map { it[AVATAR_URI_KEY] ?: "" }.first()
        avatarUri = uri
    }
    fun saveAvatarUri(uri: String) {
        avatarUri = uri
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[AVATAR_URI_KEY] = uri
            }
        }
    }
    
    // 昵称持久化
    val NICKNAME_KEY = stringPreferencesKey("nickname")
    var nickname by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val name = context.dataStore.data.map { it[NICKNAME_KEY] ?: "未登录" }.first()
        nickname = name
    }
    fun saveNickname(name: String) {
        nickname = name
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[NICKNAME_KEY] = name
            }
        }
    }
    
    // 裁剪相关
    var tempCropUri by remember { mutableStateOf<Uri?>(null) }
    // uCrop裁剪Launcher
    val cropLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val resultUri = UCrop.getOutput(result.data ?: return@rememberLauncherForActivityResult)
        resultUri?.let {
            saveAvatarUri(it.toString())
        }
    }
    // 图片选择器
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // 生成临时文件用于裁剪输出
            val cropFile = File(context.cacheDir, "avatar_crop_${System.currentTimeMillis()}.jpg")
            val cropUri =
                FileProvider.getUriForFile(context, context.packageName + ".fileprovider", cropFile)
            tempCropUri = cropUri
            // 配置uCrop
            val uCrop = UCrop.of(it, cropUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(400, 400)
                .withOptions(UCrop.Options().apply {
                    setCircleDimmedLayer(true)
                    setShowCropGrid(false)
                    setHideBottomControls(true)
                    setToolbarTitle("裁剪头像")
                    setToolbarColor(0xFF22C55E.toInt())
                    setStatusBarColor(0xFF22C55E.toInt())
                })
            val intent = uCrop.getIntent(context)
            cropLauncher.launch(intent)
        }
    }

    // 背景图持久化
    val BG_URI_KEY = stringPreferencesKey("my_bg_uri")
    var bgUri by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val uri = context.dataStore.data.map { it[BG_URI_KEY] ?: "" }.first()
        bgUri = if (uri.isNotBlank()) uri else null
    }
    fun saveBgUri(uri: String) {
        bgUri = uri
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[BG_URI_KEY] = uri
            }
        }
    }
    // 背景图选择器
    val bgLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // 拷贝图片到私有目录
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val bgFile = File(context.filesDir, "my_bg_${System.currentTimeMillis()}.jpg")
            val outputStream: OutputStream = bgFile.outputStream()
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            saveBgUri(bgFile.absolutePath)
        }
    }

    // 弹窗控制
    var showDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadAllData(context)
        viewModel.loadBudget(context)
    }
    val bills = viewModel.bills
    val budget = viewModel.budget
    val expenseCategories = viewModel.expenseCategories
    val incomeCategories = viewModel.incomeCategories

    // 统计分析数据
    val now = java.util.Calendar.getInstance()
    val year = now.get(java.util.Calendar.YEAR)
    val month = now.get(java.util.Calendar.MONTH) + 1
    val monthBills = bills.filter {
        val parts = it.time.split(" ")[0].split("-")
        parts.size == 3 &&
        parts[0].toIntOrNull() == year &&
        parts[1].toIntOrNull() == month
    }
    val totalIncome = monthBills.filter { it.amount > 0 }.sumOf { it.amount }
    val totalExpense = monthBills.filter { it.amount < 0 }.sumOf { it.amount }
    val balance = MoneyUtils.calculateBalance(totalIncome, totalExpense)
    val expenseByCategory = monthBills.filter { it.amount < 0 }.groupBy { it.category }.mapValues { it.value.sumOf { b -> -b.amount } }
    val topExpenseCategory = expenseByCategory.maxByOrNull { it.value }?.key ?: "-"
    val topExpenseAmount = expenseByCategory.maxByOrNull { it.value }?.value ?: 0.0

    // 预算管理数据
    val usedBudget = -totalExpense
    val remainBudget = budget - usedBudget
    val budgetProgress = if (budget > 0) ((usedBudget / budget).coerceIn(0.0, 1.0)).toFloat() else 0f

    // 分类摘要
    val allCategories = expenseCategories + incomeCategories
    val categoryPreview = allCategories.take(4).joinToString("、")
    val categoryCount = allCategories.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部头像区，背景图只在此区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            if (bgUri != null && File(bgUri!!).exists()) {
                val bgFile = File(bgUri!!)
                AsyncImage(
                    model = bgFile,
                    contentDescription = "背景图",
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = "默认背景图",
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // 右上角添加背景图icon按钮，top为系统栏高度
            val density = LocalDensity.current
            val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
            val statusBarHeightDp = with(density) { statusBarHeightPx.toDp() }
            
            // 左上角设置图标按钮
            IconButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = statusBarHeightDp, start = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            // 右上角背景图编辑按钮
            IconButton(
                onClick = { bgLauncher.launch("image/*") },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = statusBarHeightDp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "添加/更换背景图",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            // 半透明遮罩
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
            )
            // 头像和昵称居中叠加
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(90.dp)
                        .clickable { tempName = nickname; showDialog = true },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    if (avatarUri.isNotBlank()) {
                        AsyncImage(
                            model = avatarUri as Any,
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "头像",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    nickname,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { tempName = nickname; showDialog = true },
                    color = Color.White
                )
            }
        }
        // 新增：数据概览区
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ModernOverviewCard(title = "收入", value = MoneyUtils.formatMoney(totalIncome), color = Color(0xFF10B981), icon = Icons.Filled.TrendingUp)
            ModernOverviewCard(title = "支出", value = MoneyUtils.formatMoney(-totalExpense), color = Color(0xFFEF4444), icon = Icons.Filled.TrendingDown)
            ModernOverviewCard(title = "结余", value = MoneyUtils.formatMoney(balance), color = Color(0xFF8B5CF6), icon = Icons.Filled.AccountBalance)
            ModernOverviewCard(title = "预算", value = MoneyUtils.formatMoney(budget), color = Color(0xFFF59E0B), icon = Icons.Filled.AccountBalanceWallet)
        }
        Spacer(Modifier.height(24.dp))
        // 快捷入口区（2行网格）
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ModernQuickEntry(icon = Icons.Filled.List, label = "我的账单", color = Color(0xFF3B82F6)) { navController.navigate("all_bills") }
                ModernQuickEntry(icon = Icons.Filled.BarChart, label = "统计分析", color = Color(0xFF10B981)) { onStatisticsClick() }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ModernQuickEntry(icon = Icons.Filled.AccountBalanceWallet, label = "预算管理", color = Color(0xFFF59E0B)) { /* TODO: 跳转预算管理 */ }
                ModernQuickEntry(icon = Icons.Filled.Category, label = "我的分类", color = Color(0xFF8B5CF6)) { /* TODO: 跳转我的分类 */ }
            }
        }
        // 下方内容区加左右和底部间距
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            // 我的账单
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("all_bills") },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.List, contentDescription = "我的账单", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("我的账单", fontWeight = FontWeight.Medium)
                        Text("查看所有收支明细", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // 统计分析
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStatisticsClick() },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = "统计分析", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("统计分析", fontWeight = FontWeight.Medium)
                        Text("本月收入: ￥${MoneyUtils.formatMoney(totalIncome)}  支出: ￥${MoneyUtils.formatMoney(-totalExpense)}  结余: ￥${MoneyUtils.formatMoney(balance)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        if (topExpenseCategory != "-") {
                            Text("支出最多: $topExpenseCategory ￥${MoneyUtils.formatMoney(topExpenseAmount)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // 预算管理
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: 跳转预算管理 */ },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "预算管理", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("预算管理", fontWeight = FontWeight.Medium)
                        Text("本月预算: ￥${MoneyUtils.formatMoney(budget)}  已用: ￥${MoneyUtils.formatMoney(usedBudget)}  剩余: ￥${MoneyUtils.formatMoney(remainBudget)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        LinearProgressIndicator(progress = budgetProgress, modifier = Modifier.fillMaxWidth().height(6.dp).padding(vertical = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // 我的分类
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: 跳转我的分类 */ },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Category, contentDescription = "我的分类", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("我的分类", fontWeight = FontWeight.Medium)
                        Text("$categoryPreview${if (categoryCount > 4) "..." else ""}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                        Text("共${categoryCount}个分类", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
        
        // 底部间距
        Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
    }
    
    // 头像/昵称编辑弹窗
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("编辑个人信息") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Card(
                            shape = CircleShape,
                            modifier = Modifier
                                .size(80.dp)
                                .clickable { launcher.launch("image/*") },
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            if (avatarUri.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUri,
                                    contentDescription = "头像",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "头像",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = (-6).dp, y = (-6).dp)
                                .clickable { launcher.launch("image/*") }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "更换头像",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("昵称") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    saveNickname(tempName)
                    showDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        )
    }
} 

// 新增：概览卡片和快捷入口组件
@Composable
fun OverviewCard(title: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 14.sp, color = color, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text("￥$value", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        }
    }
}

@Composable
fun QuickEntry(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
} 

// 新增：现代概览卡片和快捷入口组件
@Composable
fun ModernOverviewCard(title: String, value: String, color: Color, icon: ImageVector) {
    Card(
        modifier = Modifier
            .width(85.dp)
            .height(85.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.1f), color.copy(alpha = 0.05f))
                    )
                )
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(title, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text("￥$value", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
            }
        }
    }
}

@Composable
fun ModernQuickEntry(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(color.copy(alpha = 0.15f), color.copy(alpha = 0.05f))
                    )
                )
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text("点击查看", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
} 