package com.example.novel_r.ui.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.LooksOne
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.novel_r.ui.theme.NovelRTheme
import com.example.novel_r.ui.viewmodel.PlayerViewModel
import com.example.novel_r.ui.viewmodel.PlaybackScope
import com.example.novel_r.ui.viewmodel.PlaybackAction
import com.example.novel_r.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    filePath: String? = null,
    fileTitle: String? = null,
    viewModel: PlayerViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf<Boolean>(false) }

    // 當傳入新的 filePath 時，自動開始播放
    LaunchedEffect(filePath) {
        if (filePath != null) {
            viewModel.playMedia(filePath, title = fileTitle)
        }
    }
    
    // 使用支援鎖定模式的主題
    NovelRTheme(isLockMode = uiState.isLockMode) {
        // 根據鎖定狀態決定某些 UI 元素的顏色或可見性
        // 注意：NovelRTheme 已經會改變 MaterialTheme.colorScheme 的背景色與前景色
        
        Scaffold(
            topBar = {
                // 鎖定模式下，隱藏或簡化 TopBar
                if (!uiState.isLockMode) {
                    CenterAlignedTopAppBar(
                        title = { Text("正在播放") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                } else {
                    // 鎖定模式下的簡易頂部（僅顯示標題與解鎖按鈕）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "鎖定中",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { viewModel.toggleLockMode() }) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "解鎖",
                                tint = MaterialTheme.colorScheme.error // 紅色鎖頭以示醒目
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background // 確保背景色跟隨主題
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 佔位空間，將控制項向下推，但保留上方空間
                Spacer(modifier = Modifier.weight(1f))

                // 2. 進度條區域
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = if (uiState.duration > 0) uiState.currentPosition.toFloat() else 0f,
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..(uiState.duration.toFloat().coerceAtLeast(1f)),
                        enabled = !uiState.isLockMode,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = TimeFormatter.formatTime(uiState.currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = TimeFormatter.formatTime(uiState.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // 3. 自定義快進/快退按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    JumpButton(text = "-1m", icon = Icons.Rounded.Replay30, onClick = { viewModel.rewind(60) }, enabled = !uiState.isLockMode)
                    JumpButton(text = "-30s", icon = Icons.Rounded.Replay10, onClick = { viewModel.rewind(30) }, enabled = !uiState.isLockMode)
                    JumpButton(text = "+30s", icon = Icons.Rounded.Forward10, onClick = { viewModel.fastForward(30) }, enabled = !uiState.isLockMode)
                    JumpButton(text = "+1m", icon = Icons.Rounded.Forward30, onClick = { viewModel.fastForward(60) }, enabled = !uiState.isLockMode)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Spacer(modifier = Modifier.height(32.dp))

                // 4. 核心播放控制 (包含範圍與行為切換)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左側按鈕：範圍切換 (Single/List)
                    IconButton(
                        onClick = { viewModel.togglePlaybackScope() },
                        enabled = !uiState.isLockMode
                    ) {
                        val icon = if (uiState.playbackScope == PlaybackScope.SINGLE) Icons.Rounded.LooksOne else Icons.Rounded.FormatListBulleted
                        Icon(
                            icon,
                            contentDescription = "切換範圍",
                             tint = if (uiState.isLockMode) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) 
                                   else if (uiState.playbackScope == PlaybackScope.SINGLE) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // 上一首
                    IconButton(
                        onClick = { viewModel.skipToPrevious() },
                        enabled = !uiState.isLockMode,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "上一首",
                            modifier = Modifier.size(43.dp),
                            tint = if (uiState.isLockMode) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // 播放/暫停
                    FloatingActionButton(
                        onClick = { viewModel.togglePlayPause() },
                        containerColor = if (uiState.isLockMode) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary,
                        contentColor = if (uiState.isLockMode) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "暫停" else "播放",
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    // 下一首
                    IconButton(
                        onClick = { viewModel.skipToNext() },
                        enabled = !uiState.isLockMode,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "下一首",
                            modifier = Modifier.size(43.dp),
                            tint = if (uiState.isLockMode) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // 右側按鈕：行為切換 (Stop/Loop)
                    IconButton(
                        onClick = { viewModel.togglePlaybackAction() },
                        enabled = !uiState.isLockMode
                    ) {
                         val icon = if (uiState.playbackAction == PlaybackAction.STOP) Icons.Rounded.StopCircle else Icons.Rounded.Repeat
                         Icon(
                             icon,
                             contentDescription = "切換行為",
                             tint = if (uiState.isLockMode) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) 
                                   else if (uiState.playbackAction == PlaybackAction.LOOP) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onBackground
                         )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 5. 底部工具列 (鎖定、速度、定時)
                if (!uiState.isLockMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically // 垂直置中
                    ) {
                        // 鎖定按鈕
                        IconButton(
                            onClick = { viewModel.toggleLockMode() },
                            modifier = Modifier.size(72.dp) // 放大按鈕觸控區
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.LockOpen, 
                                    contentDescription = "鎖定",
                                    modifier = Modifier.size(36.dp) // 放大圖示
                                )
                                Text("鎖定", style = MaterialTheme.typography.titleSmall) // 放大文字
                            }
                        }
                        
                        // 速度控制
                        IconButton(
                            onClick = { showSpeedSheet = true },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.Speed, 
                                    contentDescription = "速度",
                                    modifier = Modifier.size(36.dp)
                                )
                                Text("${uiState.playbackSpeed}x", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        
                        // 睡眠計時器
                        IconButton(
                            onClick = { showSleepSheet = true },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val isTimerActive = uiState.sleepTimer != null
                                val timerIconTint = if (isTimerActive) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "TimerFlash")
                                    val color by infiniteTransition.animateColor(
                                        initialValue = MaterialTheme.colorScheme.onBackground,
                                        targetValue = Color.Red, // 改為紅色
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(500, easing = LinearEasing), // 加快閃爍頻率
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "Color"
                                    )
                                    color
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                }
                                
                                Icon(
                                    Icons.Rounded.Timer, 
                                    contentDescription = "定時",
                                    tint = timerIconTint,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    if (isTimerActive) {
                                        val sec = uiState.sleepTimer!!
                                        if (sec < 60) "${sec}s" else "${sec / 60}m"
                                    } else "關閉",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isTimerActive) Color.Red else MaterialTheme.colorScheme.onBackground // 文字也變紅
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(64.dp)) // 增加佔位高度以匹配放大的工具列
                }

                Spacer(modifier = Modifier.weight(1f))

                // 1. 標題與作者區域 (移到底部)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.currentTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 3, // 限制行數
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "未知作者",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                }
            }
        

        // 錯誤提示 Dialog
        if (uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("錯誤") },
                text = { Text(uiState.errorMessage ?: "發生未知錯誤") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("確定")
                    }
                }
            )
        }
        
        // 速度控制 Sheet
        if (showSpeedSheet) {
            ModalBottomSheet(onDismissRequest = { showSpeedSheet = false }) {
                SpeedControlSheet(
                    currentSpeed = uiState.playbackSpeed,
                    onSpeedSelected = { 
                        viewModel.setPlaybackSpeed(it)
                        showSpeedSheet = false
                    }
                )
            }
        }
        
        // 睡眠計時器 Sheet
        if (showSleepSheet) {
            ModalBottomSheet(onDismissRequest = { showSleepSheet = false }) {
                SleepTimerSheet(
                    onTimerSelected = { seconds ->
                        viewModel.setSleepTimer(seconds)
                        showSleepSheet = false // 選擇後關閉 Sheet
                    },
                    onCancel = {
                        viewModel.cancelSleepTimer()
                        showSleepSheet = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedControlSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "播放速度",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            speeds.forEach { speed ->
                FilterChip(
                    selected = speed == currentSpeed,
                    onClick = { onSpeedSelected(speed) },
                    label = { Text("${speed}x") }
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SleepTimerSheet(
    onTimerSelected: (Long) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "睡眠計時器",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // 原始選項 (15分, 30分, 45分, 60分) - 保持不變，將 90分移除以騰出空間或保持一致性，
        // 或者保留 90分。 用戶只要求 "下面一排幫我加上 30秒 ... 10分"
        // 假設原始是上面一排，現在加下面一排。
        
        val longIntervals = listOf(15, 30, 45, 60)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            longIntervals.forEach { minutes ->
                OutlinedButton(onClick = { onTimerSelected(minutes * 60L) }) {
                    Text("${minutes}分")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 新增選項 (30秒, 1分, 5分, 10分)
        val shortIntervals = listOf(
            30L to "30秒", 
            60L to "1分", 
            300L to "5分", 
            600L to "10分"
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            shortIntervals.forEach { (seconds, label) ->
                OutlinedButton(onClick = { onTimerSelected(seconds) }) {
                    Text(label)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("關閉計時器")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun JumpButton(
    text: String, 
    icon: ImageVector, 
    onClick: () -> Unit, 
    enabled: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                icon, 
                contentDescription = text,
                modifier = Modifier.size(43.dp), // 90% of Standard 48dp IconButton
                tint = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
        Text(
            text, 
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
    }
}
