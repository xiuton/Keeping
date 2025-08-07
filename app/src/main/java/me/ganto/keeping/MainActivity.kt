@file:OptIn(ExperimentalLayoutApi::class)
package me.ganto.keeping

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import me.ganto.keeping.theme.KeepingTheme
import me.ganto.keeping.navigation.NavGraph
import me.ganto.keeping.core.data.MainViewModel
import me.ganto.keeping.core.data.BackupManager
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge() // 允许内容延伸到系统栏下方
        
        // 安装启动画面
        val splashScreen = installSplashScreen()        

        // 检查是否从通知点击进入，需要跳转到新增账单界面
        val shouldOpenAddBill = intent.getBooleanExtra("open_add_bill", false)
        
        // 立即设置主题，避免白屏
        val window = window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        setContent {
            // 立即应用主题，避免白屏
            val systemDarkTheme = isSystemInDarkTheme()
            var currentDarkTheme by remember { mutableStateOf(systemDarkTheme) }
            
            // 保持启动画面直到数据加载完成
            var keepSplashScreen by remember { mutableStateOf(true) }
            
            LaunchedEffect(Unit) {
                // 延迟一小段时间确保界面完全加载
                kotlinx.coroutines.delay(100)
                keepSplashScreen = false
            }
            
            // 控制启动画面
            splashScreen.setKeepOnScreenCondition { keepSplashScreen }
            
            KeepingTheme(darkTheme = currentDarkTheme) {
                val context = LocalContext.current
                val viewModel: MainViewModel = viewModel()
                val scope = rememberCoroutineScope()
                var navIndex by rememberSaveable { mutableStateOf(0) }
                var showAddDialog by remember { mutableStateOf(false) }
                val backupManager = remember { BackupManager(context) }

                
                // 加载数据
                LaunchedEffect(Unit) {
                    viewModel.loadAllData(context)
                }
                
                // 当主题模式设置加载完成后，更新主题
                LaunchedEffect(viewModel.themeModeLoaded, viewModel.themeMode) {
                    if (viewModel.themeModeLoaded) {
                        currentDarkTheme = when (viewModel.themeMode) {
                            "light" -> false
                            "dark" -> true
                            else -> systemDarkTheme // auto 模式，使用之前获取的系统主题
                        }
                    }
                }
                
                // 直接显示主界面，不等待数据加载
                NavGraph(
                    bills = viewModel.bills,
                    saveBills = { newBills -> viewModel.saveBills(context, newBills) },
                    themeMode = viewModel.themeMode,
                    saveThemeMode = { mode -> viewModel.saveThemeMode(context, mode) },
                    sortBy = viewModel.sortBy,
                    saveSortBy = { sort -> viewModel.saveSortBy(context, sort) },
                    expenseCategories = viewModel.expenseCategories,
                    incomeCategories = viewModel.incomeCategories,
                    expensePayTypes = viewModel.expensePayTypes,
                    incomePayTypes = viewModel.incomePayTypes,
                    navIndex = navIndex,
                    setNavIndex = { navIndex = it },
                    showAddDialog = showAddDialog,
                    setShowAddDialog = { showAddDialog = it },
                    backupManager = backupManager,
                    collectSettingsData = { viewModel.collectSettingsData() },
                    shouldOpenAddBill = shouldOpenAddBill
                )
            }
        }
    }
} 