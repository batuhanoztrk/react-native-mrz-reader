package com.mrzreader.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.text.Html
import android.util.Log
import com.googlecode.leptonica.android.AdaptiveMap
import com.googlecode.leptonica.android.Binarize
import com.googlecode.leptonica.android.Convert
import com.googlecode.leptonica.android.Enhance
import com.googlecode.leptonica.android.ReadFile
import com.googlecode.leptonica.android.Rotate
import com.googlecode.leptonica.android.Skew
import com.googlecode.leptonica.android.WriteFile
import com.mrzreader.R
import net.sf.scuba.data.Gender
import org.jmrtd.lds.icao.MRZInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern

class OcrUtil {
  var imageTextReader: ImageTextReader? = null

  private fun preProcessBitmap(tBitmap: Bitmap): Bitmap? {
    var bitmap = tBitmap
    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    var pix = ReadFile.readBitmap(bitmap)
    pix = Convert.convertTo8(pix)
    pix = AdaptiveMap.pixContrastNorm(pix)
    pix = Enhance.unsharpMasking(pix)
    pix = Binarize.otsuAdaptiveThreshold(pix)
    val f = Skew.findSkew(pix)
    pix = Rotate.rotate(pix, f)
    return WriteFile.writeBitmap(pix)
  }

  fun writeData(
    context: Context,
    tessOcrFilePath: String,
    tessOcrFilePathName: String,
    modelType: String
  ) {
    try {
      val `is` = context.resources.openRawResource(R.raw.mrz)
      val cascadeDir = File(tessOcrFilePath, tessOcrFilePathName + modelType)

      val os = FileOutputStream(cascadeDir)
      val buffer = ByteArray(4096)
      var bytesRead: Int
      while (`is`.read(buffer).also { bytesRead = it } != -1) {
        os.write(buffer, 0, bytesRead)
      }
      `is`.close()
      os.close()
    } catch (e: IOException) {
      Log.e("OcrUtil", "Error writing data: $e")
    }
  }

  @SuppressLint("SetTextI18n")
  fun convertImageToText(
    bitmap: Bitmap,
    docType: DocType
  ): MRZInfo? {
    try {
      val bitmapProcess = preProcessBitmap(bitmap)
      val res = imageTextReader?.getTextFromBitmap(bitmapProcess)
      val cleanText =
        Html.fromHtml(res).toString().trim().replace(" ", "")

      val mrzInfo = filterScannedText(cleanText, docType)

      return mrzInfo
    } catch (e: Exception) {
      Log.e("OcrUtil", "Error converting image to text: $e")
    }

    return null
  }

