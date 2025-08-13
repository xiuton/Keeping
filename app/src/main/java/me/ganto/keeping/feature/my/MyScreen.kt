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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

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
    
    // 预算设置弹窗控制
    var showBudgetDialog by remember { mutableStateOf(false) }
    var tempBudget by remember { mutableStateOf("") }

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
        
        // 数据概览区
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernOverviewCard(
                title = "收入",
                value = MoneyUtils.formatSimpleMoney(totalIncome),
                color = Color(0xFF10B981),
                icon = Icons.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            ModernOverviewCard(
                title = "支出",
                value = MoneyUtils.formatSimpleMoney(-totalExpense),
                color = Color(0xFFEF4444),
                icon = Icons.Filled.TrendingDown,
                modifier = Modifier.weight(1f)
            )
            ModernOverviewCard(
                title = "结余",
                value = MoneyUtils.formatSimpleMoney(balance),
                color = Color(0xFF8B5CF6),
                icon = Icons.Filled.AccountBalance,
                modifier = Modifier.weight(1f)
            )
            ModernOverviewCard(
                title = "预算",
                value = MoneyUtils.formatSimpleMoney(budget),
                color = Color(0xFFF59E0B),
                icon = Icons.Filled.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 功能卡片区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // 我的账单
            ModernFeatureCard(
                icon = Icons.Filled.List,
                title = "我的账单",
                subtitle = "查看所有收支明细",
                color = Color(0xFF3B82F6),
                onClick = { navController.navigate("all_bills") }
            )
            
            Spacer(Modifier.height(12.dp))
            
            // 统计分析
            ModernFeatureCard(
                icon = Icons.Filled.BarChart,
                title = "统计分析",
                subtitle = "本月收入: ￥${MoneyUtils.formatMoney(totalIncome)}  支出: ￥${MoneyUtils.formatMoney(-totalExpense)}  结余: ￥${MoneyUtils.formatMoney(balance)}${if (topExpenseCategory != "-") "\n支出最多: $topExpenseCategory ￥${MoneyUtils.formatMoney(topExpenseAmount)}" else ""}",
                color = Color(0xFF10B981),
                onClick = { onStatisticsClick() }
            )
            
            Spacer(Modifier.height(12.dp))
            
            // 预算管理
            ModernFeatureCard(
                icon = Icons.Filled.AccountBalanceWallet,
                title = "预算管理",
                subtitle = "本月预算: ￥${MoneyUtils.formatMoney(budget)}  已用: ￥${MoneyUtils.formatMoney(usedBudget)}  剩余: ￥${MoneyUtils.formatMoney(remainBudget)}",
                color = Color(0xFFF59E0B),
                onClick = { 
                    tempBudget = budget.toString()
                    showBudgetDialog = true 
                },
                extraContent = {
                    LinearProgressIndicator(
                        progress = budgetProgress, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(vertical = 4.dp),
                        color = Color(0xFFF59E0B),
                        trackColor = Color(0xFFF59E0B).copy(alpha = 0.2f)
                    )
                }
            )
            
            Spacer(Modifier.height(12.dp))
            
            // 我的分类
            ModernFeatureCard(
                icon = Icons.Filled.Category,
                title = "我的分类",
                subtitle = "$categoryPreview${if (categoryCount > 4) "..." else ""}\n共${categoryCount}个分类",
                color = Color(0xFF8B5CF6),
                onClick = { /* TODO: 跳转我的分类 */ }
            )
        }
        
        // 底部间距
        Spacer(Modifier.height(innerPadding.calculateBottomPadding() + 16.dp))
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
    
    // 预算设置弹窗
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("设置月度预算") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 当前预算使用情况
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "当前预算使用情况",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFF59E0B)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("已用预算", fontSize = 12.sp)
                                Text("￥${MoneyUtils.formatMoney(usedBudget)}", 
                                     fontWeight = FontWeight.Bold, 
                                     fontSize = 12.sp,
                                     color = Color(0xFFEF4444))
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("剩余预算", fontSize = 12.sp)
                                Text("￥${MoneyUtils.formatMoney(remainBudget)}", 
                                     fontWeight = FontWeight.Bold, 
                                     fontSize = 12.sp,
                                     color = if (remainBudget >= 0) Color(0xFF10B981) else Color(0xFFEF4444))
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = budgetProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFFF59E0B),
                                trackColor = Color(0xFFF59E0B).copy(alpha = 0.2f)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // 预算输入框
                    OutlinedTextField(
                        value = tempBudget,
                        onValueChange = { 
                            // 只允许输入数字和小数点
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                tempBudget = it
                            }
                        },
                        label = { Text("月度预算金额") },
                        prefix = { Text("￥", color = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        "设置合理的月度预算有助于控制支出",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val budgetValue = tempBudget.toDoubleOrNull() ?: 0.0
                        if (budgetValue >= 0) {
                            viewModel.saveBudget(context, budgetValue)
                            showBudgetDialog = false
                            Toast.makeText(context, "预算设置成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "请输入有效的预算金额", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = tempBudget.isNotBlank() && tempBudget.toDoubleOrNull() != null
                ) { 
                    Text("保存") 
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { 
                    Text("取消") 
                }
            }
        )
    }
} 

// 现代概览卡片组件
@Composable
fun ModernOverviewCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(90.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
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
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    title, 
                    fontSize = 12.sp, 
                    color = color, 
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "￥$value", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 12.sp, 
                    color = color,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// 现代功能卡片组件
@Composable
fun ModernFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    extraContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(color.copy(alpha = 0.08f), color.copy(alpha = 0.02f))
                    )
                )
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标区域
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                // 内容区域
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        title, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle, 
                        fontSize = 13.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    extraContent?.invoke()
                }
                
                // 箭头图标
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "进入",
                    tint = color.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
} 