package com.mrzreader.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import org.jmrtd.lds.icao.MRZInfo
import java.io.File

class MrzUtil(private val context: Context) {
  private var tessOcrFilePath: String? = null
  private var tessOcrFileDocPath: String? = null
  private var tessOcrFilePathName: String = "eng"
  private var modelType: String = ".traineddata"
  private var currentDirectory: File? = null
  private var imageTextReader: ImageTextReader? = null
  private var mPageSegMode = 1
  private val ocrUtil = OcrUtil()

  init {
    initDirectories()
    initializeOCR()
  }

  private fun initDirectories() {
    tessOcrFileDocPath =
      context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        .toString()
    tessOcrFilePath = "$tessOcrFileDocPath/tessdata"
    currentDirectory = File(tessOcrFilePath!!)
    currentDirectory!!.mkdirs()
  }

  private fun isLanguageDataExists(): Boolean {
    ocrUtil.writeData(
      context,
      tessOcrFilePath!!,
      tessOcrFilePathName,
      modelType,
    )

    currentDirectory = File(tessOcrFilePath!!, tessOcrFilePathName + modelType)

    return if (tessOcrFilePathName.contains("+")) {
      val langCodes = tessOcrFilePathName.split("\\+".toRegex()).toTypedArray()
      for (code in langCodes) {
        if (!currentDirectory!!.exists()) return false
      }
      true
    } else {
      currentDirectory!!.exists()
    }
  }

  private fun initializeOCR() {
    if (isLanguageDataExists()) {
      object : Thread() {
        override fun run() {
          try {
            if (imageTextReader != null) {
              imageTextReader!!.tearDownEverything()
            }

            imageTextReader = ImageTextReader.geInstance(
              tessOcrFileDocPath, tessOcrFilePathName, mPageSegMode
            ) {}

            imageTextReader?.let {
              if (!it.success) {
                val destf = currentDirectory
                destf?.delete()
                imageTextReader = null
              }
            }

            imageTextReader?.let {
              ocrUtil.imageTextReader = it
            } ?: run {
              Log.e("MrzUtil", "Language data not found")
            }
          } catch (e: Exception) {
            val destf = currentDirectory
            destf?.delete()

            imageTextReader = null
          }
        }
      }.start()
    } else {
      Log.e("MrzUtil", "Language data not found")
    }
  }

  fun readMRZ(bitmap: Bitmap, docType: OcrUtil.DocType): MRZInfo? {
    try {
      val mrzInfo = ocrUtil.convertImageToText(bitmap, docType)

      return mrzInfo
    } catch (e: Exception) {
      Log.e("MrzUtil", "Error reading MRZ: $e")
    }

    return null
  }
}