  private fun filterScannedText(element: String, docType: DocType): MRZInfo? {
    try {
      var scannedTextBuffer = ""
      scannedTextBuffer += element.replace(" ", "")
      scannedTextBuffer = scannedTextBuffer.trim().filter { !it.isWhitespace() }


      if (docType == DocType.ID_CARD) {
        if (scannedTextBuffer.contains("I<TUR")) {
          val idControl = scannedTextBuffer.contains("I<TUR")
          val idControl2 = scannedTextBuffer.contains("1<TUR")
          val idControl3 = scannedTextBuffer.contains("1KTUR")
          val idControl4 = scannedTextBuffer.contains("IKTUR")


          if (idControl || idControl2 || idControl3 || idControl4) {
            var intFirstPosition = -1

            if (idControl) {
              intFirstPosition = scannedTextBuffer.indexOf("I<TUR")
            } else if (idControl2) {
              typeIdCard = "1<"
              intFirstPosition = scannedTextBuffer.indexOf("1<TUR")
            } else if (idControl3) {
              typeIdCard = "1K"
              intFirstPosition = scannedTextBuffer.indexOf("1KTUR")
            } else {
              typeIdCard = "IK"
              intFirstPosition = scannedTextBuffer.indexOf("IKTUR")
            }

            if (intFirstPosition == -1) return null

            val strClearValue = scannedTextBuffer.substring(intFirstPosition)
            val intClearValueLen = strClearValue.length

            if (intClearValueLen == 90) {
              scannedTextBuffer = strClearValue
            }
          }
        }
      }


      if (scannedTextBuffer.contains("P<")) {
        val intFirstPosition = scannedTextBuffer.indexOf("P<")

        val strClearValue = scannedTextBuffer.substring(intFirstPosition)
        val intClearValueLen = strClearValue.length

        if (intClearValueLen == 85) {
          scannedTextBuffer = strClearValue
        }
      }

      if (docType === DocType.ID_CARD) {
        val patternIDCardTD1Line1 = Pattern.compile(ID_CARD_TD_1_LINE_1_REGEX)
        val matcherIDCardTD1Line1 = patternIDCardTD1Line1.matcher(scannedTextBuffer)
        val patternIDCardTD1Line2 = Pattern.compile(ID_CARD_TD_1_LINE_2_REGEX)
        val matcherIDCardTD1Line2 = patternIDCardTD1Line2.matcher(scannedTextBuffer)


        if (matcherIDCardTD1Line1.find() && matcherIDCardTD1Line2.find()) {

          var line1 = matcherIDCardTD1Line1.group(0)?.toString() ?: ""
          val line2 = matcherIDCardTD1Line2.group(0)?.toString() ?: ""
          var mrzInfo: MRZInfo? = null
          if (scannedTextBuffer.indexOf(typeIdCard) >= 0 && line1.length >= 14 && line2.length >= 14) {

            line1 = scannedTextBuffer.substring(
              scannedTextBuffer.indexOf(
                typeIdCard
              )
            )

            if (line1.length >= 14) {
              var documentNumber = line1.substring(5, 14)
              if (documentNumber.substring(3, 4) == "0") {
                documentNumber =
                  documentNumber.substring(0, 3) + "O" + documentNumber.substring(4, 9)
              }
              //  documentNumber = documentNumber.replace("O", "0");
              val dateOfBirthDay = line2.substring(0, 6)
              val expiryDate = line2.substring(8, 14)
              mrzInfo = buildTempMrz(documentNumber, dateOfBirthDay, expiryDate, docType)
              val chkdocumentNumber = line1.substring(14, 15)[0]
              val chkdateOfBirthDay = line2.substring(6, 7)[0]
              val chkexpiryDate = line2.substring(14, 15)[0]
              val valdocumentNumber = MRZInfo.checkDigit(documentNumber)
              val valdateOfBirthDay = MRZInfo.checkDigit(dateOfBirthDay)
              val valexpiryDate = MRZInfo.checkDigit(expiryDate)
              val comparedocumentNumber = chkdocumentNumber.compareTo(valdocumentNumber)
              val comparedateOfBirthDay = chkdateOfBirthDay.compareTo(valdateOfBirthDay)
              val compareexpiryDate = chkexpiryDate.compareTo(valexpiryDate)

              if (comparedocumentNumber != 0) mrzInfo = null
              if (comparedateOfBirthDay != 0) mrzInfo = null
              if (compareexpiryDate != 0) mrzInfo = null
            }
          }

          return mrzInfo
        }
      } else if (docType === DocType.PASSPORT) {
        val patternPassportTD3Line1 = Pattern.compile(PASSPORT_TD_3_LINE_1_REGEX)
        val matcherPassportTD3Line1 = patternPassportTD3Line1.matcher(scannedTextBuffer)

        val patternPassportTD3Line2 = Pattern.compile(PASSPORT_TD_3_LINE_2_REGEX)
        val matcherPassportTD3Line2 = patternPassportTD3Line2.matcher(scannedTextBuffer)

        if (matcherPassportTD3Line1.find() && matcherPassportTD3Line2.find()) {
          //val line1 = matcherPassportTD3Line1.group(0)
          val line2 = matcherPassportTD3Line2.group(0) ?: return null

          val documentNumber = line2.substring(0, 9)
          //documentNumber = documentNumber.replace("O", "0")
          val dateOfBirthDay = line2.substring(13, 19)
          val expiryDate = line2.substring(21, 27)

          var mrzInfo: MRZInfo? = null

          //-----------------------------

          val chkdocumentNumber = line2.substring(9, 10)[0]
          val chkdateOfBirthDay = line2.substring(19, 20)[0]
          val chkexpiryDate = line2.substring(27, 28)[0]
          val valdocumentNumber = MRZInfo.checkDigit(documentNumber)
          val valdateOfBirthDay = MRZInfo.checkDigit(dateOfBirthDay)
          val valexpiryDate = MRZInfo.checkDigit(expiryDate)
          val comparedocumentNumber = chkdocumentNumber.compareTo(valdocumentNumber)
          val comparedateOfBirthDay = chkdateOfBirthDay.compareTo(valdateOfBirthDay)
          val compareexpiryDate = chkexpiryDate.compareTo(valexpiryDate)
          var isValidMrz = true
          if (comparedocumentNumber != 0) {
            mrzInfo = null
            isValidMrz = false
          }
          if (comparedateOfBirthDay != 0) {
            mrzInfo = null
            isValidMrz = false
          }
          if (compareexpiryDate != 0) {
            mrzInfo = null
            isValidMrz = false
          }
          //---------------------
          if (isValidMrz) {
            val mrzInfoTemp: MRZInfo? =
              buildTempMrz(documentNumber, dateOfBirthDay, expiryDate, docType)
            if (mrzInfoTemp != null) {
              mrzInfo = mrzInfoTemp
            }
          }

          return mrzInfo
        }
      }

    } catch (_: Exception) {
    }
    return null
  }

