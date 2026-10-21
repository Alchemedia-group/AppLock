package com.alchemedia.applock.ui.main

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
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alchemedia.applock.AppLockApp
import com.alchemedia.applock.R
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
                    val lockUi by lockViewModel.ui.collectAsState()
                    val current = settings
                    val gateShown = current != null && current.setupComplete && !mainViewModel.authenticated
                    val captureEnabled = current?.captureIntruders == true
                    // Only hold the camera while the entry screen is up.
                    LaunchedEffect(gateShown, captureEnabled) {
                        if (gateShown && captureEnabled) camera.start() else camera.stop()
                    }
                    when {
                        current == null -> Unit
                        !current.setupComplete -> CredentialSetup(
                            onDone = { type, secret -> mainViewModel.setCredential(type, secret) },
                        )
                        !mainViewModel.authenticated -> LockScreen(
                            settings = current,
                            state = lockUi,
                            title = stringResource(R.string.unlock_applock),
                            icon = null,
                            onSecret = { secret ->
                                lockViewModel.submit(secret) { mainViewModel.authenticated = true }
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

    override fun onStop() {
        super.onStop()
        // Require the PIN/pattern again next time, but not across rotation or other config changes.
        if (!isChangingConfigurations) mainViewModel.authenticated = false
    }
}
