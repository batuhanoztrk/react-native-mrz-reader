package com.mrzreader.utils;

import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * This class convert the image to text and return the text on image
 */
public class ImageTextReader {

  public boolean success;
  /**
   * TessBaseAPI instance
   */
  private static volatile TessBaseAPI api;

  /**
   * initialize and train the tesseract engine
   *
   * @param path     a path to training data
   * pathName code i.e. selected by user
   * @return the instance of this class for later use
   */


  public static ImageTextReader geInstance(
    String path,
    String pathName,
    int pageSegMode,
    TessBaseAPI.ProgressNotifier progressNotifier
  ) {
    try {
      ImageTextReader imageTextReader = new ImageTextReader();
      api = new TessBaseAPI(progressNotifier);
      imageTextReader.success = api.init(path, pathName);
      api.setPageSegMode(pageSegMode);
      return imageTextReader;
    } catch (Exception e) {
      System.out.println(e.getLocalizedMessage());
      return null;
    }
  }


  /**
   * get the text from bitmap
   *
   * @param bitmap a image
   * @return text on image
   */
  public String getTextFromBitmap(Bitmap bitmap) {
    api.setImage(bitmap);
    String textOnImage;
    try {
      //textOnImage = api.getUTF8Text();
      textOnImage = api.getHOCRText(1);
    } catch (Exception e) {
      return "Scan Failed: WTF: Must be reported to developer!";
    }
    if (textOnImage.isEmpty()) {
      return "Scan Failed: Couldn't read the image\nProblem may be related to Tesseract or no Text on Image!";
    } else return textOnImage;

  }

  /**
   * stop the image TEXT reader
   */
  public void stop() {
    api.stop();
  }

  /**
   * find the confidence or
   *
   * @return confidence
   */
  public int getAccuracy() {
    return api.meanConfidence();
  }

  /**
   * Closes down tesseract and free up all memory.
   */
  public void tearDownEverything() {
    api.recycle();
  }

  /**
   * Frees up recognition results and any stored image data,
   */
  public void clearPreviousImage() {
    api.clear();
  }

}
