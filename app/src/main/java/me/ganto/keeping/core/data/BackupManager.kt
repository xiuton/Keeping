package me.ganto.keeping.core.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ganto.keeping.core.model.BillItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class BackupData(
    val version: String = "1.0",
    val timestamp: Long = System.currentTimeMillis(),
    val bills: List<BillItem> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
    val dataHash: String = "",
    val description: String = ""
)

class BackupManager(private val context: Context) {
    private val gson = Gson()
    private val backupDir = File(context.filesDir, "backups")
    private val externalBackupDir = File(context.getExternalFilesDir(null), "backups")
    
    init {
        if (!backupDir.exists()) {
            val created = backupDir.mkdirs()
            Log.d("BackupManager", "内部备份目录创建: $created, 路径: ${backupDir.absolutePath}")
        }
        if (!externalBackupDir.exists()) {
            val created = externalBackupDir.mkdirs()
            Log.d("BackupManager", "外部备份目录创建: $created, 路径: ${externalBackupDir.absolutePath}")
        }
    }
    
    /**
     * 创建备份文件
     */
    suspend fun createBackup(
        bills: List<BillItem>,
        settings: Map<String, String>,
        description: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "keeping_backup_${dateFormat.format(Date(timestamp))}.json"
            
            // 同时保存到内部和外部存储
            val internalBackupFile = File(backupDir, fileName)
            val externalBackupFile = File(externalBackupDir, fileName)
            
            // 计算数据哈希用于验证
            val dataHash = calculateDataHash(bills, settings)
            
            val backupData = BackupData(
                version = "1.0",
                timestamp = timestamp,
                bills = bills,
                settings = settings,
                dataHash = dataHash,
                description = description
            )
            
            val json = gson.toJson(backupData)
            
            // 保存到内部存储
            internalBackupFile.writeText(json)
            Log.d("BackupManager", "内部备份创建成功: ${internalBackupFile.absolutePath}")
            
            // 保存到外部存储（应用专属外部存储）
            try {
                externalBackupFile.writeText(json)
                Log.d("BackupManager", "外部备份创建成功: ${externalBackupFile.absolutePath}")
            } catch (e: Exception) {
                Log.w("BackupManager", "外部备份创建失败，但内部备份成功", e)
            }
            
            // 尝试保存到Downloads文件夹（需要权限）
            try {
                val downloadsDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "Keeping_Backups")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val downloadsBackupFile = File(downloadsDir, fileName)
                downloadsBackupFile.writeText(json)
                Log.d("BackupManager", "Downloads备份创建成功: ${downloadsBackupFile.absolutePath}")
            } catch (e: Exception) {
                Log.w("BackupManager", "Downloads备份创建失败", e)
            }
            
