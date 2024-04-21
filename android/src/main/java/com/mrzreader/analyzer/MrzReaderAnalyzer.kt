package com.mrzreader.analyzer

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mrzreader.extensions.cropToCenterSquare
import com.mrzreader.extensions.rotate
import com.mrzreader.utils.MrzUtil
import com.mrzreader.utils.OcrUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jmrtd.lds.icao.MRZInfo

class MrzReaderAnalyzer(context: Context) : ImageAnalysis.Analyzer {
  private val mrzUtil = MrzUtil(context)

  private val _mrzInfo = MutableLiveData<MRZInfo>()
  val mrzInfo: LiveData<MRZInfo> = _mrzInfo

  private var docType: OcrUtil.DocType = OcrUtil.DocType.ID_CARD

  fun setDocType(docType: String) {
    this.docType = OcrUtil.DocType.valueOf(docType)
  }

  override fun analyze(image: ImageProxy) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val originalBitmap = image.toBitmap()

        val angle = image.imageInfo.rotationDegrees.toFloat()

        val rotatedBitmap = originalBitmap.rotate(
          angle
        )

        val cropBitmapCenterSquare = rotatedBitmap.cropToCenterSquare()

        val mrzInfo = mrzUtil.readMRZ(cropBitmapCenterSquare, docType)

        mrzInfo?.let {
          _mrzInfo.postValue(it)
        }

      } catch (_: Exception) {
      }

      image.close()
    }
  }
}
