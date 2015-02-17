package com.andyscan.andycam;
//region COPYRIGHT
/**
 * Copyright 2015 Sean Janson. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
//endregion

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

final class UT {   private UT() {}    // singleton pattern
  private static UT mInst;
  static Context acx;
  static boolean isLandTab;  //N7-I(Nexus7-1stGen), PD10 have LAND default, N7-II, SAMS have PORT

  static UT init(Context ctx, Activity act) {
    if (mInst == null) {
      acx = ctx.getApplicationContext();
      if (act != null) {
        Display dsp = act.getWindowManager().getDefaultDisplay();
        Point dispSz = new Point();
        dsp.getSize(dispSz);
        int rot = dsp.getRotation();
        if (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270) {
          isLandTab = dispSz.x < dispSz.y;
        } else {
          isLandTab = dispSz.x > dispSz.y;
        }
      }
      mInst = new UT();
    }
    return mInst;
  }

  private static final String L_TG = "A_S";
  private static final String E_TG = L_TG;

  // cam preview ratio may differ from final picture ratio, i.e. it will:
  // - FIT_IN ... will have black stripes on sides
  // - FIT_OUT ... part of the picture will overflow the screen
  static final boolean FIT_IN = true;
  static final int IMAGE_SZ = 1600;  // 0 for preview area, else  1200, 1600, 2048 ... envelopes;
  static final int IMAGE_QUAL = 90;

  static Bitmap getRotBM(byte[] buf, int rot) {
    Bitmap bm = null;
    if (buf != null) try {
      bm = BitmapFactory.decodeByteArray(buf, 0, buf.length);
      if (bm != null) {                 //lg(" " + bm.getWidth() + " " + bm.getHeight());
        if (isLandTab) {
          switch (rot) {
            case Surface.ROTATION_0:   default: break;
            case Surface.ROTATION_90:  bm = rotBM(bm, 270); break;
            case Surface.ROTATION_180: bm = rotBM(bm, 180); break;
            case Surface.ROTATION_270: bm = rotBM(bm, 90);  break;
          }
        } else {
          switch (rot) {
            case Surface.ROTATION_0:   bm = rotBM(bm, 90);  break;
            case Surface.ROTATION_90:  default: break;
            case Surface.ROTATION_180: bm = rotBM(bm, 270); break;
            case Surface.ROTATION_270: bm = rotBM(bm, 180);
              break;
          }
        }
      }
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

  static int getDegs(int rot) {   //TAB sensing handle the N7-I, N10, PD10 tablets
    int degs = 0;
    switch (rot) {
      case Surface.ROTATION_0:      degs =  90;    break;
      case Surface.ROTATION_90:     degs =   0;    break;
      case Surface.ROTATION_180:    degs = 270;    break;
      case Surface.ROTATION_270:    degs = 180;    break;
    }
    if (isLandTab) {  // NEX7(1st gen), PD10
      degs = (degs + 270) % 360;   //deduct 90, limit to  0...360
    }
    return degs;
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
    } catch (Exception e) {}   // ignore by design
  }
  static void le(Throwable ex) {leX (ex, null);}
  static void lg(String msg) {logX(msg, false);}
}
