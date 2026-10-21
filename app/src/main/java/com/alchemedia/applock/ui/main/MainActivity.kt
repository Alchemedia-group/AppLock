package com.alchemedia.applock.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alchemedia.applock.AppLockApp
import com.alchemedia.applock.R
import com.alchemedia.applock.i18n.LanguageManager
import com.alchemedia.applock.intruder.IntruderCamera
import com.alchemedia.applock.ui.lock.LockScreen
import com.alchemedia.applock.ui.lock.LockViewModel
import com.alchemedia.applock.ui.theme.AppBackdrop
import com.alchemedia.applock.ui.theme.AppLockTheme
import com.alchemedia.applock.ui.theme.ImmersiveMode
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val lockViewModel: LockViewModel by viewModels()
    private lateinit var camera: IntruderCamera

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keeps the app's contents (installed-app list, intruder photos) out of screenshots and recents.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        camera = IntruderCamera(this, (application as AppLockApp).intruderRepository)
        // Wrong attempts at AppLock's own entry screen are photographed too (no target app).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                lockViewModel.captureRequests.collect { camera.capture(null) }
            }
        }

        setContent {
            AppLockTheme {
                AppBackdrop {
                    val settings by mainViewModel.settings.collectAsState()
                    var languageChosen by remember { mutableStateOf(LanguageManager.isChosen(this@MainActivity)) }
                    var recovering by rememberSaveable { mutableStateOf(false) }
                    val lockUi by lockViewModel.ui.collectAsState()
                    val current = settings
                    val gateShown = current != null && current.setupComplete && !mainViewModel.authenticated
                    val captureEnabled = current?.captureIntruders == true && current.proUnlocked
                    // Only hold the camera while the entry screen is up.
                    LaunchedEffect(gateShown, captureEnabled) {
                        if (gateShown && captureEnabled) camera.start() else camera.stop()
                    }
                    when {
                        !languageChosen -> LanguageChooserScreen(onPick = { tag ->
                            LanguageManager.save(this@MainActivity, tag)
                            languageChosen = true
                            recreate()
                        })
                        current == null -> Unit
                        mainViewModel.accessibilityDisclosurePending -> AccessibilityDisclosureScreen(
                            onAgree = {
                                mainViewModel.agreeToAccessibilityDisclosure()
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onDecline = { mainViewModel.accessibilityDisclosurePending = false },
                        )
                        !current.setupComplete -> OnboardingFlow(mainViewModel)
                        !mainViewModel.authenticated && recovering -> RecoveryFlow(
                            viewModel = mainViewModel,
                            onWrongCode = lockViewModel::registerFailure,
                            onClose = { recovering = false },
                        )
                        !mainViewModel.authenticated -> LockScreen(
                            settings = current,
                            state = lockUi,
                            title = stringResource(R.string.unlock_applock),
                            icon = null,
                            onSecret = { secret ->
                                lockViewModel.submit(secret) { mainViewModel.authenticated = true }
                            },
                            onForgot = if (current.recoveryHash != null) {
                                { recovering = true }
                            } else {
                                null
                            },
                        )
                        else -> HomeScreen(current, mainViewModel)
                    }
                }
            }
        }
        ImmersiveMode.apply(window)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ImmersiveMode.apply(window)
    }

    override fun onStart() {
        super.onStart()
        // Re-checks the Pro purchase with Google Play each time the app opens.
        mainViewModel.startBilling()
    }

    override fun onStop() {
        super.onStop()
        // Require the PIN/pattern again next time, but not across rotation or other config changes.
        if (!isChangingConfigurations) mainViewModel.authenticated = false
    }
}

/** Forgot PIN or pattern: verify the recovery code, pick a new secret, then save a fresh recovery code. */
@androidx.compose.runtime.Composable
private fun RecoveryFlow(
    viewModel: MainViewModel,
    onWrongCode: () -> Unit,
    onClose: () -> Unit,
) {
    var verified by rememberSaveable { mutableStateOf(false) }
    var newCode by rememberSaveable { mutableStateOf<String?>(null) }

    val code = newCode
    when {
        code != null -> RecoveryCodeScreen(
            code = code,
            onSaved = {
                viewModel.saveRecoveryCode(code)
                viewModel.authenticated = true
                onClose()
            },
        )
        verified -> CredentialSetup(
            onDone = { type, secret ->
                viewModel.setCredential(type, secret, authenticate = false)
                newCode = com.alchemedia.applock.security.RecoveryCode.generate()
            },
        )
        else -> RecoverySignIn(
            onVerify = { entered, onResult ->
                viewModel.verifyRecovery(entered) { ok ->
                    if (ok) verified = true else onWrongCode()
                    onResult(ok)
                }
            },
            onBack = onClose,
        )
    }
}
