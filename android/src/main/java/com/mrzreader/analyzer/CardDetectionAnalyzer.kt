package com.mrzreader.analyzer

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mrzreader.dto.Coordinates
import com.mrzreader.dto.Line
import com.mrzreader.dto.Point
import com.mrzreader.dto.enums.CardType
import com.mrzreader.ml.CardDetection
import com.mrzreader.utils.MrzUtil
import com.mrzreader.utils.TensorFlowUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jmrtd.lds.icao.MRZInfo
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class CardDetectionAnalyzer(context: Context) : ImageAnalysis.Analyzer {
  private val model = CardDetection.newInstance(context)
  private val mrzUtil = MrzUtil(context)

  private val _mrzInfo = MutableLiveData<MRZInfo>()
  val mrzInfo: LiveData<MRZInfo> = _mrzInfo

  override fun analyze(image: ImageProxy) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val originalBitmap = image.toBitmap()

        val rotatedBitmap = TensorFlowUtil.rotateBitmap(
          originalBitmap,
          90f
        )

        val cropBitmapCenterSquare = TensorFlowUtil.cropBitmapToCenterSquare(
          rotatedBitmap
        )

        val resizedBitmap =
          Bitmap.createScaledBitmap(cropBitmapCenterSquare, 384, 384, true)

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)

        val inputFeature0 =
          TensorBuffer.createFixedSize(intArrayOf(1, 384, 384, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(tensorImage.buffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature1AsTensorBuffer
        val outputFeature1 = outputs.outputFeature0AsTensorBuffer

        val probabilities = TensorFlowUtil.softmax(outputFeature0.floatArray)
        val max = probabilities.maxOrNull() ?: 0.0f
        val maxIndex = probabilities.indexOfFirst { it == max }
        val cardType = CardType.values()[maxIndex]

        if (cardType == CardType.TURKISH_NEW_ID_BACK && max >= 0.8f) {
          val points = mutableListOf<Point>()

          for (i in 0 until outputFeature1.floatArray.size step 2) {
            points.add(
              Point(
                outputFeature1.floatArray[i] * cropBitmapCenterSquare.width / resizedBitmap.width,
                outputFeature1.floatArray[i + 1] * cropBitmapCenterSquare.height / resizedBitmap.height
              )
            )
          }

          val topLeft1 = points[0]
          val topLeft2 = points[7]

          val topRight1 = points[1]
          val topRight2 = points[2]

          val bottomRight1 = points[3]
          val bottomRight2 = points[4]

          val bottomLeft1 = points[5]
          val bottomLeft2 = points[6]

          val topLine = Line(topLeft1, topRight1)
          val rightLine = Line(topRight2, bottomRight1)
          val bottomLine = Line(bottomRight2, bottomLeft1)
          val leftLine = Line(bottomLeft2, topLeft2)

          val topRight = TensorFlowUtil.findIntersection(topLine, rightLine)
          val bottomRight = TensorFlowUtil.findIntersection(rightLine, bottomLine)
          val bottomLeft = TensorFlowUtil.findIntersection(bottomLine, leftLine)
          val topLeft = TensorFlowUtil.findIntersection(leftLine, topLine)

          val coordinates = Coordinates(
            topLeft = topLeft,
            topRight = topRight,
            bottomRight = bottomRight,
            bottomLeft = bottomLeft
          )

          val croppedBitmap =
            TensorFlowUtil.cropBitmapWithCoordinates(cropBitmapCenterSquare, coordinates)

          val mrzInfo = mrzUtil.readMRZ(croppedBitmap, cardType)

          mrzInfo?.let {
            _mrzInfo.postValue(it)
          }
        }
      } catch (_: Exception) {
      }

      image.close()
    }
  }
}
