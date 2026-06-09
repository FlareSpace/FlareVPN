package flare.client.app.ui.components.dialogs

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import flare.client.app.R
import flare.client.app.data.db.AppDatabase
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.theme.FlareTheme
import flare.client.app.ui.notification.AppNotificationManager
import flare.client.app.ui.notification.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val GeologicaMedium = FontFamily(Font(R.font.geologica_medium, FontWeight.Medium))
private val GeologicaRegular = FontFamily(Font(R.font.geologica_regular, FontWeight.Normal))

enum class DataMgmtStep {
    SELECTION, EXPORTING, EXPORT_SUCCESS, IMPORTING, IMPORT_SUCCESS
}

fun DataMgmtStep.index(): Int = ordinal

fun restoreSharedPreferencesFromXml(context: Context, xmlFile: File) {
    android.util.Log.d("FlareVPN_Restore", "Starting SharedPreferences restore from ${xmlFile.absolutePath}")
    if (!xmlFile.exists()) {
        android.util.Log.e("FlareVPN_Restore", "SharedPreferences backup file does not exist!")
        return
    }
    val prefs = context.getSharedPreferences("flare_settings", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor.clear()

    try {
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        doc.documentElement.normalize()

        val rootNode = doc.documentElement
        val childNodes = rootNode.childNodes
        android.util.Log.d("FlareVPN_Restore", "Found ${childNodes.length} nodes in XML")

        var keyCount = 0
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                val element = node as org.w3c.dom.Element
                val type = element.tagName
                val key = element.getAttribute("name") ?: continue

                keyCount++
                when (type) {
                    "boolean" -> {
                        val value = element.getAttribute("value").toBoolean()
                        editor.putBoolean(key, value)
                    }
                    "string" -> {
                        val value = element.textContent ?: ""
                        editor.putString(key, value)
                    }
                    "int" -> {
                        val value = element.getAttribute("value").toIntOrNull() ?: 0
                        editor.putInt(key, value)
                    }
                    "long" -> {
                        val value = element.getAttribute("value").toLongOrNull() ?: 0L
                        editor.putLong(key, value)
                    }
                    "float" -> {
                        val value = element.getAttribute("value").toFloatOrNull() ?: 0f
                        editor.putFloat(key, value)
                    }
                    "set" -> {
                        val stringList = mutableListOf<String>()
                        val itemNodes = element.getElementsByTagName("string")
                        for (j in 0 until itemNodes.length) {
                            val itemNode = itemNodes.item(j) as? org.w3c.dom.Element
                            if (itemNode != null) {
                                stringList.add(itemNode.textContent ?: "")
                            }
                        }
                        editor.putStringSet(key, stringList.toSet())
                    }
                }
            }
        }
        val committed = editor.commit()
        android.util.Log.d("FlareVPN_Restore", "Committed $keyCount keys to SharedPreferences. Success: $committed")
    } catch (e: Exception) {
        android.util.Log.e("FlareVPN_Restore", "Error parsing SharedPreferences XML", e)
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun DataManagementDialog(
    onDismissRequest: () -> Unit,
    accentColor: Int,
    hazeState: HazeState? = null,
    onRestartRequired: () -> Unit
) {
    var currentStep by remember { mutableStateOf(DataMgmtStep.SELECTION) }
    var selectedAction by remember { mutableStateOf<String?>(null) } 
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        object : ActivityResultContracts.CreateDocument("application/zip") {
            override fun createIntent(context: Context, input: String): android.content.Intent {
                val intent = super.createIntent(context, input)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments"))
                }
                return intent
            }
        }
    ) { uri ->
        if (uri != null) {
            currentStep = DataMgmtStep.EXPORTING
            scope.launch {
                delay(1500)
                withContext(Dispatchers.IO) {
                    try {
                        val pfd = context.contentResolver.openFileDescriptor(uri, "w")
                        if (pfd != null) {
                            ZipOutputStream(java.io.FileOutputStream(pfd.fileDescriptor)).use { zos ->
                                val filesToZip = listOf(
                                    File(context.filesDir.parentFile, "shared_prefs/flare_settings.xml"),
                                    context.getDatabasePath("flare_client.db"),
                                    context.getDatabasePath("flare_client.db-shm"),
                                    context.getDatabasePath("flare_client.db-wal")
                                )
                                for (file in filesToZip) {
                                    if (file.exists()) {
                                        zos.putNextEntry(ZipEntry(file.name))
                                        file.inputStream().use { it.copyTo(zos) }
                                        zos.closeEntry()
                                    }
                                }
                            }
                            pfd.close()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                currentStep = DataMgmtStep.EXPORT_SUCCESS
            }
        } else {
            currentStep = DataMgmtStep.SELECTION
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        object : ActivityResultContracts.OpenDocument() {
            override fun createIntent(context: Context, input: Array<String>): android.content.Intent {
                val intent = super.createIntent(context, input)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments"))
                }
                return intent
            }
        }
    ) { uri ->
        if (uri != null) {
            currentStep = DataMgmtStep.IMPORTING
            scope.launch {
                delay(1500)
                withContext(Dispatchers.IO) {
                    try {
                        AppDatabase.closeInstance()
                        delay(200)

                        val prefsFile = File(context.filesDir.parentFile, "shared_prefs/flare_settings.xml")
                        val dbFile = context.getDatabasePath("flare_client.db")
                        val dbShm = context.getDatabasePath("flare_client.db-shm")
                        val dbWal = context.getDatabasePath("flare_client.db-wal")

                        prefsFile.delete()
                        dbFile.delete()
                        dbShm.delete()
                        dbWal.delete()

                        val tempDir = File(context.cacheDir, "restore_tmp")
                        tempDir.mkdirs()

                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        if (pfd != null) {
                            ZipInputStream(java.io.FileInputStream(pfd.fileDescriptor)).use { zis ->
                                var entry = zis.nextEntry
                                while (entry != null) {
                                    val outFile = File(tempDir, entry.name)
                                    outFile.outputStream().use { zis.copyTo(it) }
                                    zis.closeEntry()
                                    entry = zis.nextEntry
                                }
                            }
                            pfd.close()

                            val tempDb = File(tempDir, "flare_client.db")
                            if (tempDb.exists()) tempDb.copyTo(dbFile, overwrite = true)
                            val tempShm = File(tempDir, "flare_client.db-shm")
                            if (tempShm.exists()) tempShm.copyTo(dbShm, overwrite = true)
                            val tempWal = File(tempDir, "flare_client.db-wal")
                            if (tempWal.exists()) tempWal.copyTo(dbWal, overwrite = true)
                            
                            val tempPrefs = File(tempDir, "flare_settings.xml")
                            if (tempPrefs.exists()) restoreSharedPreferencesFromXml(context, tempPrefs)
                            
                            tempDir.deleteRecursively()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                currentStep = DataMgmtStep.IMPORT_SUCCESS
            }
        } else {
            currentStep = DataMgmtStep.SELECTION
        }
    }

    @Composable
    fun BouncingDots(color: Color) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            val dots = listOf(
                remember { Animatable(0f) },
                remember { Animatable(0f) },
                remember { Animatable(0f) }
            )

            dots.forEachIndexed { index, animatable ->
                LaunchedEffect(animatable) {
                    delay(index * 150L)
                    while (true) {
                        animatable.animateTo(
                            targetValue = 1f,
                            animationSpec = repeatable(
                                iterations = 1,
                                animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        animatable.animateTo(
                            targetValue = 0f,
                            animationSpec = repeatable(
                                iterations = 1,
                                animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        delay(300L)
                    }
                }
            }

            dots.forEach { animatable ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .size(10.dp)
                        .graphicsLayer {
                            translationY = -animatable.value * 12.dp.toPx()
                        }
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }

    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = (currentStep == DataMgmtStep.SELECTION),
            dismissOnClickOutside = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        SideEffect {
            dialogWindow?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0.60f)
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val params = window.attributes
                    params.blurBehindRadius = (15 * context.resources.displayMetrics.density).toInt()
                    window.attributes = params
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .widthIn(max = 380.dp)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .let {
                        if (hazeState != null) {
                            it.hazeEffect(
                                state = hazeState,
                                style = HazeMaterials.ultraThin(
                                    containerColor = FlareTheme.colors.dialogGlassFill
                                )
                            )
                        } else {
                            it.background(FlareTheme.colors.dialogGlassFill)
                        }
                    }
                    .border(
                        width = 0.5.dp,
                        color = FlareTheme.colors.dialogGlassStroke,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isLandscape) Modifier
                                .heightIn(max = 240.dp)
                                .verticalScroll(scrollState)
                            else Modifier
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = I18n.strings.data_mgmt_title,
                            color = FlareTheme.colors.textPrimary,
                            fontSize = 20.sp,
                            fontFamily = GeologicaMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (currentStep == DataMgmtStep.SELECTION) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true),
                                        onClick = onDismissRequest
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = null,
                                    tint = FlareTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState.index() > initialState.index()) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "data_mgmt_transition"
                    ) { step ->
                        when (step) {
                            DataMgmtStep.SELECTION -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = I18n.strings.settings_desc_data_mgmt,
                                        color = FlareTheme.colors.textSecondary,
                                        fontSize = 14.sp,
                                        fontFamily = GeologicaRegular,
                                        modifier = Modifier.padding(bottom = 20.dp)
                                    )

                                    val isImportSelected = selectedAction == "import"
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isImportSelected) Color(accentColor).copy(alpha = 0.08f)
                                                else FlareTheme.colors.bgItem.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isImportSelected) Color(accentColor)
                                                else FlareTheme.colors.glassStroke.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { selectedAction = "import" }
                                            .padding(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(accentColor).copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_arrow_left),
                                                    contentDescription = null,
                                                    tint = Color(accentColor),
                                                    modifier = Modifier.size(20.dp).graphicsLayer(rotationZ = 90f)
                                                )
                                            }
                                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                                Text(
                                                    text = I18n.strings.data_mgmt_import,
                                                    color = FlareTheme.colors.textPrimary,
                                                    fontFamily = GeologicaMedium,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = I18n.strings.data_mgmt_import_desc,
                                                    color = FlareTheme.colors.textSecondary,
                                                    fontFamily = GeologicaRegular,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val isExportSelected = selectedAction == "export"
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isExportSelected) Color(accentColor).copy(alpha = 0.08f)
                                                else FlareTheme.colors.bgItem.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isExportSelected) Color(accentColor)
                                                else FlareTheme.colors.glassStroke.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { selectedAction = "export" }
                                            .padding(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(accentColor).copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_copy),
                                                    contentDescription = null,
                                                    tint = Color(accentColor),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                                Text(
                                                    text = I18n.strings.data_mgmt_export,
                                                    color = FlareTheme.colors.textPrimary,
                                                    fontFamily = GeologicaMedium,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = I18n.strings.data_mgmt_export_desc,
                                                    color = FlareTheme.colors.textSecondary,
                                                    fontFamily = GeologicaRegular,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .background(
                                                color = if (selectedAction != null) Color(accentColor)
                                                else FlareTheme.colors.textSecondary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .then(
                                                if (selectedAction != null) {
                                                    Modifier.clickable {
                                                        if (selectedAction == "export") {
                                                            exportLauncher.launch("flare_backup.zip")
                                                        } else {
                                                            importLauncher.launch(arrayOf("application/zip"))
                                                        }
                                                    }
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = I18n.strings.btn_next,
                                            color = if (selectedAction != null) Color.White
                                            else FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
                                            fontSize = 15.sp,
                                            fontFamily = GeologicaMedium
                                        )
                                    }
                                }
                            }

                            DataMgmtStep.EXPORTING -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = I18n.strings.data_mgmt_creating,
                                        color = FlareTheme.colors.textPrimary,
                                        fontFamily = GeologicaMedium,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    BouncingDots(color = Color(accentColor))
                                }
                            }

                            DataMgmtStep.EXPORT_SUCCESS -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                            .border(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = I18n.strings.data_mgmt_created,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 18.sp,
                                        fontFamily = GeologicaMedium,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .background(Color(accentColor), RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onDismissRequest() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = I18n.strings.btn_done,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontFamily = GeologicaMedium
                                        )
                                    }
                                }
                            }

                            DataMgmtStep.IMPORTING -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = I18n.strings.data_mgmt_restoring,
                                        color = FlareTheme.colors.textPrimary,
                                        fontFamily = GeologicaMedium,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    BouncingDots(color = Color(accentColor))
                                }
                            }

                            DataMgmtStep.IMPORT_SUCCESS -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                            .border(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = I18n.strings.data_mgmt_restored,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 18.sp,
                                        fontFamily = GeologicaMedium,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .background(Color(accentColor), RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { restartApp() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = I18n.strings.btn_done,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontFamily = GeologicaMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
