package me.ganto.keeping.feature.my

import android.Manifest
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
import androidx.compose.foundation.background
import java.io.InputStream
import java.io.OutputStream
import androidx.compose.material.icons.filled.Edit
import androidx.navigation.NavController
import me.ganto.keeping.navigation.ROUTE_FEEDBACK
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream
import java.io.IOException
import android.app.AlarmManager
import android.app.PendingIntent
import java.util.Calendar
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.LightMode
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalTime
import com.vanpra.composematerialdialogs.datetime.time.TimePickerDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import me.ganto.keeping.core.ui.ContentLoading
// 备份相关导入
import me.ganto.keeping.core.data.BackupManager
import me.ganto.keeping.core.data.BackupFileInfo
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.foundation.layout.FlowRow
import me.ganto.keeping.core.data.DefaultValues
import me.ganto.keeping.core.ui.SkeletonLoading
import me.ganto.keeping.core.data.PREF_KEY_EXP_CAT
import me.ganto.keeping.core.data.PREF_KEY_INC_CAT
import me.ganto.keeping.core.data.PREF_KEY_EXP_PAY
import me.ganto.keeping.core.data.PREF_KEY_INC_PAY
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.datastore.preferences.core.emptyPreferences
import me.ganto.keeping.core.model.BillItem

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    navController: NavController,
    innerPadding: PaddingValues,
    backupManager: BackupManager? = null,
    collectSettingsData: (() -> Map<String, String>)? = null,
    bills: List<BillItem> = emptyList(),
    saveBills: ((List<BillItem>) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    val currentVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "1.0.0"
    }

    // 备份相关状态
    var backupFiles by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf<BackupFileInfo?>(null) }
    var showDeleteBackupDialog by remember { mutableStateOf<BackupFileInfo?>(null) }
    var backupDescription by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf("") }
    var showBackupPathDialog by remember { mutableStateOf(false) }
    var showImporting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var isLoadingBackupFiles by remember { mutableStateOf(true) }
    val gson = remember { Gson() }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showMessage = "权限已授予"
        } else {
            showMessage = "需要存储权限才能创建外部备份"
        }
    }

    // 加载备份文件列表
    LaunchedEffect(Unit) {
        backupManager?.let { manager ->
            manager.getBackupFiles().onSuccess { files ->
                backupFiles = files
                isLoadingBackupFiles = false
            }.onFailure {
                isLoadingBackupFiles = false
            }
        }
    }

    // 备份相关函数
    fun createBackup() {
        if (backupManager == null || collectSettingsData == null || saveBills == null) return
        
        // 检查权限
        val hasWritePermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasReadPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasWritePermission || !hasReadPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
            return
        }
        
        scope.launch {
            isLoading = true
            try {
                val settings = collectSettingsData()
                val result = backupManager.createBackup(bills, settings, backupDescription)
                result.onSuccess { fileName ->
                    showMessage = "备份创建成功: $fileName"
                    backupDescription = ""
                    showBackupDialog = false
                    // 刷新备份文件列表
                    backupManager.getBackupFiles().onSuccess { files ->
                        backupFiles = files
                    }
                }.onFailure { error ->
                    showMessage = "备份创建失败: ${error.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    fun restoreBackup(backupFile: BackupFileInfo) {
        if (saveBills == null) return
        
        scope.launch {
            isLoading = true
            try {
                val result = backupManager?.restoreData(backupFile.fileName)
                result?.onSuccess { (restoredBills, restoredSettings) ->
                    // 恢复账单数据
                    saveBills(restoredBills)
                    
                    // 恢复设置数据
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            restoredSettings.forEach { (key, value) ->
                                when (key) {
                                    "expense_categories" -> prefs[PREF_KEY_EXP_CAT] = value
                                    "income_categories" -> prefs[PREF_KEY_INC_CAT] = value
                                    "expense_pay_types" -> prefs[PREF_KEY_EXP_PAY] = value
                                    "income_pay_types" -> prefs[PREF_KEY_INC_PAY] = value
                                    "is_dark" -> prefs[booleanPreferencesKey("is_dark")] = value.toBoolean()
                                    "sort_by" -> prefs[stringPreferencesKey("sort_by")] = value
                                }
                            }
                        }
                    }
                    
                    showMessage = "数据恢复成功"
                    showRestoreDialog = null
                }?.onFailure { error ->
                    showMessage = "数据恢复失败: ${error.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    fun deleteBackup(backupFile: BackupFileInfo) {
        scope.launch {
            isLoading = true
            try {
                val result = backupManager?.deleteBackup(backupFile.fileName)
                result?.onSuccess {
                    showMessage = "备份文件删除成功"
                    showDeleteBackupDialog = null
                    // 刷新备份文件列表
                    backupManager.getBackupFiles().onSuccess { files ->
                        backupFiles = files
                    }
                }?.onFailure { error ->
                    showMessage = "删除失败: ${error.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                showImporting = true
                importError = null
                val result = backupManager?.importBackupFromJsonUri(context, it)
                result?.onSuccess { (restoredBills, restoredSettings) ->
                    saveBills?.invoke(restoredBills)
                    // 恢复设置数据
                    context.dataStore.edit { prefs ->
                        restoredSettings.forEach { (key, value) ->
                            when (key) {
                                "expense_categories" -> prefs[PREF_KEY_EXP_CAT] = value
                                "income_categories" -> prefs[PREF_KEY_INC_CAT] = value
                                "expense_pay_types" -> prefs[PREF_KEY_EXP_PAY] = value
                                "income_pay_types" -> prefs[PREF_KEY_INC_PAY] = value
                                "is_dark" -> prefs[booleanPreferencesKey("is_dark")] = value.toBoolean()
                                "sort_by" -> prefs[stringPreferencesKey("sort_by")] = value
                            }
                        }
                    }
                    showMessage = "从JSON文件恢复成功"
                    showImporting = false
                }?.onFailure { error ->
                    importError = "导入失败: ${error.message}"
                    showImporting = false
                }
            }
        }
    }

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

    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    // 删除所有与 OkHttp、downloadFileWithProgress、onResponse、onFailure、OkHttpClient 相关的代码和变量，只保留 DownloadManager 相关逻辑。

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
                    // val apkUrl = "https://downv6.qq.com/qqweb/QQ_1/android_apk/Android_9.2.0_64.apk"
                    if (isNewerVersion(serverVersion, currentVersion)) {
                        latestVersion = serverVersion
                        updateUrl = apkUrl
                        updateAvailable = true
                        scope.launch(Dispatchers.Main) {
                            // displayVersion = serverVersion (不再更新右侧版本号)
                            showUpdateDialog = true // 弹窗
                        }
                    } else {
                        updateAvailable = false
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "已是最新版本 (本地:$currentVersion, 服务器:$serverVersion)", Toast.LENGTH_LONG).show()
                        }
                        Handler(Looper.getMainLooper()).post {
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

    var downloadProgress by remember { mutableStateOf(0) }
    var downloadCurrent by remember { mutableStateOf(0L) }
    var downloadTotal by remember { mutableStateOf(0L) }
    var downloadStatus by remember { mutableStateOf(0) }

    var pendingInstallApk by remember { mutableStateOf<File?>(null) }
    var deleteAfterInstall by remember { mutableStateOf(false) }

    // 注册广播接收进度
    DisposableEffect(Unit) {
        val mainHandler = Handler(Looper.getMainLooper())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "me.ganto.keeping.DOWNLOAD_PROGRESS") {
                    mainHandler.post {
                        val progress = intent.getIntExtra("progress", 0)
                        val downloaded = intent.getLongExtra("downloaded", 0L)
                        val total = intent.getLongExtra("total", 0L)
                        val status = intent.getIntExtra("status", 0)
                        downloadProgress = progress
                        downloadCurrent = downloaded
                        downloadTotal = total
                        downloadStatus = status
                    }
                }
            }
        }
        val filter = IntentFilter("me.ganto.keeping.DOWNLOAD_PROGRESS")
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // 授权后自动安装
    LaunchedEffect(pendingInstallApk) {
        if (pendingInstallApk != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls()) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    pendingInstallApk!!
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(installIntent)
                pendingInstallApk = null
            }
        }
    }

    // 记账提醒时间选择弹窗
    val timeDialogState = rememberMaterialDialogState()

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
        Spacer(Modifier.height(16.dp))
        // 账户与设置
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = "主题模式",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("主题模式", fontSize = 16.sp)
                    }
                    // 主题模式选择
                    var expanded by remember { mutableStateOf(false) }
                    val themeOptions = listOf("跟随系统", "浅色模式", "深色模式")
                    val themeValues = listOf("auto", "light", "dark")
                    val currentThemeIndex = themeValues.indexOf(themeMode)
                    val currentThemeText = if (currentThemeIndex >= 0) themeOptions[currentThemeIndex] else "跟随系统"
                    
                    Box {
                        TextButton(
                            onClick = { expanded = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = currentThemeText,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "展开",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            themeOptions.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = when (index) {
                                                    0 -> Icons.Filled.AutoMode
                                                    1 -> Icons.Filled.LightMode
                                                    else -> Icons.Filled.DarkMode
                                                },
                                                contentDescription = null,
                                                tint = if (index == currentThemeIndex) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = option,
                                                color = if (index == currentThemeIndex) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    },
                                    onClick = {
                                        onThemeModeChange(themeValues[index])
                                        expanded = false
                                    },
                                    modifier = Modifier
                                        .height(40.dp)
                                        .background(
                                            if (index == currentThemeIndex) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { checkUpdate() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = "检查更新",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("检查更新", fontSize = 16.sp)
                    }
                    if (checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            // 只在没新版本时显示版本号
                            Text(
                                text = "版本号: $currentVersion",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        versionClickCount++
                                        if (versionClickCount >= 5) {
                                            testEntryVisible = true
                                            versionClickCount = 0
                                        }
                                    }
                            )
                        }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // 记账提醒独立板块
        val REMINDER_ENABLED_KEY = stringPreferencesKey("reminder_enabled")
        val REMINDER_TIME_KEY = stringPreferencesKey("reminder_time")
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        // 直接用collectAsState绑定DataStore
        val prefsFlow = context.dataStore.data
        val prefs by prefsFlow.collectAsState(initial = null)
        val reminderEnabled = prefs?.get(REMINDER_ENABLED_KEY)?.toBoolean() ?: false
        val reminderTime = prefs?.get(REMINDER_TIME_KEY) ?: "21:30"
        var tempHour by remember { mutableStateOf(21) }
        var tempMinute by remember { mutableStateOf(30) }
        fun saveReminder(enabled: Boolean, time: String) {
            scope.launch {
                context.dataStore.edit { prefs ->
                    prefs[REMINDER_ENABLED_KEY] = enabled.toString()
                    prefs[REMINDER_TIME_KEY] = time
                }
            }
        }
        fun setDailyReminder(context: Context, hour: Int, minute: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:" + context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "请授权允许设置精确闹钟，否则无法定时提醒", Toast.LENGTH_LONG).show()
                    return
                }
            }
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "me.ganto.keeping.ACTION_REMIND"
                putExtra("hour", hour)
                putExtra("minute", minute)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "请在系统设置中允许精确闹钟权限，否则无法定时提醒", Toast.LENGTH_LONG).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:" + context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        }
        fun cancelDailyReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmManager.cancel(pendingIntent)
        }
        // 通知权限请求Launcher（Android 13+）
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (!granted) {
                    Toast.makeText(context, "请在系统设置中开启通知权限，否则无法收到记账提醒！", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        )
        fun checkAndRequestNotificationPermission(onGranted: () -> Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                onGranted()
            }
        }
        // 始终渲染Card，只在内容区显示loading
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                if (prefs == null) {
                    // 数据加载前显示loading占位符
                    ContentLoading(
                        message = "加载提醒设置...",
                        modifier = Modifier.height(48.dp)
                    )
                } else {
                    // 数据加载完成后显示真实内容
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "记账提醒",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("记账提醒", fontSize = 16.sp)
                        Spacer(Modifier.width(4.dp))
                            var showReminderTip by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { showReminderTip = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "定时提醒可靠性说明",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (showReminderTip) {
                                AlertDialog(
                                    onDismissRequest = { showReminderTip = false },
                                    title = { Text("定时提醒可靠性说明", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Text(
                                            "为保证定时提醒可靠，请在系统设置中：\n- 允许自启动\n- 设置电池无限制\n- 允许后台弹窗/通知\n否则在后台或被杀死时可能无法收到提醒。",
                                            fontSize = 15.sp
                                        )
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:" + context.packageName)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        }) {
                                            Text("一键前往系统设置")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showReminderTip = false }) { Text("关闭") }
                                    }
                                )
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = {
                                saveReminder(it, reminderTime)
                                if (it) {
                                    checkAndRequestNotificationPermission {
                                        val parts = reminderTime.split(":")
                                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
                                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 30
                                        setDailyReminder(context, hour, minute)
                                    }
                                } else {
                                    cancelDailyReminder(context)
                                }
                            }
                        )
                    }
                    if (reminderEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("提醒时间: $reminderTime", fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = {
                                    val parts = reminderTime.split(":")
                                    tempHour = parts.getOrNull(0)?.toIntOrNull() ?: 21
                                    tempMinute = parts.getOrNull(1)?.toIntOrNull() ?: 30
                                    timeDialogState.show()
                                }) {
                                    Text("选择时间")
                                }
                            }
                        }
                    }
                }
            }
        }
        // 记账提醒时间选择弹窗
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
        ) {
            MaterialDialog(
                dialogState = timeDialogState,
                backgroundColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(24.dp),
                buttons = {
                    positiveButton("确定")
                    negativeButton("取消")
                }
            ) {
                Box(Modifier.padding(top = 10.dp)) {
                    timepicker(
                        initialTime = LocalTime.of(tempHour, tempMinute),
                        title = "选择提醒时间",
                        is24HourClock = true,
                        colors = TimePickerDefaults.colors(
                            selectorTextColor = MaterialTheme.colorScheme.onBackground,
                            inactiveTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            headerTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) { time ->
                        tempHour = time.hour
                        tempMinute = time.minute
                        saveReminder(reminderEnabled, "%02d:%02d".format(tempHour, tempMinute))
                        if (reminderEnabled) {
                            checkAndRequestNotificationPermission {
                                setDailyReminder(context, tempHour, tempMinute)
                            }
                        }
                        timeDialogState.hide()
                    }
                }
            }
                }
        Spacer(Modifier.height(16.dp))
        
        // 数据备份管理卡片
        if (backupManager != null) {
            Card(
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("数据备份", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showBackupPathDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "备份文件位置",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 创建备份按钮
                    Button(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = "创建备份")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("创建备份")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 从JSON文件恢复按钮
                    OutlinedButton(
                        onClick = { importLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !showImporting
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "从JSON恢复")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("从JSON文件恢复")
                    }
                    if (showImporting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在导入JSON...", fontSize = 13.sp)
                        }
                    }
                    if (importError != null) {
                        Text(importError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 备份文件列表
                    if (isLoadingBackupFiles) {
                        // 加载时显示骨架屏
                        Text("备份文件列表", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonLoading(
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (backupFiles.isNotEmpty()) {
                        Text("备份文件列表", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            backupFiles.forEach { backupFile ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = backupFile.fileName,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (backupFile.description.isNotEmpty()) {
                                                    Text(
                                                        text = backupFile.description,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "账单数量: ${backupFile.billsCount} | 文件大小: ${backupFile.fileSize / 1024}KB",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                                        .format(Date(backupFile.timestamp)),
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row {
                                                IconButton(
                                                    onClick = { showRestoreDialog = backupFile },
                                                    enabled = !isLoading
                                                ) {
                                                    Icon(
                                                        Icons.Default.Restore,
                                                        contentDescription = "恢复",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { showDeleteBackupDialog = backupFile },
                                                    enabled = !isLoading
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "删除",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "暂无备份文件",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        
        // 关于与帮助
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { navController.navigate(ROUTE_FEEDBACK) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Feedback,
                        contentDescription = "意见反馈",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("意见反馈", fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                            .clickable {
                                Toast.makeText(
                                    context,
                                    "Keeping App Version: $currentVersion",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "关于App",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("关于App", fontSize = 16.sp)
                }
            }
        }
        if (testEntryVisible) {
            Spacer(Modifier.height(16.dp))
            // 测试页面入口板块
            Card(
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { navController.navigate("test") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BugReport,
                            contentDescription = "测试页面入口",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "测试页面入口",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
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
        // 新版本弹窗
        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { if (downloadStatus == 8 || downloadStatus == 16) showUpdateDialog = false },
                title = { Text("发现新版本") },
                text = {
                    Column {
                        Text("新版本号: $latestVersion")
                        when (downloadStatus) {
                            1, 2 -> {
                                Spacer(Modifier.height(20.dp))
                                LinearProgressIndicator(
                                    progress = if (downloadTotal > 0) downloadCurrent / downloadTotal.toFloat() else 0f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    if (downloadCurrent == 0L || downloadTotal <= 0L)
                                        "正在获取进度..."
                                    else
                                        "下载进度：${(downloadCurrent * 100 / downloadTotal)}% (${downloadCurrent / 1024}KB/${downloadTotal / 1024}KB)",
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            8 -> { // 下载完成，显示“去安装”与“安装后删除”按钮
                                Spacer(Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(Modifier.height(24.dp))
                                    Text(
                                        "下载完成",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Checkbox(
                                            checked = deleteAfterInstall,
                                            onCheckedChange = { deleteAfterInstall = it }
                                        )
                                        Text("安装后自动删除安装包")
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            16 -> { // 下载失败，显示“关闭”
                                Button(onClick = { showUpdateDialog = false }, modifier = Modifier.height(32.dp)) {
                                    Text("关闭")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row {
                        when (downloadStatus) {
                            0 -> { // 未下载，显示“下载”和“取消”
                                TextButton(
                                    onClick = { showUpdateDialog = false },
                                    modifier = Modifier.height(32.dp)
                                ) { Text("取消") }
                                Spacer(Modifier.width(16.dp))
                                Button(
                                    onClick = {
                                        isDownloading = true
                                        downloadProgress = 0
                                        downloadError = null
                                        downloadStatus = 1 // 立即切换到“下载中”分支
                                        val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Keeping_${latestVersion}_Android.apk")
                                        downloadFileWithProgress(
                                            url = updateUrl,
                                            destFile = destFile,
                                            onProgress = { current, total ->
                                                downloadCurrent = current
                                                downloadTotal = total
                                                downloadStatus = 2
                                            },
                                            onComplete = {
                                                downloadStatus = 8
                                                isDownloading = false
                                                var canInstall = true
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    if (!context.packageManager.canRequestPackageInstalls()) {
                                                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                            data = Uri.parse("package:" + context.packageName)
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        context.startActivity(intent)
                                                        Handler(Looper.getMainLooper()).post {
                                                            Toast.makeText(context, "请授权允许安装未知应用", Toast.LENGTH_LONG).show()
                                                        }
                                                        canInstall = false
                                                        pendingInstallApk = destFile
                                                    }
                                                }
                                                if (canInstall) {
                                                    // 自动弹出安装
                                                    val apkUri = FileProvider.getUriForFile(
                                                        context,
                                                        context.packageName + ".fileprovider",
                                                        destFile
                                                    )
                                                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(installIntent)
                                                    pendingInstallApk = null
                                                }
                                            },
                                            onError = { e ->
                                                downloadStatus = 16
                                                isDownloading = false
                                                downloadError = e.message
                                            }
                                        )
                                    },
                                    modifier = Modifier.height(32.dp)
                                ) { Text("下载") }
                            }
                            1, 2 -> {
                                // 下载中，不显示按钮或可选显示灰色“下载中...”
                            }
                            8 -> { // 下载完成，显示“去安装”按钮
                                Button(
                                    onClick = {
                                        val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Keeping_${latestVersion}_Android.apk")
                                        val apkUri = FileProvider.getUriForFile(
                                            context,
                                            context.packageName + ".fileprovider",
                                            destFile
                                        )
                                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        if (deleteAfterInstall) {
                                            // 只在安装完成后删除安装包
                                            val packageReceiver = object : BroadcastReceiver() {
                                                override fun onReceive(context: Context?, intent: Intent?) {
                                                    val action = intent?.action
                                                    val data = intent?.data
                                                    if (action == Intent.ACTION_PACKAGE_ADDED && data != null) {
                                                        // 这里可以进一步判断包名是否为你的目标包名
                                                        if (destFile.exists()) destFile.delete()
                                                        context?.unregisterReceiver(this)
                                                    }
                                                }
                                            }
                                            val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
                                            filter.addDataScheme("package")
                                            context.registerReceiver(packageReceiver, filter)
                                        }
                                        context.startActivity(installIntent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                ) { Text("去安装") }
                            }
                            16 -> { // 下载失败，显示“关闭”
                                Button(onClick = { showUpdateDialog = false }, modifier = Modifier.height(32.dp)) {
                                    Text("关闭")
                                }
                            }
                        }
                    }
                },
                dismissButton = null
            )
        }
        
        // 备份相关对话框
        // 创建备份对话框
        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                title = { Text("创建备份") },
                text = {
                    Column {
                        Text("将为当前数据创建备份文件")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = backupDescription,
                            onValueChange = { backupDescription = it },
                            label = { Text("备份描述（可选）") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { createBackup() },
                        enabled = !isLoading
                    ) { Text("创建") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showBackupDialog = false
                            backupDescription = ""
                        }
                    ) { Text("取消") }
                }
            )
        }
        
        // 恢复备份对话框
        showRestoreDialog?.let { backupFile ->
            AlertDialog(
                onDismissRequest = { showRestoreDialog = null },
                title = { Text("恢复备份") },
                text = {
                    Text("确定要恢复备份文件 \"${backupFile.fileName}\" 吗？\n当前数据将被覆盖。")
                },
                confirmButton = {
                    Button(
                        onClick = { restoreBackup(backupFile) },
                        enabled = !isLoading
                    ) { Text("恢复") }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showRestoreDialog = null }
                    ) { Text("取消") }
                }
            )
        }
        
        // 删除备份对话框
        showDeleteBackupDialog?.let { backupFile ->
            AlertDialog(
                onDismissRequest = { showDeleteBackupDialog = null },
                title = { Text("删除备份") },
                text = {
                    Text("确定要删除备份文件 \"${backupFile.fileName}\" 吗？")
                },
                confirmButton = {
                    Button(
                        onClick = { deleteBackup(backupFile) },
                        enabled = !isLoading
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteBackupDialog = null }
                    ) { Text("取消") }
                }
            )
        }
        
        // 备份路径信息对话框
        if (showBackupPathDialog) {
            AlertDialog(
                onDismissRequest = { showBackupPathDialog = false },
                title = { Text("备份文件位置") },
                text = {
                    Text("备份文件保存在：\n/storage/emulated/0/Keeping/backup/\n\n您可以通过文件管理器访问此目录。")
                },
                confirmButton = {
                    TextButton(
                        onClick = { showBackupPathDialog = false }
                    ) { Text("知道了") }
                }
            )
        }
        
        // 显示消息
        if (showMessage.isNotEmpty()) {
            LaunchedEffect(showMessage) {
                Toast.makeText(context, showMessage, Toast.LENGTH_SHORT).show()
                showMessage = ""
            }
        }
        
        // 底部间距
        Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
    }
}

// OkHttp 实时进度下载
fun downloadFileWithProgress(
    url: String,
    destFile: File,
    onProgress: (Long, Long) -> Unit,
    onComplete: () -> Unit,
    onError: (Throwable) -> Unit
) {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            onError(e)
        }
        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            if (!response.isSuccessful) {
                onError(IOException("Unexpected code $response"))
                return
            }
            val body = response.body ?: return onError(IOException("Empty body"))
            val total = body.contentLength()
            var sum = 0L
            val input = body.byteStream()
            val output = FileOutputStream(destFile)
            val buffer = ByteArray(8 * 1024)
            var len: Int
            try {
                while (input.read(buffer).also { len = it } != -1) {
                    output.write(buffer, 0, len)
                    sum += len
                    onProgress(sum, total)
                }
                output.flush()
                onComplete()
            } catch (e: Exception) {
                onError(e)
            } finally {
                input.close()
                output.close()
            }
        }
    })
} 