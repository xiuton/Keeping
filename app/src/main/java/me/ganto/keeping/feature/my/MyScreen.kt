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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import android.app.AlarmManager
import android.app.PendingIntent
import java.util.Calendar
import me.ganto.keeping.feature.my.ReminderReceiver
import android.provider.Settings
import android.content.pm.PackageManager

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyScreen(
    isDark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    navController: NavController,
    innerPadding: PaddingValues
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
//                    val apkUrl = "https://keeping-release.netlify.app/$outputFile"
                    val apkUrl = "https://downv6.qq.com/qqweb/QQ_1/android_apk/Android_9.2.0_64.apk"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部头像区，背景图只在此区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
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
            // 右上角添加背景图icon按钮
                IconButton(
                    onClick = { bgLauncher.launch("image/*") },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 32.dp, end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "添加/更换背景图",
                        tint = MaterialTheme.colorScheme.primary
                    )
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
        val reminderTime = prefs?.get(REMINDER_TIME_KEY) ?: "20:00"
        var showTimePicker by remember { mutableStateOf(false) }
        var tempHour by remember { mutableStateOf(20) }
        var tempMinute by remember { mutableStateOf(0) }
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
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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
                    Box(
                        Modifier
                            .height(48.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // 数据加载完成后显示真实内容
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("记账提醒", fontSize = 16.sp)
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = {
                                saveReminder(it, reminderTime)
                                if (it) {
                                    checkAndRequestNotificationPermission {
                                        val parts = reminderTime.split(":")
                                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        setDailyReminder(context, hour, minute)
                                    }
                                } else {
                                    cancelDailyReminder(context)
                                }
                            }
                        )
                    }
                    if (reminderEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("提醒时间: $reminderTime", fontSize = 15.sp)
                            Button(onClick = {
                                val parts = reminderTime.split(":")
                                tempHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                                tempMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                showTimePicker = true
                            }) {
                                Text("选择时间")
                            }
                        }
                    }
                }
            }
        }
        if (showTimePicker && prefs != null) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("选择提醒时间", fontWeight = FontWeight.Bold) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { tempHour = (tempHour + 23) % 24 }) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "减小时")
                        }
                        Text("%02d".format(tempHour), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { tempHour = (tempHour + 1) % 24 }) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "加小时")
                        }
                        Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { tempMinute = (tempMinute + 59) % 60 }) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "减分钟")
                        }
                        Text("%02d".format(tempMinute), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { tempMinute = (tempMinute + 1) % 60 }) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "加分钟")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        saveReminder(reminderEnabled, "%02d:%02d".format(tempHour, tempMinute))
                        if (reminderEnabled) {
                            checkAndRequestNotificationPermission {
                                setDailyReminder(context, tempHour, tempMinute)
                            }
                        }
                        showTimePicker = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("取消") }
                }
            )
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
                            .clickable {
                                Toast.makeText(
                                    context,
                                    "Keeping App Version: $currentVersion",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
                                    progress = { if (downloadTotal > 0) downloadCurrent / downloadTotal.toFloat() else 0f },
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
                                                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
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