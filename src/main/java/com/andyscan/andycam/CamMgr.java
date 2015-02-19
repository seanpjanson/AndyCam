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
// endregion

import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
final class CamMgr {   private CamMgr(){}

  private static CamVw mCamVw;
  private static Camera mCamera;
  private static SurfaceHolder mSurfHolder;

  private static boolean mIsBusy;
  private static boolean mIsPreVw;

  static void init (CamVw camVw, SurfaceView sv){
    mCamVw = camVw;
    mSurfHolder = sv.getHolder();
    mSurfHolder.addCallback(camVw);
  }

  static void open() {  // called from RESUME
    if (mCamera == null) {   // don't re-open
      try {
        mCamera = Camera.open();                                      //UT.lg("cam opened ");
      } catch (Exception e) {UT.le(e);}
      if (mCamera == null) try {  // OUCH !!!
        Method m = Camera.class.getMethod("open", Integer.TYPE);
        mCamera = (Camera)m.invoke(null, 0);                       //UT.lg("on second attempt");
      } catch (Exception e) {UT.le(e);}
    }
  }

  static void close() {  // called from PAUSE to release the camera for use by other apps
    if (mCamera != null) {
      preview(false);
      mCamera.release();
      mCamera = null;                                              //UT.lg("cam closed");
    }
  }

  static void snapPic() {
    if (mCamera != null && mIsPreVw && !mIsBusy) try {                           //UT.lg("ready");
      mIsBusy = true;
      mCamera.autoFocus(new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean bSuccess, Camera cam) {                                    //UT.lg("focused");
          mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera cam) {
              preview(false);
              mCamVw.onPicTaken(data.clone());                    //UT.lg("taken " + data.length);
              mIsBusy = false;
            }
          });
        }
      });
    }catch (Exception e) {UT.le(e);}
  }

  static void setHolder(SurfaceHolder sh) { mSurfHolder = sh; }

  static float setCamera(SurfaceHolder surfHldr, int wid, int hei, int orient) {
    float ratio = 0.0f;
    if (mCamera != null) try {
      preview(false);
      Parameters prms = mCamera.getParameters();    //UT.lg(""+wid+"x"+hei+" "+(float)wid/hei);

      Camera.Size picSz = pictSz(prms.getSupportedPictureSizes(), wid, hei, UT.IMAGE_SZ);
      if (picSz != null) {
        prms.setPictureSize(picSz.width, picSz.height);
        Camera.Size pvwSz = prvwSz(prms.getSupportedPreviewSizes(), picSz.width, picSz.height);
        if (pvwSz != null){
          prms.setPreviewSize(pvwSz.width, pvwSz.height);
          ratio = (float)pvwSz.width/pvwSz.height;
        }
      }

      if (UT.acx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)){
        if (!prms.getFocusMode().equals(Parameters.FOCUS_MODE_AUTO)) {
          prms.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        }
        if (prms.getMaxNumFocusAreas() > 0){ // check that metering areas are supported
          List<Camera.Area> focusAreas = new ArrayList<>();
          focusAreas.add(new Camera.Area(new Rect(-300, -300, 300, 300), 1000));
          prms.setMeteringAreas(focusAreas);
        }
      }

      prms.setJpegQuality(UT.IMAGE_QUAL);

      mCamera.setParameters(prms);

      mCamera.setDisplayOrientation(orient);
      mCamera.setPreviewDisplay(surfHldr);

      preview(true);
    } catch (Exception e) {UT.le(e);}
    return ratio;
  }

  private static Camera.Size pictSz(List<Camera.Size> sizes, int wid, int hei, int imgSz) {
    if (sizes != null && (wid * hei) != 0) {                      //String allSzs = "";
      // required area is the preview area  if not explicitly requested by 'img size'  envelope
      int reqSz = (imgSz == 0 ) ?
       (wid * hei) : imgSz * (wid > hei ? (imgSz * hei) / wid : (imgSz * wid) / hei);
      Camera.Size posOptimSz = null, negOptimSz = null;
      int posDiff = +Integer.MAX_VALUE;
      int negDiff = -Integer.MAX_VALUE;
      for (Camera.Size size : sizes) {         //allSzs += (" " + size.width + "x" + size.height);
        int curSz = size.width * size.height;
        int diff = curSz - reqSz;
        if (diff >= 0) {
          if (posDiff >= diff) {
            posDiff = diff;
            posOptimSz = size;                    //UT.lg("larger " + sz.width + "x" + sz.height);
          }
        } else {
          if (negDiff < diff) {
            negDiff = diff;
            negOptimSz = size;                  //UT.lg("smaller " + sz.width + "x" + sz.height);
          }
        }
      }                                                             // UT.lg("P" + allSzs);
      return posOptimSz != null ?  posOptimSz : negOptimSz;    // preferring closest larger
    }
    return null;
  }

  private static Camera.Size prvwSz(List<Camera.Size> sizes, int wid, int hei) {
    Camera.Size sz = null;
    if (sizes != null && (wid * hei) != 0) {                          //String allSzs = "";
      float wantRat = (float)wid / hei;        //UT.lg("d: " + wid + "x" + hei + " " + dispRat);

      // first step, get the closest ratio
      float minFDiff = Float.MAX_VALUE;
      float minRat = 0;
      for (Camera.Size size : sizes) {         //allSzs += (" " + size.width + "x" + size.height);
        float hasRat = (float)size.width / size.height;
        float ratDif = Math.abs(hasRat - wantRat);
        if (ratDif < minFDiff) {
          minFDiff = ratDif;
          minRat = hasRat;
        }
      }                                                            //UT.lg("C" + allSzs);
      // for every item close to ratio (+/- 8%), find the closest size match
      int reqSz = hei * wid;
      int minIDiff = Integer.MAX_VALUE;
      for (Camera.Size size : sizes) {
        float camRat = (float)size.width / size.height;
        if (Math.abs(camRat - minRat) <= 0.08f) {
          int szDif = Math.abs((size.width * size.height) - (reqSz));
          if (szDif < minIDiff) {
            minIDiff = szDif;
            sz = size;
          }                           //UT.lg("" + sz.width + "x" + sz.height + " " + camRat);
        }
      }                                           //UT.lg("" + pvSz.width + "x" + pvSz.height);
    }
    return sz;
  }

  private static void preview(boolean bOn) {
    if (mCamera != null) {
      if (bOn) {
        if (!mIsPreVw) {
          mCamera.startPreview();                                   //UT.lg("turn started");
          mIsPreVw = true;
        }
      } else {
        if (mIsPreVw) {
          mCamera.stopPreview();                                   //UT.lg("turn stopped");
          mIsPreVw = false;
        }
      }
    }
  }
}

