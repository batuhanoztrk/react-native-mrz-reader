package com.mrzreader

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.mrzreader.types.PreviewViewType
import com.mrzreader.types.ResizeMode
import com.mrzreader.view.CameraPreviewView

class MrzReaderViewManager : SimpleViewManager<CameraPreviewView>() {
  override fun getName() = "MrzReaderView"

  override fun createViewInstance(reactContext: ThemedReactContext): CameraPreviewView {
    return CameraPreviewView(reactContext)
  }

  @ReactProp(name = "cameraSelector")
  fun setCameraSelector(view: CameraPreviewView, cameraType: String) {
    view.setCameraSelector(cameraType)
  }

  @ReactProp(name = "docType")
  fun setDocType(view: CameraPreviewView, cardType: String) {
    view.setDocType(cardType)
  }

  @ReactProp(name = "resizeMode")
  fun setResizeMode(view: CameraPreviewView, resizeMode: String?) {
    if (resizeMode != null) {
      val newMode = ResizeMode.fromValue(resizeMode)
      view.resizeMode = newMode
    } else {
      view.resizeMode = ResizeMode.COVER
    }
  }

  @ReactProp(name = "androidPreviewViewType")
  fun setAndroidPreviewViewType(view: CameraPreviewView, previewViewType: String?) {
    if (previewViewType != null) {
      val newType = PreviewViewType.fromValue(previewViewType)
      view.androidPreviewViewType = newType
    } else {
      view.androidPreviewViewType = PreviewViewType.SURFACE_VIEW
    }
  }
}
