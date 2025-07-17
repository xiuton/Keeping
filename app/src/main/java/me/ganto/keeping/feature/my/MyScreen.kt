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

@Composable
fun MyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    val currentVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) { "1.0.0" }
    var isDark by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部头像和昵称
        Spacer(Modifier.height(24.dp))
        Card(
            shape = CircleShape,
            modifier = Modifier.size(90.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "头像",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("未登录", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(24.dp))

        // 账户与设置
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("深色模式", fontSize = 16.sp)
                    Switch(checked = isDark, onCheckedChange = {
                        isDark = it
                        // 你可以在这里同步到全局或DataStore
                    })
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
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
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { downloadAndInstall(updateUrl) }) { Text("下载并安装") }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 关于与帮助
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { Toast.makeText(context, "敬请期待", Toast.LENGTH_SHORT).show() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("意见反馈", fontSize = 16.sp)
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { Toast.makeText(context, "Keeping 记账App", Toast.LENGTH_SHORT).show() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("关于App", fontSize = 16.sp)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = "版本号：$currentVersion",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
    }
} 