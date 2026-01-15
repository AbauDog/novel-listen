package com.example.novel_r.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.novel_r.ui.viewmodel.FileListViewModel
import com.example.novel_r.util.FileUtils
import com.example.novel_r.util.TimeFormatter
import androidx.compose.animation.core.*

/**
 * 檔案列表畫面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    viewModel: FileListViewModel = viewModel(),
    playerViewModel: com.example.novel_r.ui.viewmodel.PlayerViewModel = viewModel(),
    onFileSelected: (String, String) -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()
    
    // 刪除確認對話框狀態
    var fileToDelete by remember { mutableStateOf<com.example.novel_r.data.local.AudioFile?>(null) }
    // 選項對話框狀態 (長按時顯示)
    var fileForOptions by remember { mutableStateOf<com.example.novel_r.data.local.AudioFile?>(null) }
    // 清除全部確認對話框
    var showClearAllDialog by remember { mutableStateOf(false) }
    
    // 資料夾選擇器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.scanFolder(it) }
    }
    
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("刪除檔案") },
            text = { Text("確定要刪除「${fileToDelete?.fileName}」嗎？此操作無法復原。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.let { viewModel.deleteFile(it) }
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 長按選項對話框
    if (fileForOptions != null) {
        AlertDialog(
            onDismissRequest = { fileForOptions = null },
            title = { Text("選擇操作") },
            text = { 
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("針對「${fileForOptions?.fileName}」您想要執行什麼動作？")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 從列表移除
                    Button(
                        onClick = {
                            fileForOptions?.let { viewModel.removeFile(it) }
                            fileForOptions = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("從列表清除 (保留檔案)")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 永久刪除
                    Button(
                        onClick = {
                            // 轉移到刪除確認流程
                            fileToDelete = fileForOptions
                            fileForOptions = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("永久刪除檔案")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { fileForOptions = null }
                ) {
                    Text("取消")
                }
            },
            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    
    // 新增 URL 對話框
    var showAddUrlDialog by remember { mutableStateOf(false) }
    // 更多選項選單
    var showMenu by remember { mutableStateOf(false) }
    
    // 清除全部確認對話框
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清除全部") },
            text = { Text("確定要清除列表並重置所有權限嗎？\n這將清空目前列表，這能解決「Source error」問題。\n(不會刪除您的實體檔案)") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllFiles()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清除重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showAddUrlDialog) {
        com.example.novel_r.ui.components.AddUrlDialog(
            onDismiss = { showAddUrlDialog = false },
            onConfirm = { url ->
                viewModel.processYouTubeUrl(url)
                showAddUrlDialog = false
            }
        )
    }

    if (uiState.showPlaylistSelectionDialog) {
        // ... (existing dialog code)
        com.example.novel_r.ui.components.PlaylistSelectionDialog(
            playlist = uiState.playlistCandidates,
            onConfirm = { selected ->
                viewModel.confirmPlaylistSelection(selected)
            },
            onDismiss = {
                viewModel.cancelPlaylistSelection()
            }
        )
    }

    if (uiState.showBookmarkDialog) {
        com.example.novel_r.ui.components.BookmarkListDialog(
            bookmarks = uiState.bookmarks,
            onSelect = { bookmark ->
                viewModel.processYouTubeUrl(bookmark.url)
                viewModel.toggleBookmarkDialog(false)
            },
            onDelete = { bookmark ->
                viewModel.deleteBookmark(bookmark)
            },
            onDismiss = {
                viewModel.toggleBookmarkDialog(false)
            }
        )
    }
    
    // ... (existing update dialog)
    if (uiState.isUpdatingYtDlp) {
        // ...
        AlertDialog(
            onDismissRequest = { /* 禁止關閉 */ },
            title = { Text("正在更新元件") },
            text = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在下載最新的 YouTube 解析核心，請稍候...")
                }
            },
            confirmButton = {}
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音訊檔案列表") },
                actions = {
                    // 新增 URL 按鈕
                    IconButton(onClick = { showAddUrlDialog = true }) {
                        Icon(Icons.Default.AddLink, contentDescription = "新增 URL")
                    }
                    
                    // 我的最愛按鈕
                    IconButton(onClick = { viewModel.toggleBookmarkDialog(true) }) {
                        Icon(Icons.Default.Bookmarks, contentDescription = "我的最愛")
                    }

                    // 重新整理按鈕
                    IconButton(onClick = { 
                        uiState.selectedFolderUri?.let { 
                            viewModel.scanFolder(Uri.parse(it))
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新整理")
                    }

                    // 更多選項
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多選項")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("清除全部檔案") },
                                onClick = {
                                    showMenu = false
                                    showClearAllDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("更新 YouTube 元件") },
                                onClick = {
                                    showMenu = false
                                    viewModel.updateYtDlp()
                                },
                                leadingIcon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) }
                            )
                        }
                    }
                    
                    // 播放器按鈕
                    IconButton(onClick = onNavigateToPlayer) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "播放器")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { folderPickerLauncher.launch(null) }) {
                Icon(Icons.Default.FolderOpen, contentDescription = "選擇資料夾")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.clearError() }) {
                            Text("確定")
                        }
                    }
                }
                
                uiState.audioFiles.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "尚未選擇資料夾",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "點擊右下角按鈕選擇包含音訊檔案的資料夾",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.audioFiles) { audioFile ->
                            val isPlaying = playerUiState.currentFilePath == audioFile.filePath
                            
                            // 如果是正在播放的檔案，使用 PlayerViewModel 的即時進度
                            // 否則使用資料庫中的儲存進度
                            val displayProgress = if (isPlaying) {
                                // 只有當有有效的 duration 時才建立合成的 Progress 物件
                                if (playerUiState.duration > 0) {
                                    com.example.novel_r.data.local.AudioProgress(
                                        filePath = audioFile.filePath,
                                        fileName = audioFile.fileName,
                                        fileSize = audioFile.fileSize,
                                        lastPosition = playerUiState.currentPosition, // 即時位置
                                        duration = playerUiState.duration,           // 即時總長度
                                        lastPlayedTimestamp = System.currentTimeMillis()
                                    )
                                } else {
                                    viewModel.getProgress(audioFile.filePath)
                                }
                            } else {
                                viewModel.getProgress(audioFile.filePath)
                            }

                            AudioFileItem(
                                fileName = audioFile.fileName,
                                filePath = audioFile.filePath,
                                fileSize = audioFile.fileSize,
                                duration = audioFile.duration,
                                isStream = audioFile.isStream,
                                progress = displayProgress,
                                isCurrentlyPlaying = isPlaying, 
                                onClick = {
                                    onFileSelected(audioFile.filePath, audioFile.fileName)
                                },
                                onLongClick = {
                                    fileForOptions = audioFile
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 音訊檔案項目
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AudioFileItem(
    fileName: String,
    filePath: String,
    fileSize: Long,
    duration: Long,
    isStream: Boolean = false,
    progress: com.example.novel_r.data.local.AudioProgress?,
    isCurrentlyPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // 如果正在播放或有播放進度，則使用藍色
    val shouldHighlight = isCurrentlyPlaying || progress != null
    
    // 播放中圖標的動畫
    val infiniteTransition = rememberInfiniteTransition(label = "playing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 檔案名稱和播放圖標
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 如果正在播放，顯示動畫圖標
                if (isCurrentlyPlaying) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "正在播放",
                        tint = androidx.compose.ui.graphics.Color(0xFF2196F3).copy(alpha = alpha),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    // 顯示檔案類型圖標
                    Icon(
                        imageVector = if (isStream) Icons.Default.Cloud else Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = if (shouldHighlight) androidx.compose.ui.graphics.Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (shouldHighlight) androidx.compose.ui.graphics.Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 檔案資訊
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = FileUtils.formatFileSize(fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = TimeFormatter.formatTime(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 播放進度（如果有）
            if (progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = TimeFormatter.calculateProgress(progress.lastPosition, progress.duration),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "已播放: ${TimeFormatter.formatProgress(progress.lastPosition, progress.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                    )
                    
                    Text(
                        text = "剩餘: ${TimeFormatter.formatRemainingTime(progress.lastPosition, progress.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
