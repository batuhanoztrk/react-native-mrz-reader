package com.mrzreader.types

import androidx.camera.view.PreviewView

enum class ResizeMode(val value: String) {
  COVER("cover"),
  CONTAIN("contain");

  fun toScaleType(): PreviewView.ScaleType =
    when (this) {
      COVER -> PreviewView.ScaleType.FILL_CENTER
      CONTAIN -> PreviewView.ScaleType.FIT_CENTER
    }

  companion object {
    fun fromValue(value: String): ResizeMode =
      when (value) {
        COVER.value -> COVER
        CONTAIN.value -> CONTAIN
        else -> throw IllegalArgumentException("Unknown ResizeMode value: $value")
      }
  }
}