  @Synchronized
  private fun buildTempMrz(
    documentNumber: String,
    dateOfBirth: String,
    expiryDate: String,
    docType: DocType
  ): MRZInfo? {
    var mrzInfo: MRZInfo? = null
    try {
      if (docType === DocType.ID_CARD) {
        mrzInfo = MRZInfo(
          "P",
          "NNN",
          "",
          "",
          documentNumber,
          "NNN",
          dateOfBirth,
          Gender.UNSPECIFIED,
          expiryDate,
          ""
        )
      }
      if (docType === DocType.PASSPORT) {
        mrzInfo = MRZInfo(
          "P",
          "NNN",
          "",
          "",
          documentNumber,
          "NNN",
          dateOfBirth,
          Gender.UNSPECIFIED,
          expiryDate,
          ""
        )
      }
      if (docType === DocType.FRONT) {
        mrzInfo = MRZInfo(
          "P",
          "NNN",
          "",
          "",
          documentNumber,
          "NNN",
          dateOfBirth,
          Gender.UNSPECIFIED,
          expiryDate,
          ""
        )
      }
      if (docType === DocType.OLD_ID) {
        mrzInfo = MRZInfo(
          "P",
          "NNN",
          "",
          "",
          documentNumber,
          "NNN",
          dateOfBirth,
          Gender.UNSPECIFIED,
          expiryDate,
          ""
        )
      }
    } catch (_: Exception) {
    }
    return mrzInfo
  }

  companion object {
    private const val ID_CARD_TD_1_LINE_1_REGEX = "([A|C|I][A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{31})"
    private const val ID_CARD_TD_1_LINE_2_REGEX =
      "([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z]{3})([A-Z0-9<]{11})([0-9]{1})"
    private const val PASSPORT_TD_3_LINE_1_REGEX = "(P[A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{39})"
    private const val PASSPORT_TD_3_LINE_2_REGEX =
      "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9]{1})([0-9]{1})"
  }


  private var typeIdCard = "I<"


  enum class DocType {
    PASSPORT, ID_CARD, OTHER, FRONT, OLD_ID, OLD_DRIVER, NEW_DRIVER
  }
}
