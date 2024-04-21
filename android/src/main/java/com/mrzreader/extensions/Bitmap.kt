package com.mrzreader.extensions

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.min

fun Bitmap.cropToCenterSquare(): Bitmap {
  val newSize = min(this.width, this.height)

  val xStart = (this.width - newSize) / 2
  val yStart = (this.height - newSize) / 2

  return Bitmap.createBitmap(this, xStart, yStart, newSize, newSize)
}

fun Bitmap.rotate(angle: Float): Bitmap {
  if (angle == 0.0f) {
    return this
  }
  val matrix = Matrix()
  matrix.postRotate(angle)
  return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}
