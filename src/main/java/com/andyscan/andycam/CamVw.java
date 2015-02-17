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

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

final class CamVw extends SurfaceView implements SurfaceHolder.Callback {
  private MainActivity mAct;
  private SurfaceView mSv;
  private static int mRot;      // Surface.ROTATION_0, Surface.ROTATION_90, ...
  private static float mRatio;
  CamVw(Context act) { super(act);}

  CamVw(MainActivity act, SurfaceView sv, int rot) { this(act);
    mAct = act;
    mSv = sv;
    mRot = rot;
    CamMgr.init(this, sv);
  }

  public void onPicTaken(byte[] buf) {
    Bitmap bm = UT.getRotBM(buf, mRot);
    if (bm != null)
      Toast.makeText(mAct, "picture "+bm.getWidth()+"x"+bm.getHeight(), Toast.LENGTH_LONG).show();
    mAct.onPicTaken(bm);
  }

  @Override
  public void surfaceChanged(SurfaceHolder sh, int fmt, int wid, int hei){
    float oldRat = (float)wid/hei;     //UT.lg("_"); UT.lg("" + wid + "x" + hei + " " + oldRat);
    if (sh.getSurface() != null && Math.abs(mRatio - oldRat) > 0.03f) {
      CamMgr.preview(false);

      // CAM sizes always come in LAND, whereas width,height is PORT/LAND based on rotation.
      // so, in PORT, we may get 480x800, but mCam params list show 800x480, 640x320, ...
      if (mRot == Surface.ROTATION_0 || mRot == Surface.ROTATION_180) {
        int tmp = hei; hei = wid; wid = tmp;                 // just a swap
      }                                                      // UT.lg("_ in " + wid + "x" + hei);

      oldRat = (float)wid/hei;                //UT.lg("in " + wid + "x" + hei + " " + oldRat);
      float newRat = CamMgr.setCamera(sh, wid, hei, UT.getDegs(mRot));
      if (newRat != 0.0f && Math.abs(newRat-oldRat) > 0.03f) {
        if (UT.FIT_IN) {
          if (oldRat < newRat) hei = (int)((oldRat * hei) / newRat);
          else                 wid = (int)((newRat * wid) / oldRat);
        } else {
          if (oldRat > newRat) hei = (int)((oldRat * hei) / newRat);
          else                 wid = (int)((newRat * wid) / oldRat);
        }
        mRatio = (float)wid/hei;              //UT.lg("out " + wid + "x" + hei + " " + mRatio);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(wid, hei);
        lp.gravity = Gravity.CENTER;
        mSv.setLayoutParams(lp);
      }                                                    //else UT.lg("cool");
      Toast.makeText(mAct, "preview " + wid + "x" + hei, Toast.LENGTH_LONG).show();
    }
  }

  @Override public void surfaceCreated(SurfaceHolder sh) {CamMgr.setHolder(sh);  }

  @Override public void surfaceDestroyed(SurfaceHolder sh) {}
}
