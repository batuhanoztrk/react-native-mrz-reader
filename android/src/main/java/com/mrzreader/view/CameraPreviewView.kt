package com.mrzreader.view

import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.mrzreader.analyzer.MrzReaderAnalyzer
import com.mrzreader.extensions.installHierarchyFitter
import com.mrzreader.types.PreviewViewType
import com.mrzreader.types.ResizeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.min

class CameraPreviewView @JvmOverloads constructor(
  private val reactContext: ReactContext, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(reactContext, attrs, defStyleAttr) {

  companion object {
    private const val RATIO_4_3_VALUE = 4.0 / 3.0
    private const val RATIO_16_9_VALUE = 16.0 / 9.0
  }

  private var previewView: PreviewView? = null
  private val mainCoroutineScope = CoroutineScope(Dispatchers.Main)

  private val mrzReaderAnalyzer = MrzReaderAnalyzer(reactContext)
  private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private lateinit var surfaceProvider: Preview.SurfaceProvider
  private var cameraProvider: ProcessCameraProvider? = null

  private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
  private var isCameraStarted = false

  private var camera: Camera? = null

  var androidPreviewViewType: PreviewViewType = PreviewViewType.SURFACE_VIEW
    set(value) {
      field = value
      updatePreview()
    }
  var resizeMode: ResizeMode = ResizeMode.COVER
    set(value) {
      field = value
      updatePreview()
    }

  init {
    this.installHierarchyFitter()
    updatePreview()

    mrzReaderAnalyzer.mrzInfo.observeForever {
      val mrz = it.toString().replace(Regex("\\s+"), "")

      CoroutineScope(Dispatchers.Main).launch {
        sendMessageToReactNative(mrz)
      }
    }
  }

  private fun updatePreview() {
    mainCoroutineScope.launch {
      if (previewView == null) {
        previewView = createPreviewView()
        addView(previewView)
      }
      previewView?.let {
        // Update implementation type from React
        it.implementationMode = androidPreviewViewType.toPreviewImplementationMode()
        // Update scale type from React
        it.scaleType = resizeMode.toScaleType()
      }
      startCamera()
    }
  }

  private fun createPreviewView(): PreviewView =
    PreviewView(context).also {
      it.installHierarchyFitter()
      it.layoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT,
        Gravity.CENTER
      )
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
          AspectRatioStrategy.FALLBACK_RULE_AUTO
        )
      ).build()

    cameraProviderFuture.addListener({
      cameraProvider = cameraProviderFuture.get()

      val preview = Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()
        .also {
          it.setSurfaceProvider(this.previewView?.surfaceProvider)
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
