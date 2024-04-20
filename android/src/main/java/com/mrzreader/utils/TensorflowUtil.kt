package com.mrzreader.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.mrzreader.dto.Coordinates
import com.mrzreader.dto.Line
import com.mrzreader.dto.Point
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.min

class TensorFlowUtil {
  companion object {
    fun softmax(scores: FloatArray): FloatArray {
      val maxScore = scores.maxOrNull() ?: 0f
      val expScores = scores.map { exp(it - maxScore) }.toFloatArray()
      val sumExpScores = expScores.sum()
      return expScores.map { it / sumExpScores }.toFloatArray()
    }

    fun argmax(values: FloatArray): Int {
      var re = Float.MIN_VALUE
      var arg = -1
      for (i2 in values.indices) {
        if (values[i2] > re) {
          re = values[i2]
          arg = i2
        }
      }
      return arg
    }

    fun findIntersection(line1: Line, line2: Line): Point {
      val x1 = line1.p1.x
      val y1 = line1.p1.y
      val x2 = line1.p2.x
      val y2 = line1.p2.y

      val x3 = line2.p1.x
      val y3 = line2.p1.y
      val x4 = line2.p2.x
      val y4 = line2.p2.y

      val denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)

      // Eğer payda 0 ise, çizgiler paraleldir veya tamamen çakışıyordur ve kesişim noktası yoktur.
      if (denominator == 0f) {
        throw IllegalArgumentException("Lines are parallel or coincident")
      }

      val px =
        ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denominator
      val py =
        ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denominator

      return Point(px, py)
    }

    fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap {
      val matrix = Matrix().apply { preScale(-1f, 1f) }
      return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun flipBitmapVertically(bitmap: Bitmap): Bitmap {
      val matrix = Matrix().apply { preScale(1f, -1f) }
      return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun convertBitmapToByteBuffer(bp: Bitmap, width: Int, height: Int): ByteBuffer {
      val imgData = ByteBuffer.allocateDirect(java.lang.Float.BYTES * width * height * 3)
      imgData.order(ByteOrder.nativeOrder())
      val bitmap = Bitmap.createScaledBitmap(bp, width, height, true)
      val intValues = IntArray(width * height)
      bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

      var pixel = 0

      for (i in 0..<width) {
        for (j in 0..<height) {
          val `val` = intValues[pixel++]

          imgData.putFloat(((`val` shr 16) and 0xFF) / 255f)
          imgData.putFloat(((`val` shr 8) and 0xFF) / 255f)
          imgData.putFloat((`val` and 0xFF) / 255f)
        }
      }
      return imgData
    }

    fun cropBitmapWithCoordinates(originalBitmap: Bitmap, coordinates: Coordinates): Bitmap {
      val width1 = coordinates.topRight.x - coordinates.topLeft.x
      val width2 = coordinates.bottomRight.x - coordinates.bottomLeft.x

      val width = if (width1 > width2) width1 else width2

      val height1 = coordinates.bottomLeft.y - coordinates.topLeft.y
      val height2 = coordinates.bottomRight.y - coordinates.topRight.y

      val height = if (height1 > height2) height1 else height2

      if (width.toInt() <= 0 || height.toInt() <= 0) {
        return originalBitmap
      }

      // Yeni bir bitmap ve canvas oluştur
      val croppedBitmap =
        Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
      val canvas = Canvas(croppedBitmap)

      val src = floatArrayOf(
        coordinates.topLeft.x,
        coordinates.topLeft.y + 1,
        coordinates.topRight.x,
        coordinates.topRight.y + 1,
        coordinates.bottomRight.x,
        coordinates.bottomRight.y + 1,
        coordinates.bottomLeft.x,
        coordinates.bottomLeft.y + 1
      )

      val dsc = floatArrayOf(
        0.0f, 0.0f, width, 0.0f, width, height, 0.0f, height
      )

      val matrix = Matrix()

      matrix.setPolyToPoly(src, 0, dsc, 0, 4)

      canvas.drawBitmap(originalBitmap, matrix, null)

      return croppedBitmap
    }

    fun draw(originalBitmap: Bitmap, points: List<Point>, coordinates: Coordinates): Bitmap {
      val bitmap =
        originalBitmap.copy(Bitmap.Config.ARGB_8888, true) // Kopyayı değiştirilebilir yap

      val canvas = Canvas(bitmap)

      val paint = Paint()
      paint.color = Color.RED
      paint.style = Paint.Style.FILL

      for (point in points) {
        canvas.drawCircle(point.x, point.y, 10f, paint)
      }

      val paint2 = Paint()
      paint2.color = Color.GREEN
      paint2.style = Paint.Style.FILL

      canvas.drawCircle(coordinates.topLeft.x, coordinates.topLeft.y, 10f, paint2)
      canvas.drawCircle(coordinates.topRight.x, coordinates.topRight.y, 10f, paint2)
      canvas.drawCircle(coordinates.bottomRight.x, coordinates.bottomRight.y, 10f, paint2)
      canvas.drawCircle(coordinates.bottomLeft.x, coordinates.bottomLeft.y, 10f, paint2)

      val paint3 = Paint()
      paint3.color = Color.BLUE
      paint3.style = Paint.Style.STROKE
      paint3.strokeWidth = 5f

      canvas.drawLine(
        coordinates.topLeft.x,
        coordinates.topLeft.y,
        coordinates.topRight.x,
        coordinates.topRight.y,
        paint3
      )

      canvas.drawLine(
        coordinates.topRight.x,
        coordinates.topRight.y,
        coordinates.bottomRight.x,
        coordinates.bottomRight.y,
        paint3
      )

      canvas.drawLine(
        coordinates.bottomRight.x,
        coordinates.bottomRight.y,
        coordinates.bottomLeft.x,
        coordinates.bottomLeft.y,
        paint3
      )

      canvas.drawLine(
        coordinates.bottomLeft.x,
        coordinates.bottomLeft.y,
        coordinates.topLeft.x,
        coordinates.topLeft.y,
        paint3
      )

      return bitmap
    }

    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
      if (angle == 0.0f) {
        return source
      }
      val matrix = Matrix()
      matrix.postRotate(angle)
      return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun cropBitmapToCenterSquare(bitmap: Bitmap): Bitmap {
      // Gelen bitmap'in genişlik ve yüksekliğine bağlı olarak kısa kenarı bul
      val newSize = min(bitmap.width, bitmap.height)

      // Yeni bitmap için başlangıç koordinatlarını hesapla
      val xStart = (bitmap.width - newSize) / 2
      val yStart = (bitmap.height - newSize) / 2

      // Bitmap'i keserek yeni bir kare bitmap oluştur
      return Bitmap.createBitmap(bitmap, xStart, yStart, newSize, newSize)
    }
  }
}
