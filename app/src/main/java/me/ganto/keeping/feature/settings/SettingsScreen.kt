package me.ganto.keeping.feature.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.ganto.keeping.core.data.dataStore
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import android.os.Handler
import android.os.Looper
import java.io.File
import android.app.AlarmManager
import android.app.PendingIntent
import java.util.Calendar
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.app.NotificationCompat
import me.ganto.keeping.R
import android.app.NotificationManager
import android.app.NotificationChannel

import me.ganto.keeping.core.data.BackupManager
import me.ganto.keeping.core.data.BackupFileInfo
import me.ganto.keeping.core.model.BillItem
import me.ganto.keeping.feature.my.ReminderReceiver
import me.ganto.keeping.navigation.ROUTE_FEEDBACK
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextOverflow
import java.time.LocalTime
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SettingsScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    navController: NavController,
    innerPadding: PaddingValues,
    backupManager: BackupManager? = null,
    collectSettingsData: (() -> Map<String, String>)? = null,
    bills: List<BillItem> = emptyList(),
    saveBills: ((List<BillItem>) -> Unit)? = null
) {
    // 基本状态定义
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
    @SuppressLint("SuspiciousIndentation")
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

    // 下载文件带进度
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



    // 记账提醒相关
    var showTimeDialog by remember { mutableStateOf(false) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf("20:00") }
    var tempHour by remember { mutableStateOf(20) }
    var tempMinute by remember { mutableStateOf(0) }
    var selectedHour by remember { mutableStateOf(tempHour) }
    var selectedMinute by remember { mutableStateOf(tempMinute) }
    var showReminderTip by remember { mutableStateOf(false) }

    // DataStore键定义
    val REMINDER_ENABLED_KEY = stringPreferencesKey("reminder_enabled")
    val REMINDER_TIME_KEY = stringPreferencesKey("reminder_time")

    // 从DataStore读取提醒设置
    LaunchedEffect(Unit) {
        val enabled = context.dataStore.data.map { it[REMINDER_ENABLED_KEY] ?: "false" }.first()
        reminderEnabled = enabled == "true"
        val time = context.dataStore.data.map { it[REMINDER_TIME_KEY] ?: "20:00" }.first()
        reminderTime = time
        val parts = time.split(":")
        tempHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        tempMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    }

    fun saveReminder() {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[REMINDER_ENABLED_KEY] = if (reminderEnabled) "true" else "false"
                prefs[REMINDER_TIME_KEY] = reminderTime
            }
        }
        
        // 同时保存到SharedPreferences作为备选
        val sharedPrefs = context.getSharedPreferences("bills", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("reminder_enabled", if (reminderEnabled) "true" else "false")
            putString("reminder_time", reminderTime)
            apply()
        }
    }

    fun setDailyReminder() {
        try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "me.ganto.keeping.ACTION_REMIND"
            putExtra("hour", tempHour)
            putExtra("minute", tempMinute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
            
            // 先取消之前的闹钟
            alarmManager.cancel(pendingIntent)
            
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, tempHour)
            set(Calendar.MINUTE, tempMinute)
            set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
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
            
            // 显示设置成功的提示
            Toast.makeText(context, "提醒已设置: ${String.format("%02d:%02d", tempHour, tempMinute)}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "设置提醒失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelDailyReminder() {
        try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "me.ganto.keeping.ACTION_REMIND"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
            Toast.makeText(context, "提醒已取消", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "取消提醒失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showReminderTip = true
                return
            }
        }
        
        // 检查精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showReminderTip = true
                return
            }
        }
    }

    // 基本UI结构
    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() }
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // 账户与设置卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 记账提醒卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
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
                                        Column {
                            Text(
                                                "为保证定时提醒可靠，请在系统设置中：\n- 允许自启动\n- 设置电池无限制\n- 允许后台弹窗/通知\n否则在后台或被杀死时可能无法收到提醒。",
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    // 立即发送测试通知
                                                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                                    val channelId = "reminder_channel"
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        val channel = NotificationChannel(channelId, "记账提醒", NotificationManager.IMPORTANCE_HIGH)
                                                        notificationManager.createNotificationChannel(channel)
                                                    }
                                                    val notification = NotificationCompat.Builder(context, channelId)
                                                        .setContentTitle("记账提醒测试")
                                                        .setContentText("这是一条测试通知，提醒功能正常工作")
                                                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                                                        .setAutoCancel(true)
                                                        .build()
                                                    notificationManager.notify(1002, notification)
                                                    Toast.makeText(context, "测试通知已发送", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("测试通知")
                                            }
                                        }
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
                            onCheckedChange = { enabled ->
                                reminderEnabled = enabled
                                if (enabled) {
                                    checkAndRequestNotificationPermission()
                                    setDailyReminder()
                                } else {
                                    cancelDailyReminder()
                                }
                                saveReminder()
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
                                                                                     Button(onClick = {
                                val parts = reminderTime.split(":")
                                tempHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                                tempMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                selectedHour = tempHour
                                selectedMinute = tempMinute
                                    showTimeDialog = true
                            }) {
                                     Text("选择时间")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 数据备份管理卡片
            if (backupManager != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
                            Icon(Icons.Filled.Backup, contentDescription = "创建备份")
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
                            Icon(Icons.Filled.Restore, contentDescription = "从JSON恢复")
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
                                                    Text(
                                                        text = "创建时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(backupFile.timestamp))}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (backupFile.description.isNotBlank()) {
                                                        Text(
                                                            text = "描述: ${backupFile.description}",
                                                            fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                }
                                                Row {
                                                    IconButton(
                                                        onClick = { showRestoreDialog = backupFile }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Restore,
                                                            contentDescription = "恢复",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { showDeleteBackupDialog = backupFile }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Delete,
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
                            Text("暂无备份文件", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 关于与帮助卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
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
                Card(
                        modifier = Modifier
                            .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // 时间选择对话框
    if (showTimeDialog) {
        val hourListState = rememberLazyListState()
        val minuteListState = rememberLazyListState()
        
        // 时间选择器尺寸配置
        val timePickerContainerHeight = 120.dp
        val timePickerContainerWidth = 80.dp
        val timePickerItemHeight = 40.dp
        
        // 自动滚动到选中的时间，确保选中项居中显示
        val density = LocalDensity.current
        var containerHeightPx by remember { mutableStateOf(0) }
        var itemHeightPx by remember { mutableStateOf(0) }
        
        LaunchedEffect(showTimeDialog) {
            if (showTimeDialog) {
                val scrollOffsetPx = -(containerHeightPx / 2 - itemHeightPx / 2)
                hourListState.animateScrollToItem(
                    index = selectedHour,
                    scrollOffset = scrollOffsetPx
                )
                minuteListState.animateScrollToItem(
                    index = selectedMinute,
                    scrollOffset = scrollOffsetPx
                )
            }
        }
        
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            title = { Text("选择提醒时间") },
            text = {
                Column {
                    Text("当前时间: ${String.format("%02d:%02d", selectedHour, selectedMinute)}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 小时选择
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("时", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Box(
                                modifier = Modifier
                                    .height(timePickerContainerHeight)
                                    .width(timePickerContainerWidth)
                                    .onSizeChanged { size ->
                                        containerHeightPx = size.height
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LazyColumn(
                                    state = hourListState,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxSize()
                            ) {
                                items(24) { hour ->
                                        Box(
                                            modifier = Modifier
                                                .height(timePickerItemHeight)
                                                .fillMaxWidth()
                                                .onSizeChanged { size ->
                                                    itemHeightPx = size.height
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = String.format("%02d", hour),
                                                fontSize = if (selectedHour == hour) 20.sp else 14.sp,
                                                fontWeight = if (selectedHour == hour) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedHour == hour) 
                                                MaterialTheme.colorScheme.onSurface
                                                else 
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.clickable { selectedHour = hour }
                                        )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 分钟选择
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("分", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Box(
                                modifier = Modifier
                                    .height(timePickerContainerHeight)
                                    .width(timePickerContainerWidth),
                                contentAlignment = Alignment.Center
                            ) {
                                LazyColumn(
                                    state = minuteListState,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxSize()
                            ) {
                                items(60) { minute ->
                                        Box(
                                            modifier = Modifier
                                                .height(timePickerItemHeight)
                                                .fillMaxWidth()
                                                .onSizeChanged { size ->
                                                    itemHeightPx = size.height
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = String.format("%02d", minute),
                                                fontSize = if (selectedMinute == minute) 20.sp else 14.sp,
                                                fontWeight = if (selectedMinute == minute) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedMinute == minute) 
                                                MaterialTheme.colorScheme.onSurface
                                                else 
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.clickable { selectedMinute = minute }
                                        )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tempHour = selectedHour
                        tempMinute = selectedMinute
                        reminderTime = String.format("%02d:%02d", tempHour, tempMinute)
                        if (reminderEnabled) setDailyReminder()
                        saveReminder()
                        showTimeDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimeDialog = false }
                ) {
                    Text("取消")
                }
            }
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { createBackup() },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "创建中..." else "创建")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackupDialog = false },
                    enabled = !isLoading
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 恢复备份确认对话框
    if (showRestoreDialog != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text("恢复备份") },
            text = {
                Text("确定要恢复备份文件 \"${showRestoreDialog?.fileName}\" 吗？\n这将覆盖当前的所有数据。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog?.let { restoreBackup(it) }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "恢复中..." else "恢复")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreDialog = null },
                    enabled = !isLoading
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 删除备份确认对话框
    if (showDeleteBackupDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteBackupDialog = null },
            title = { Text("删除备份") },
            text = {
                Text("确定要删除备份文件 \"${showDeleteBackupDialog?.fileName}\" 吗？\n此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showDeleteBackupDialog?.let { deleteBackup(it) }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "删除中..." else "删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteBackupDialog = null },
                    enabled = !isLoading
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 备份路径信息对话框
    if (showBackupPathDialog) {
        AlertDialog(
            onDismissRequest = { showBackupPathDialog = false },
            title = { Text("备份文件位置") },
            text = {
                Text("备份文件保存在：\n${context.getExternalFilesDir(null)}/backups/")
            },
            confirmButton = {
                TextButton(onClick = { showBackupPathDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    // 消息提示对话框
    if (showMessage.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showMessage = "" },
            title = { Text("提示") },
            text = { Text(showMessage) },
            confirmButton = {
                TextButton(onClick = { showMessage = "" }) {
                    Text("确定")
                }
            }
        )
    }

    // 提醒权限提示对话框
    if (showReminderTip) {
        AlertDialog(
            onDismissRequest = { showReminderTip = false },
            title = { Text("权限设置", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "为保证记账提醒正常工作，请在系统设置中：\n\n" +
                    "1. 允许通知权限\n" +
                    "2. 允许精确闹钟权限\n" +
                    "3. 允许自启动\n" +
                    "4. 设置电池无限制\n" +
                    "5. 允许后台弹窗/通知\n\n" +
                    "否则在后台或被杀死时可能无法收到提醒。",
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
                    showReminderTip = false
                }) {
                    Text("前往系统设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTip = false }) {
                    Text("稍后设置")
                }
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
                        8 -> { // 下载完成，显示"去安装"与"安装后删除"按钮
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
                        16 -> { // 下载失败，显示"关闭"
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
                        0 -> { // 未下载，显示"下载"和"取消"
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
                                    downloadStatus = 1 // 立即切换到"下载中"分支
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
                            // 下载中，不显示按钮或可选显示灰色"下载中..."
                        }
                        8 -> { // 下载完成，显示"去安装"按钮
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
                        16 -> { // 下载失败，显示"关闭"
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
} 