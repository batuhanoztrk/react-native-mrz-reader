package com.mrzreader.types

import androidx.camera.view.PreviewView

enum class PreviewViewType(val value: String) {
  SURFACE_VIEW("surface-view"),
  TEXTURE_VIEW("texture-view");

  fun toPreviewImplementationMode(): PreviewView.ImplementationMode =
    when (this) {
      SURFACE_VIEW -> PreviewView.ImplementationMode.PERFORMANCE
      TEXTURE_VIEW -> PreviewView.ImplementationMode.COMPATIBLE
    }

  companion object {
    fun fromValue(value: String): PreviewViewType =
      when (value) {
        SURFACE_VIEW.value -> SURFACE_VIEW
        TEXTURE_VIEW.value -> TEXTURE_VIEW
        else -> throw IllegalArgumentException("Unknown PreviewViewType value: $value")
      }
  }
}
