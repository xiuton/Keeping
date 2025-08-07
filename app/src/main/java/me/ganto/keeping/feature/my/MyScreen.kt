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

@Composable
fun MyScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    navController: NavController,
    innerPadding: PaddingValues
) {
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
        // 下方内容区加左右和底部间距
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // 删除所有底部板块内容，只保留背景图区域
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