package com.alchemedia.applock.intruder

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.alchemedia.applock.security.hasCameraPermission

/**
 * Front-camera capture without a preview. The camera is bound while the host activity is visible
 * (so exposure has settled by the time a wrong attempt happens) and released automatically with
 * the activity lifecycle.
 */
class IntruderCamera(
    private val activity: FragmentActivity,
    private val repository: IntruderRepository,
) {
    private var imageCapture: ImageCapture? = null

    fun start() {
        if (imageCapture != null || !hasCameraPermission(activity)) return
        val providerFuture = ProcessCameraProvider.getInstance(activity)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(activity, CameraSelector.DEFAULT_FRONT_CAMERA, capture)
                imageCapture = capture
            } catch (e: Exception) {
                // No front camera, camera in use, or permission revoked: the lock still works without a photo.
                Log.w(TAG, "Intruder camera unavailable", e)
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    /** Releases the camera; used when the host stays open but no longer needs to watch for intruders. */
    fun stop() {
        if (imageCapture == null) return
        imageCapture = null
        val providerFuture = ProcessCameraProvider.getInstance(activity)
        providerFuture.addListener({ providerFuture.get().unbindAll() }, ContextCompat.getMainExecutor(activity))
    }

    fun capture(targetPackage: String?) {
        val capture = imageCapture ?: return
        val file = repository.newFile(targetPackage)
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    repository.refresh()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "Intruder capture failed", exception)
                    file.delete()
                }
            },
        )
    }

    private companion object {
        const val TAG = "IntruderCamera"
    }
}