            Result.success(fileName)
        } catch (e: Exception) {
            Log.e("BackupManager", "创建备份失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 读取备份文件
     */
    suspend fun readBackup(fileName: String): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupDir, fileName)
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("备份文件不存在"))
            }
            
            val json = backupFile.readText()
            val backupData = gson.fromJson(json, BackupData::class.java)
            
            // 验证数据完整性
            val expectedHash = calculateDataHash(backupData.bills, backupData.settings)
            if (backupData.dataHash != expectedHash) {
                return@withContext Result.failure(Exception("备份文件数据损坏"))
            }
            
            Result.success(backupData)
        } catch (e: Exception) {
            Log.e("BackupManager", "读取备份失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取所有备份文件列表
     */
    suspend fun getBackupFiles(): Result<List<BackupFileInfo>> = withContext(Dispatchers.IO) {
        try {
            // 获取内部存储的备份文件
            val internalFiles = backupDir.listFiles { file ->
                file.isFile && file.name.endsWith(".json")
            } ?: emptyArray()
            
            Log.d("BackupManager", "内部备份目录: ${backupDir.absolutePath}")
            Log.d("BackupManager", "找到内部备份文件数量: ${internalFiles.size}")
            
            val backupFiles = internalFiles.mapNotNull { file ->
                try {
                    Log.d("BackupManager", "处理备份文件: ${file.name}, 大小: ${file.length()}")
                    val json = file.readText()
                    val backupData = gson.fromJson(json, BackupData::class.java)
                    BackupFileInfo(
                        fileName = file.name,
                        timestamp = backupData.timestamp,
                        description = backupData.description,
                        billsCount = backupData.bills.size,
                        fileSize = file.length()
                    )
                } catch (e: Exception) {
                    Log.w("BackupManager", "解析备份文件失败: ${file.name}", e)
                    null
                }
            }.sortedByDescending { it.timestamp }
            
            Log.d("BackupManager", "成功解析备份文件数量: ${backupFiles.size}")
            Result.success(backupFiles)
        } catch (e: Exception) {
            Log.e("BackupManager", "获取备份文件列表失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 删除备份文件
     */
    suspend fun deleteBackup(fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupDir, fileName)
            if (backupFile.exists()) {
                backupFile.delete()
                Log.d("BackupManager", "备份文件删除成功: $fileName")
                Result.success(Unit)
            } else {
                Result.failure(Exception("备份文件不存在"))
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "删除备份文件失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 验证当前数据是否需要备份（防止覆盖有效数据）
     */
    suspend fun shouldCreateBackup(
        currentBills: List<BillItem>,
        currentSettings: Map<String, String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupFiles = getBackupFiles().getOrNull() ?: return@withContext true
            
            // 如果没有备份文件，需要创建
            if (backupFiles.isEmpty()) return@withContext true
            
            // 获取最新的备份
            val latestBackup = backupFiles.first()
            val latestBackupData = readBackup(latestBackup.fileName).getOrNull() ?: return@withContext true
            
            // 比较数据
            val currentHash = calculateDataHash(currentBills, currentSettings)
            val backupHash = latestBackupData.dataHash
            
            // 如果数据不同，需要创建备份
            currentHash != backupHash
        } catch (e: Exception) {
            Log.e("BackupManager", "验证备份需求失败", e)
            true // 出错时默认需要备份
        }
    }
    
    /**
     * 智能恢复数据（检查数据完整性）
     */
    suspend fun restoreData(fileName: String): Result<Pair<List<BillItem>, Map<String, String>>> = withContext(Dispatchers.IO) {
        try {
            val backupData = readBackup(fileName).getOrThrow()
            
            // 验证数据完整性
            if (backupData.bills.isEmpty() && backupData.settings.isEmpty()) {
                return@withContext Result.failure(Exception("备份文件为空"))
            }
            
            Result.success(Pair(backupData.bills, backupData.settings))
        } catch (e: Exception) {
            Log.e("BackupManager", "恢复数据失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取备份文件路径信息
     */
    fun getBackupPaths(): Map<String, String> {
        return mapOf(
            "internal_path" to backupDir.absolutePath,
            "external_path" to externalBackupDir.absolutePath,
            "downloads_path" to File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "Keeping_Backups").absolutePath
        )
    }
    
    /**
     * 检查备份文件是否存在
     */
    fun checkBackupFileExists(fileName: String): Map<String, Boolean> {
        val internalFile = File(backupDir, fileName)
        val externalFile = File(externalBackupDir, fileName)
        val downloadsFile = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "Keeping_Backups/$fileName")
        
        return mapOf(
            "internal_exists" to internalFile.exists(),
            "external_exists" to externalFile.exists(),
            "downloads_exists" to downloadsFile.exists()
        )
    }
    
    /**
     * 计算数据哈希值
     */
    private fun calculateDataHash(bills: List<BillItem>, settings: Map<String, String>): String {
        val billsJson = gson.toJson(bills)
        val settingsJson = gson.toJson(settings)
        val combinedData = "$billsJson$settingsJson"
        return combinedData.hashCode().toString()
    }
}

data class BackupFileInfo(
    val fileName: String,
    val timestamp: Long,
    val description: String,
    val billsCount: Int,
    val fileSize: Long
) 