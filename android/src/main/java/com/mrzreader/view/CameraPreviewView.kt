package com.mrzreader.view

import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.mrzreader.analyzer.MrzReaderAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

class CameraPreviewView @JvmOverloads constructor(
  private val reactContext: ReactContext, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextureView(reactContext, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

  companion object {
    private const val RATIO_4_3_VALUE = 4.0 / 3.0
    private const val RATIO_16_9_VALUE = 16.0 / 9.0
  }

  private val mrzReaderAnalyzer = MrzReaderAnalyzer(reactContext)
  private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private lateinit var surfaceProvider: Preview.SurfaceProvider
  private var cameraProvider: ProcessCameraProvider? = null

  private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
  private var isCameraStarted = false

  private var camera: Camera? = null

  private var lastTouchX: Float = 0f
  private var lastTouchY: Float = 0f

  init {
    surfaceTextureListener = this

    mrzReaderAnalyzer.mrzInfo.observeForever {
      val mrz = it.toString().replace(Regex("\\s+"), "")

      CoroutineScope(Dispatchers.Main).launch {
        sendMessageToReactNative(mrz)
      }
    }

    setOnTouchListener { _, event ->
      if (event.action == android.view.MotionEvent.ACTION_DOWN) {
        lastTouchX = event.x
        lastTouchY = event.y
        performClick()

        true
      } else false
    }
  }

  fun setDocType(docType: String) {
    mrzReaderAnalyzer.setDocType(docType)
  }

  fun setCameraSelector(cameraType: String) {
    this.cameraSelector = when (cameraType) {
      "front" -> CameraSelector.DEFAULT_FRONT_CAMERA
      else -> CameraSelector.DEFAULT_BACK_CAMERA
    }

    if (isCameraStarted) {
      startCamera()
    }
  }

  private fun focusOnPoint(x: Float, y: Float): Boolean {
    camera?.let {
      val factory = DisplayOrientedMeteringPointFactory(
        display,
        it.cameraInfo,
        this.width / 2f,
        this.height / 2f
      )

      val point = factory.createPoint(x, y)

      val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
        .setAutoCancelDuration(5, TimeUnit.SECONDS).build()

      it.cameraControl.startFocusAndMetering(action)

      return true
    } ?: run {
      return false
    }
  }

  private fun sendMessageToReactNative(message: String) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onMRZRead", message)
  }

  private fun aspectRatio(width: Int, height: Int): Int {
    val previewRatio =
      width.toDouble().coerceAtLeast(height.toDouble()) / min(width.toDouble(), height.toDouble())
    if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
      return AspectRatio.RATIO_4_3
    }
    return AspectRatio.RATIO_16_9
  }

  override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
    val surface = Surface(surfaceTexture)
    surfaceProvider = Preview.SurfaceProvider { request: SurfaceRequest ->
      val resolution = request.resolution
      surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
      request.provideSurface(surface, cameraExecutor) {
        // Handle surface completion
      }
    }
    startCamera()
  }

  override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
  }

  override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
    return true
  }

  override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
  }

  override fun performClick(): Boolean {
    super.performClick()

    return focusOnPoint(lastTouchX, lastTouchY)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    cameraProvider?.unbindAll()
    cameraExecutor.shutdown()
  }

  private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(reactContext)

    val resolutionSelector =
      ResolutionSelector.Builder().setAspectRatioStrategy(
        AspectRatioStrategy(
          aspectRatio(width, height),
          AspectRatioStrategy.FALLBACK_RULE_NONE
        )
      ).build()

    cameraProviderFuture.addListener({
      cameraProvider = cameraProviderFuture.get()

      val preview = Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()
        .also {
          it.setSurfaceProvider(this.surfaceProvider)
        }

      val imageAnalysis = ImageAnalysis.Builder()
        .setTargetRotation(this.display!!.rotation)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(resolutionSelector)
        .build()
        .also {
          it.setAnalyzer(cameraExecutor, mrzReaderAnalyzer)
        }

      try {
        cameraProvider!!.unbindAll()
        camera = cameraProvider!!.bindToLifecycle(
          reactContext.currentActivity!! as LifecycleOwner,
          cameraSelector,
          preview,
          imageAnalysis
        )
        isCameraStarted = true
      } catch (exc: Exception) {
        exc.printStackTrace()
      }
    }, ContextCompat.getMainExecutor(reactContext))
  }
}
