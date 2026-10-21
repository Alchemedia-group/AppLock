package com.alchemedia.applock.ui.lock

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import com.alchemedia.applock.AppLockApp
import com.alchemedia.applock.R
import com.alchemedia.applock.intruder.IntruderCamera
import com.alchemedia.applock.security.UnlockSession
import com.alchemedia.applock.security.isBiometricAvailable
import com.alchemedia.applock.ui.theme.AppBackdrop
import com.alchemedia.applock.ui.theme.AppLockTheme
import com.alchemedia.applock.ui.theme.ImmersiveMode
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Covers a protected app until the user proves who they are; photographs wrong attempts. */
class LockActivity : FragmentActivity() {

    private val viewModel: LockViewModel by viewModels()
    private lateinit var camera: IntruderCamera
    private var targetPackage by mutableStateOf<String?>(null)
    private var revealed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep the lock screen out of screenshots and the recents thumbnail.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE)

        camera = IntruderCamera(this, (application as AppLockApp).intruderRepository)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.captureRequests.collect { camera.capture(targetPackage) }
            }
        }
        lifecycleScope.launch {
            val settings = viewModel.settings.filterNotNull().first()
            if (settings.captureIntruders && settings.proUnlocked) camera.start()
            // With the fake crash screen up, a fingerprint prompt would give the game away.
            if (settings.biometricEnabled && !settings.fakeCrash && isBiometricAvailable(this@LockActivity)) {
                lifecycle.withResumed { showBiometricPrompt() }
            }
        }

        setContent {
            AppLockTheme {
                val settings by viewModel.settings.collectAsState()
                val ui by viewModel.ui.collectAsState()
                val target = targetPackage
                val appInfo = remember(target) { target?.let(::loadTargetApp) }
                val current = settings
                AppBackdrop {
                    if (current != null && current.fakeCrash && !revealed && target != null) {
                        FakeCrashScreen(
                            appLabel = appInfo?.first ?: target,
                            onClose = ::goHome,
                            onReveal = { revealed = true },
                        )
                    } else if (current != null) {
                        LockScreen(
                            settings = current,
                            state = ui,
                            title = appInfo?.first?.let { stringResource(R.string.unlock_app, it) }
                                ?: stringResource(R.string.unlock_applock),
                            icon = appInfo?.second,
                            onSecret = { secret -> viewModel.submit(secret, ::unlock) },
                            onBiometric = if (current.biometricEnabled && isBiometricAvailable(this@LockActivity)) {
                                ::showBiometricPrompt
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
        ImmersiveMode.apply(window)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE)
        revealed = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ImmersiveMode.apply(window)
    }

    private fun unlock() {
        targetPackage?.let(UnlockSession::markUnlocked)
        finish()
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    private fun showBiometricPrompt() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.onSuccessfulBiometric()
                    unlock()
                }

                override fun onAuthenticationFailed() = viewModel.registerFailure()
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setNegativeButtonText(getString(R.string.biometric_use_secret))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    /** Returns the app's label and icon, or null if it can't be resolved. */
    private fun loadTargetApp(packageName: String): Pair<String, ImageBitmap>? = try {
        val pm = packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString() to pm.getApplicationIcon(info).toBitmap(128, 128).asImageBitmap()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    companion object {
        const val EXTRA_PACKAGE = "com.alchemedia.applock.extra.PACKAGE"
    }
}
