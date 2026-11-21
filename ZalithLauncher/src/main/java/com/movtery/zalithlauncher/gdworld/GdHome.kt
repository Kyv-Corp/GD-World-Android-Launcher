package com.movtery.zalithlauncher.gdworld

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
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
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    var operation by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    val busy = operation != null
    val signedIn = account?.accountType == "GDWorld"
    val gold = Color(0xffffd300)
    val genericError = stringResource(R.string.gd_error)
    val installText = stringResource(R.string.gd_install)
    val updateText = stringResource(R.string.gd_update)
    val codeText = stringResource(R.string.gd_code_required)
    val launchingText = stringResource(R.string.gd_launching)
    fun run(block: suspend () -> Unit) {
        operation = scope.launch {
            failed = false
            status = ""
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                failed = true
                status = if (e.message in listOf("auth.require2fa", "auth.wrong2fa")) codeText else genericError
            }
            finally { operation = null }
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = gold, secondary = gold, background = Color.Black, surface = Color(0xff272729))) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.gd_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.gd_logo), "GDWorld", Modifier.height(64.dp).fillMaxWidth(), contentScale = ContentScale.Fit)
            Text("GDWorld", style = MaterialTheme.typography.headlineLarge, color = gold)
            Text(stringResource(R.string.gd_subtitle), color = Color.White)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xff272729), contentColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!signedIn) {
                        OutlinedTextField(login, { login = it }, enabled = !busy, singleLine = true, label = { Text(stringResource(R.string.gd_login)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, enabled = !busy, singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text(stringResource(R.string.gd_password)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(totp, { totp = it }, enabled = !busy, singleLine = true, label = { Text(stringResource(R.string.gd_totp)) }, modifier = Modifier.fillMaxWidth())
                        Button(enabled = !busy && login.isNotBlank() && password.isNotEmpty(), onClick = { run {
                            val session = withContext(Dispatchers.IO) { GravitClient().use { it.login(login.trim(), password, totp.trim()) } }
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
                    } else {
                        Text(account!!.username, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.gd_pack))
                        Button(modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black), onClick = { run {
                            val (name, address) = GdPack.prepare(context) { status = when (it) { "install" -> installText; "update" -> updateText; else -> "$updateText $it" } }
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
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = gold)
                    if (status.isNotEmpty()) Text(status, color = if (failed) Color(0xffffaaaa) else gold)
                    if (busy) TextButton(onClick = { operation?.cancel() }) { Text(stringResource(R.string.gd_cancel)) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(enabled = !busy, onClick = { settings = true }) { Text(stringResource(R.string.gd_settings)) }
                TextButton(enabled = !busy, onClick = advanced) { Text(stringResource(R.string.gd_advanced)) }
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
