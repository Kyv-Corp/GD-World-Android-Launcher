package com.movtery.zalithlauncher.gdworld

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.download.jvm_server.JvmCrashException
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.AppLanguage
import com.movtery.zalithlauncher.setting.enums.applyLanguage
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.UUID

@Composable
fun GdHome(events: EventViewModel, advanced: () -> Unit) {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    var operation by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf<GdProgress?>(null) }
    var status by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    val busy = operation != null || tasks.isNotEmpty()
    LaunchedEffect(busy) { events.sendKeepScreen(busy) }
    DisposableEffect(Unit) { onDispose { events.sendKeepScreen(false) } }
    val signedIn = account?.accountType == "GDWorld"
    val gold = Color(0xffffd300)
    val genericError = stringResource(R.string.gd_error)
    val installerError = stringResource(R.string.gd_install_error)
    val codeText = stringResource(R.string.gd_code_required)
    val launchingText = stringResource(R.string.gd_launching)
    fun run(block: suspend () -> Unit) {
        if (busy) return
        focus.clearFocus()
        operation = scope.launch {
            failed = false
            status = ""
            progress = null
            try { block() }
            catch (e: TimeoutCancellationException) { failed = true; status = installerError; Logger.warning("GDWorld", "Installer timed out", e) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                failed = true
                status = when {
                    e is JvmCrashException -> installerError
                    e.message in listOf("auth.require2fa", "auth.wrong2fa") -> codeText
                    else -> genericError
                }
                Logger.warning("GDWorld", "Operation failed", e)
            }
            finally { operation = null; progress = null }
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = gold, secondary = gold, background = Color.Black, surface = Color(0xff272729))) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.gd_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).safeDrawingPadding().imePadding()) {
                val compact = maxWidth < 600.dp
                val landscape = !compact && LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val header: @Composable () -> Unit = {
                    Row(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(painterResource(R.drawable.gd_logo), null, Modifier.size(if (compact || landscape) 48.dp else 64.dp), contentScale = ContentScale.Fit)
                        Column(Modifier.weight(1f)) {
                            Text("GDWorld", style = if (landscape) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge, color = gold)
                            Text(stringResource(R.string.gd_subtitle), color = Color.White)
                        }
                    }
                }
                val sessionControls: @Composable () -> Unit = {
                    Text(account!!.username, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!landscape) Text(stringResource(R.string.gd_pack))
                    Button(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black), onClick = { run {
                        val (name, address) = GdPack.prepare(context) { progress = it }
                        val version = withTimeout(15000) { VersionsManager.versions.first { versions -> versions.any { it.getVersionName() == name } }.first { it.getVersionName() == name } }
                        status = launchingText
                        events.sendEvent(EventViewModel.Event.Launch.PlayServer(version, address))
                    } }) { Text(stringResource(R.string.gd_play)) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(enabled = !busy, onClick = { uri.openUri("https://gd-network.ddns.net/cabinet") }) { Text(stringResource(R.string.gd_cabinet)) }
                        TextButton(enabled = !busy, onClick = { run {
                            withContext(Dispatchers.IO) { GdSession.clear() }
                            account?.let { AccountsManager.deleteAccount(it) }
                        } }) { Text(stringResource(R.string.gd_logout)) }
                    }
                }
                val accountCard: @Composable () -> Unit = {
                    Card(Modifier.widthIn(max = if (landscape) 640.dp else 480.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xff272729), contentColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(if (compact || landscape) 16.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!signedIn) {
                                OutlinedTextField(login, { login = it }, enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), label = { Text(stringResource(R.string.gd_login)) }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(password, { password = it }, enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next), visualTransformation = PasswordVisualTransformation(), label = { Text(stringResource(R.string.gd_password)) }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(totp, { totp = it }, enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }), label = { Text(stringResource(R.string.gd_totp)) }, modifier = Modifier.fillMaxWidth())
                                Button(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy && login.isNotBlank() && password.isNotEmpty(), onClick = { run {
                                    val session = runInterruptible(Dispatchers.IO) { GravitClient().use { it.login(login.trim(), password, totp.trim()) } }
                                    val profile = session.getJSONObject("playerProfile")
                                    withContext(Dispatchers.IO) { GdSession.write(session) }
                                    val gd = Account(uniqueUUID = "gdworld", username = profile.getString("username"), profileId = profile.getString("uuid"), accountType = "GDWorld", accessToken = UUID.randomUUID().toString(), expiresAt = Long.MAX_VALUE)
                                    AccountsManager.suspendSaveAccount(gd)
                                    password = ""; totp = ""
                                } }) { Text(stringResource(R.string.gd_sign_in)) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextButton(onClick = { uri.openUri("https://gd-network.ddns.net/auth/register") }) { Text(stringResource(R.string.gd_register)) }
                                    TextButton(onClick = { uri.openUri("https://gd-network.ddns.net/auth/reset") }) { Text(stringResource(R.string.gd_reset)) }
                                }
                            } else if (!landscape) {
                                sessionControls()
                            }
                            if (landscape && signedIn) Text(stringResource(R.string.gd_pack))
                            if (operation != null) {
                                val current = progress
                                if (current != null) {
                                    Text(stringResource(current.textRes), color = gold)
                                    GdProgressBar(current.fraction)
                                    if (current.completedBytes != null && current.totalBytes != null) {
                                        Text(stringResource(R.string.gd_bytes, formatFileSize(current.completedBytes), formatFileSize(current.totalBytes)))
                                    }
                                    if (current.detail.isNotEmpty()) Text(current.detail, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                } else LinearProgressIndicator(Modifier.fillMaxWidth(), color = gold)
                            }
                            tasks.forEach { task ->
                                key(task.id) {
                                    val fraction by task.progress.collectAsStateWithLifecycle()
                                    val message by task.message.collectAsStateWithLifecycle()
                                    message?.let { AndroidStringText(it) }
                                    GdProgressBar(fraction.takeIf { it >= 0f })
                                    TextButton(onClick = { TaskSystem.cancelTask(task) }) { Text(stringResource(R.string.gd_cancel)) }
                                }
                            }
                            if (status.isNotEmpty()) Text(status, color = if (failed) Color(0xffffaaaa) else gold)
                            if (operation != null) TextButton(onClick = { operation?.cancel() }) { Text(stringResource(R.string.gd_cancel)) }
                        }
                    }
                }
                val footer: @Composable () -> Unit = {
                    FlowRow(Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !busy, onClick = { settings = true }) { Text(stringResource(R.string.gd_settings)) }
                        TextButton(enabled = !busy, onClick = advanced) { Text(stringResource(R.string.gd_advanced)) }
                    }
                }
                if (landscape) {
                    Row(Modifier.widthIn(max = 1100.dp).fillMaxSize().align(Alignment.TopCenter).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            header()
                            if (signedIn) sessionControls()
                            footer()
                        }
                        Column(Modifier.weight(1.8f).fillMaxHeight().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                            accountCard()
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (compact) 16.dp else 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        header()
                        accountCard()
                        footer()
                    }
                }
            }
        }
        if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text(stringResource(R.string.gd_settings)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                listOf(AppLanguage.RUSSIAN, AppLanguage.ENGLISH, AppLanguage.UKRAINIAN, AppLanguage.BELARUSIAN, AppLanguage.POLISH).forEach { language ->
                    TextButton(onClick = { AllSettings.launcherLanguage.save(language); applyLanguage(language) }) { Text(stringResource(language.textRes)) }
                }
                Text(stringResource(R.string.gd_settings_hint))
            }
        }, confirmButton = { TextButton(onClick = { settings = false }) { Text(stringResource(R.string.gd_close)) } })
    }
}

@Composable
private fun GdProgressBar(fraction: Float?) {
    if (fraction != null && fraction.isFinite()) {
        val value = fraction.coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.gd_percent, (value * 100).toInt()))
    } else LinearProgressIndicator(Modifier.fillMaxWidth())
}
