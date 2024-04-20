package com.mrzreader

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.mrzreader.view.CameraPreviewView

class MrzReaderViewManager : SimpleViewManager<CameraPreviewView>() {
  override fun getName() = "MrzReaderView"

  override fun createViewInstance(reactContext: ThemedReactContext): CameraPreviewView {
    return CameraPreviewView(reactContext)
  }
}
