package com.classicscan

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classicscan.data.SettingsRepository
import com.classicscan.ui.AppNav
import com.classicscan.ui.LockScreen
import com.classicscan.ui.ProvideWindowLayout
import com.classicscan.ui.theme.ScanTheme
import com.classicscan.ui.theme.isDark
import org.koin.android.ext.android.inject

const val ACTION_SCAN = "com.classicscan.action.SCAN"
const val ACTION_IMPORT = "com.classicscan.action.IMPORT"

private const val LOCK_AFTER_MS = 30_000L

class MainActivity : AppCompatActivity() {
    private val settingsRepo: SettingsRepository by inject()
    private var pendingAction by mutableStateOf<String?>(null)
    private var unlocked by mutableStateOf(false)
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        unlocked = savedInstanceState?.getBoolean("unlocked") ?: false
        if (savedInstanceState == null) pendingAction = intent?.action
        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = null)
            val s = settings ?: return@setContent
            val dark = isDark(s.theme)
            LaunchedEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, Color.Black.toArgb()),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, Color.Black.toArgb()),
                )
            }

            LaunchedEffect(s.appLock) {
                if (s.appLock) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            ScanTheme(s.theme, s.dynamicColor, s.amoled, s.accent) {
                Surface(color = MaterialTheme.colorScheme.background) { ProvideWindowLayout(this@MainActivity) {
                    if (s.appLock && !unlocked) {
                        LockScreen(onUnlock = ::authenticate)
                        LaunchedEffect(Unit) { authenticate() }
                    } else {
                        AppNav(s, pendingAction) { pendingAction = null }
                    }
                } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingAction = intent.action
    }

    override fun onStart() {
        super.onStart()
        if (stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > LOCK_AFTER_MS) unlocked = false
    }

    override fun onStop() {
        super.onStop()

        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("unlocked", unlocked)
    }

    private fun authenticate() {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlocked = true
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_title))
                .setSubtitle(getString(R.string.unlock_subtitle))
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build()
        )
    }
}
