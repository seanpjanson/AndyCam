package com.andyscan.andycam;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

final class UT {   private UT() {}

  // IMAGE_SZ the desired (approximate) image size specified as size of a square the
  //   final image should fit. For instance, the value of 1600 will attempt to select
  //   the picture size closest to 1600 x ... where the 1600 is the longer side.
  //   Depending on the camera's offering, the result may different.
  //   Value of 0, takes the camera's screen size and tries to find the closest
  //   picture size. For instance, if the current screen real-estate is 1184 x 768
  //   (varies even on a single device based on current orientation, action bars etc..),
  //   the picture size may end up being 1280x720
  static final int IMAGE_SZ = 1600;   // 0 for preview area, else 800, 1200, 1600, 2048 ...;

  // FIT-IN controls the relation between camera preview and final picture.
  //   Since the desired picture dimensions are not initially known, the picture
  //   sides' ratio is unknown as well (for instance for desired IMG_SZ of 1600,
  //   the camera returns 1600x1200 picture size.
  //   In the next step, the 1600x1200 (1.333 ratio) is compared with preview sizes
  //   offered by the camera and the closest preview ratio is selected, for instance
  //   1280x720 (1.777 ratio). The ratio discrepancy will result either
  //   - in fitting the picture inside the preview (FIT_IN = true) resulting in
  //     centered image with black stripes filling the area, or
  //   - in filling the available screen with two sides of the image 'overflowing'
  //     resulting in final image larger the visible on the streen
  static final boolean FIT_IN = true; // letterbox / pan&scan(overflowing) preview

  // IMAGE_QUAL camera delivers the image as a byte array (JPEG-compressed).
  //   The IMAGE_QUAL (0...100) sets the desired compression / quality ratio.
  static final int IMAGE_QUAL = 90;   // desired jpeg quality from the camera

  private static final String L_TG = "A_S";
  private static final String E_TG = L_TG;

  private static UT  mInst;
  static Context acx;
  //N7-I(Nexus7-1stGen), PD10 have LAND default, N7-II, SAMS have PORT
  private static boolean isLandTab;
  static UT init(Context ctx, Activity act) {
    if (mInst == null) {
      acx = ctx.getApplicationContext();
      isLandTab = (act != null) && getTabType(act);
      mInst = new UT();                                         //lg("img cache " + ccheSz);
    }
    return mInst;
  }

  static int getDegs(int rot) {   //TAB sensing handle the N7-I, N10, PD10 tablets
    int degs = 0;
    switch (rot) {
      case Surface.ROTATION_0:      degs =  90;    break;
      case Surface.ROTATION_180:    degs = 270;    break;
      case Surface.ROTATION_270:    degs = 180;    break;
    }
    if (isLandTab) {  // NEX7(1st gen), PD10
      degs = (degs + 270) % 360;   //deduct 90, limit to  0...360
    }
    return degs;
  }

  static Bitmap getRotBM(byte[] buf, int rot) {
    Bitmap bm = null;
    if (buf != null) try {
      bm = rotBM(BitmapFactory.decodeByteArray(buf, 0, buf.length), getDegs(rot));
    } catch (OutOfMemoryError oom) { System.gc(); }
    catch (Exception e) { le(e); }
    return bm;
  }
  private static Bitmap rotBM(Bitmap src, float degs) {
    if (degs == 0) return src;
    Bitmap bm = null;
    if (src != null) {
      Matrix matrix = new Matrix();
      matrix.postRotate(degs);
      bm = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }
    return bm;
  }

  private static boolean getTabType(Activity act) {
    Display dsp = act.getWindowManager().getDefaultDisplay();
    Point dispSz = new Point();
    dsp.getSize(dispSz);
    int rot = dsp.getRotation();
    return (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270) ?
     (dispSz.x < dispSz.y) : (dispSz.x > dispSz.y);
  }
  static int getOri(Activity act) {    // returns 0,1,8,9  (SCREEN_ORIENTATION_...)
    Display dsp = act.getWindowManager().getDefaultDisplay();
    Point dispSz = new Point();
    dsp.getSize(dispSz);
    int rot = dsp.getRotation();
    int ori;
    if (dispSz.x > dispSz.y) {
      switch (rot) {
        case Surface.ROTATION_0:  default: //lg("NLAND on P10, NEX7 I");
          ori = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          break;
        case Surface.ROTATION_90:          //lg("NLAND on phn, SAM, NEX7 II");
          ori = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          break;
        case Surface.ROTATION_180:         //lg("RLAND on P10, NEX7 I");
          ori = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
          break;
        case Surface.ROTATION_270:         //lg("RLAND on phn, SAM, NEX7 II");
          ori = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
          break;
      }
    } else {
      switch (rot) {
        case Surface.ROTATION_0:  default: //lg("NPORT,RPORT on phn, NPORT on SAM");
          ori = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          break;
        case Surface.ROTATION_90:          //lg("RPORT on P10, , NEX7 I");
          ori = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
          break;
        case Surface.ROTATION_180:         //lg("RPORT on SAM, NEX7 II");
          ori = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
          break;
        case Surface.ROTATION_270:         //lg("NPORT  on P10, , NEX7 I");
          ori = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          break;
      }
    }
    return ori;
  }
  static int lockRot(Activity act, int ori) {      // returns 0,1,2,3  (Surface.ROTATION_...)
    act.setRequestedOrientation(ori);
    return act.getWindowManager().getDefaultDisplay().getRotation();
  }

  private static String stack2String(Throwable ex) {
    final Writer result = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(result);
    try {
      ex.printStackTrace(printWriter);
      return result.toString();
    } finally {printWriter.close();}
  }
  private static void logX(String msg, boolean bCaller) {
    if (msg == null)
      msg = "";
    StackTraceElement[] ste = Thread.currentThread().getStackTrace();
    String cNm = (!bCaller || ste.length < 6) ? null : " <-"+ste[5].getMethodName();
    Log.d(L_TG, msg + (cNm == null ? "" : cNm));
  }
  private static void leX(Throwable ex, String msg){
    try {
      String err = (ex == null || ex.getMessage() == null) ? "?" : ex.getMessage();
      msg = (msg == null) ? err : msg + (": " + err);
      Log.e(E_TG, msg + "\n " + stack2String(ex));
    } catch (Exception ignored){}
  }
  static void le(Throwable ex) {leX (ex, null);}
  static void lg(String msg) {logX(msg, false);}
}
