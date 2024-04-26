package com.mrzreader

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
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
}
