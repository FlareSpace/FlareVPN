package flare.client.app.ui.components

import flare.client.app.ui.i18n.I18n

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import flare.client.app.R
import flare.client.app.ui.notification.AppNotificationManager
import flare.client.app.ui.notification.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import flare.client.app.ui.theme.FlareTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import flare.client.app.ui.components.flareGlass
import flare.client.app.ui.components.bottomNavSoftShadow
import androidx.compose.ui.draw.clip

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, DEFAULT
}

data class ParsedLogLine(
    val raw: String,
    val timestamp: String,
    val level: LogLevel,
    val message: String
)

private fun parseLogLine(line: String): ParsedLogLine {
    val trimmed = line.trim()
    var level = LogLevel.DEFAULT
    var timestamp = ""
    var message = trimmed
    
    
    val infoIndex = trimmed.indexOf("INFO", ignoreCase = true)
    val warnIndex = trimmed.indexOf("WARN", ignoreCase = true)
    val errorIndex = trimmed.indexOf("ERROR", ignoreCase = true)
    val fatalIndex = trimmed.indexOf("FATAL", ignoreCase = true)
    val debugIndex = trimmed.indexOf("DEBUG", ignoreCase = true)
    
    if (errorIndex >= 0 || fatalIndex >= 0) {
        level = LogLevel.ERROR
    } else if (warnIndex >= 0) {
        level = LogLevel.WARN
    } else if (infoIndex >= 0) {
        level = LogLevel.INFO
    } else if (debugIndex >= 0) {
        level = LogLevel.DEBUG
    }
    
    
    if (trimmed.length >= 19 && (trimmed[4] == '-' || trimmed[4] == '/') && (trimmed[7] == '-' || trimmed[7] == '/')) {
        timestamp = trimmed.substring(0, 19)
        message = trimmed.substring(19).trim()
    } else if (trimmed.startsWith("INFO[") || trimmed.startsWith("WARN[") || trimmed.startsWith("ERR[") || trimmed.startsWith("DBG[")) {
        val closeBracket = trimmed.indexOf(']')
        if (closeBracket > 0) {
            timestamp = trimmed.substring(0, closeBracket + 1)
            message = trimmed.substring(closeBracket + 1).trim()
        }
    }
    
    if (message.startsWith("[INFO]") || message.startsWith("[WARN]") || message.startsWith("[ERROR]") || message.startsWith("[DEBUG]")) {
        message = message.substring(message.indexOf(']') + 1).trim()
    } else if (message.startsWith("INFO:") || message.startsWith("WARN:") || message.startsWith("ERROR:") || message.startsWith("DEBUG:")) {
        message = message.substring(message.indexOf(':') + 1).trim()
    }
    
    return ParsedLogLine(line, timestamp, level, message)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    logFile: File,
    accentColor: Color,
    onBack: () -> Unit,
    hazeState: HazeState
) {
    val logsList = remember { mutableStateListOf<ParsedLogLine>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        val MAX_LOG_LINES = 300
        var fileReadOffset = 0L

        while (isActive) {
            if (!logFile.exists()) {
                delay(1000)
                continue
            }

            try {
                val fileLength = logFile.length()
                if (fileLength < fileReadOffset) {
                    logsList.clear()
                    fileReadOffset = 0L
                }

                if (fileLength > fileReadOffset) {
                    val startSeek = if (fileReadOffset == 0L) {
                        maxOf(0L, fileLength - 128 * 1024)
                    } else {
                        fileReadOffset
                    }

                    val newParsedLines = withContext(Dispatchers.IO) {
                        mutableListOf<ParsedLogLine>().apply {
                            RandomAccessFile(logFile, "r").use { raf ->
                                raf.seek(startSeek)
                                val reader = java.io.InputStreamReader(java.io.FileInputStream(raf.fd), Charsets.UTF_8).buffered()
                                if (startSeek > 0L) {
                                    reader.readLine()
                                }
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    val raw = line!!
                                    if (raw.isNotBlank()) {
                                        add(parseLogLine(raw))
                                    }
                                }
                            }
                        }
                    }

                    fileReadOffset = fileLength

                    if (newParsedLines.isNotEmpty()) {
                        if (logsList.size + newParsedLines.size > MAX_LOG_LINES) {
                            val toRemove = (logsList.size + newParsedLines.size) - MAX_LOG_LINES
                            repeat(minOf(toRemove, logsList.size)) {
                                logsList.removeAt(0)
                            }
                        }
                        logsList.addAll(newParsedLines)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(1000)
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                true
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
            }
        }
    }

    LaunchedEffect(logsList.size) {
        if (logsList.isNotEmpty() && isAtBottom) {
            listState.scrollToItem(logsList.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .hazeSource(state = hazeState)
        ) {
            if (logsList.isEmpty()) {
                Text(
                    text = I18n.strings.journal_waiting_logs,
                    color = FlareTheme.colors.textSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 68.dp,
                            bottom = 112.dp
                        )
                    ) {
                        itemsIndexed(
                            items = logsList,
                            key = { index, item -> "${index}_${item.raw.hashCode()}" }
                        ) { _, parsed ->
                            LogLineItem(parsed)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isAtBottom && logsList.isNotEmpty(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 96.dp)
        ) {
            val isDarkTheme = FlareTheme.colors.isDark
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .bottomNavSoftShadow(isDarkTheme, cornersRadius = 20.dp)
                    .clip(CircleShape)
                    .clickable {
                        scope.launch {
                            if (logsList.isNotEmpty()) {
                                listState.animateScrollToItem(logsList.size - 1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(1.dp)
                        .flareGlass(
                            isDark = isDarkTheme,
                            radius = 20f,
                            intensity = 1.6f,
                            index = 1.5f,
                            glassHeight = 0.5f,
                            thickness = 5f,
                            hasOutline = false
                        )
                        .hazeEffect(state = hazeState) {
                            blurRadius = 2.5.dp
                        }
                        .background(
                            color = if (isDarkTheme) Color(0xA0202228) else Color(0xA0FFFFFF),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = FlareTheme.colors.glassStroke,
                            shape = CircleShape
                        )
                )

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = "Scroll to bottom",
                    tint = FlareTheme.colors.navIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        FlareTopBar(
            title = I18n.strings.journal_title,
            hazeState = hazeState,
            lazyListState = listState,
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        val fullLogs = logsList.joinToString("\n") { it.raw }
                        if (fullLogs.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(fullLogs))
                            AppNotificationManager.showNotification(
                                NotificationType.SUCCESS,
                                I18n.strings.journal_copy_success,
                                3
                            )
                        }
                    },
                    enabled = logsList.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = "Copy all",
                        tint = if (logsList.isNotEmpty()) FlareTheme.colors.textPrimary else FlareTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )
                }

                IconButton(onClick = {
                    try {
                        if (logFile.exists()) logFile.writeText("")
                        logsList.clear()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = FlareTheme.colors.textSecondary
                    )
                }
            }
        )
    }
}

@Composable
fun LogLineItem(parsed: ParsedLogLine) {
    val levelColor = when (parsed.level) {
        LogLevel.DEBUG -> FlareTheme.colors.textSecondary.copy(alpha = 0.7f)
        LogLevel.INFO -> Color(0xFF34C759)
        LogLevel.WARN -> Color(0xFFFF9500)
        LogLevel.ERROR -> Color(0xFFFF3B30)
        LogLevel.DEFAULT -> FlareTheme.colors.textPrimary
    }

    val levelBg = when (parsed.level) {
        LogLevel.DEBUG -> FlareTheme.colors.textSecondary.copy(alpha = 0.15f)
        LogLevel.INFO -> Color(0xFF34C759).copy(alpha = 0.15f)
        LogLevel.WARN -> Color(0xFFFF9500).copy(alpha = 0.15f)
        LogLevel.ERROR -> Color(0xFFFF3B30).copy(alpha = 0.15f)
        LogLevel.DEFAULT -> Color.Transparent
    }

    val levelText = when (parsed.level) {
        LogLevel.DEBUG -> "DEBUG"
        LogLevel.INFO -> "INFO"
        LogLevel.WARN -> "WARN"
        LogLevel.ERROR -> "ERROR"
        LogLevel.DEFAULT -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .background(
                color = FlareTheme.colors.bgItem.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
            .drawWithContent {
                drawContent()
                if (parsed.level != LogLevel.DEFAULT) {
                    drawRect(
                        color = levelColor,
                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height),
                        topLeft = Offset.Zero
                    )
                }
            }
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (levelText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(levelBg, shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = levelText,
                        color = levelColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (parsed.timestamp.isNotEmpty()) {
                Text(
                    text = parsed.timestamp,
                    color = FlareTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = parsed.message,
            color = if (parsed.level == LogLevel.DEBUG) FlareTheme.colors.textSecondary else FlareTheme.colors.textPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
