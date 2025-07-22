package me.ganto.keeping.feature.my

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import java.io.InputStream
import java.io.OutputStream
import androidx.compose.material.icons.filled.Edit
import androidx.navigation.NavController
import me.ganto.keeping.navigation.ROUTE_FEEDBACK

@Composable
fun MyScreen(
    isDark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    val currentVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) { "1.0.0" }

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
    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
            val cropUri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", cropFile)
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
                    setToolbarColor(0xFF6C63FF.toInt())
                    setStatusBarColor(0xFF6C63FF.toInt())
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
    val bgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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

    // 更严谨的版本号比较
    fun isNewerVersion(server: String, local: String): Boolean {
        val s = server.split(".")
        val l = local.split(".")
        for (i in 0 until maxOf(s.size, l.size)) {
            val sv = s.getOrNull(i)?.toIntOrNull() ?: 0
            val lv = l.getOrNull(i)?.toIntOrNull() ?: 0
            if (sv > lv) return true
            if (sv < lv) return false
        }
        return false
    }

    // 带重试和超时的网络请求
    fun fetchWithRetry(urlStr: String, maxRetry: Int = 3, timeout: Int = 8000): String {
        var lastEx: Exception? = null
        repeat(maxRetry) { attempt ->
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = timeout
                conn.readTimeout = timeout
                conn.requestMethod = "GET"
                conn.connect()
                if (conn.responseCode == 200) {
                    return conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw Exception("HTTP ${conn.responseCode}")
                }
            } catch (e: Exception) {
                lastEx = e
                Thread.sleep(300)
            }
        }
        throw lastEx ?: Exception("Unknown error")
    }

    fun checkUpdate() {
        checking = true
        scope.launch(Dispatchers.IO) {
            try {
                val metaUrl = "https://keeping-release.netlify.app/output-metadata.json"
                val json = fetchWithRetry(metaUrl, maxRetry = 3, timeout = 8000)
                val obj = JSONObject(json)
                val elements = obj.getJSONArray("elements")
                if (elements.length() > 0) {
                    val element = elements.getJSONObject(0)
                    val serverVersion = element.getString("versionName")
                    val outputFile = element.getString("outputFile")
                    val apkUrl = "https://keeping-release.netlify.app/$outputFile"
                    if (isNewerVersion(serverVersion, currentVersion)) {
                        latestVersion = serverVersion
                        updateUrl = apkUrl
                        updateAvailable = true
                    } else {
                        updateAvailable = false
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "已是最新版本 (本地:$currentVersion, 服务器:$serverVersion)", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "未找到版本信息", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                scope.launch(Dispatchers.Main) {
                    if (!updateAvailable) {
                        Toast.makeText(context, "检查更新失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } finally {
                checking = false
            }
        }
    }

    fun downloadAndInstall(url: String) {
        val version = latestVersion.ifBlank { "unknown" }
        val fileName = "Keeping_${version}_Android.apk"
        val request = DownloadManager.Request(Uri.parse(url))
        request.setTitle(fileName) // 兼容部分国产ROM
        request.setDescription("正在下载新版本...")
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "正在后台下载更新包，请稍后安装", Toast.LENGTH_LONG).show()
    }

    // 弹窗控制
    var showDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    // WebView弹窗控制
    var showFeedbackWeb by remember { mutableStateOf(false) }
    val feedbackUrl = "https://github.com/xiuton/Keeping/issues"

    var testEntryVisible by remember { mutableStateOf(false) }
    var versionClickCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部背景图+头像+昵称区域（无左右padding）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                // 去掉圆角clip
        ) {
            // 背景图
            if (bgUri != null && File(bgUri!!).exists()) {
                val bgFile = File(bgUri!!)
                AsyncImage(
                    model = bgFile,
                    contentDescription = "背景图",
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(16.dp), // 去掉clip
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = "默认背景图",
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(16.dp), // 去掉clip
                    contentScale = ContentScale.Crop
                )
            }
            // 右上角添加背景图icon按钮
            Box(
                Modifier
                    .fillMaxSize()
                    // 去掉clip
            ) {
                IconButton(
                    onClick = { bgLauncher.launch("image/*") },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "添加/更换背景图",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
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
                    @Suppress("INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING", "UNCHECKED_CAST")
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
                Text(nickname, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.clickable { tempName = nickname; showDialog = true }, color = Color.White)
            }
        }
        // 内容区（有左右padding）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            // 账户与设置
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("深色模式", fontSize = 16.sp)
                        Switch(checked = isDark, onCheckedChange = onDarkChange)
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { checkUpdate() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("检查更新", fontSize = 16.sp)
                        if (checking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else if (updateAvailable) {
                            Text("发现新版本: $latestVersion", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            Button(onClick = { downloadAndInstall(updateUrl) }) { Text("下载并安装") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // 关于与帮助
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(ROUTE_FEEDBACK) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("意见反馈", fontSize = 16.sp)
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { Toast.makeText(context, "Keeping App Version: $currentVersion", Toast.LENGTH_SHORT).show() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("关于App", fontSize = 16.sp)
                    }
                }
            }
            if (testEntryVisible) {
                Spacer(Modifier.height(16.dp))
                // 测试页面入口板块
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("test") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("测试页面入口", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "版本号: $currentVersion",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        versionClickCount++
                        if (versionClickCount >= 5) {
                            testEntryVisible = true
                            versionClickCount = 0
                        }
                    }
            )
        }
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
                            elevation = CardDefaults.cardElevation(2.dp)
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