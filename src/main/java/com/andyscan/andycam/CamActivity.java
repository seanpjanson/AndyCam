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
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

public class CamActivity extends Activity implements CamVw.CB {
  private SurfaceView mCamSV;
  private ImageView   mImgVw;
  private FrameLayout mWaitFl;
  private Point mTouchPt;

  @Override
  protected void onCreate(Bundle bundle) { super.onCreate(bundle);
    if (!UT.init(getApplicationContext(), this) ||
       !getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ) {
      Toast.makeText(this, R.string.err_init, Toast.LENGTH_LONG).show();
      finish();  return; //---suicide ------------------->>>
    }

    getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
    getWindow().requestFeature(Window.FEATURE_NO_TITLE);

    int rot = UT.lockRot(this, UT.getOri(this));  //ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    setContentView(R.layout.act_cam);

    mCamSV = (SurfaceView)findViewById(R.id.sv_cam);
    mCamSV.setVisibility(View.VISIBLE);

    mWaitFl = (FrameLayout)findViewById(R.id.fl_wait);
    mWaitFl.setVisibility(View.GONE);

    mImgVw = (ImageView)findViewById(R.id.iv_img);
    mImgVw.setVisibility(View.GONE);
    new CamVw(this, mCamSV, rot);
  }

  @Override
  protected void onDestroy() {
    CamMgr.close();
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    CamMgr.open();
  }

  @Override
  protected void onPause() {
    CamMgr.close();
    super.onPause();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    mTouchPt = new Point((int) event.getX(), (int) event.getY());
    return  mCamSV.isShown() &&  event.getAction() == MotionEvent.ACTION_DOWN ?
     CamMgr.snapPic(mTouchPt) : super.onTouchEvent(event);
  }

  @Override
  public void onFocus(Rect focArea) {
    int wid = mWaitFl.getLayoutParams().width/2;
    int hei = mWaitFl.getLayoutParams().height/2;
    if (wid > 0 && hei > 0) {
      FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
       FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
      );
      lp.setMargins(mTouchPt.x - wid, mTouchPt.y - hei, 0, 0);       //lg("" + wid + " " + hei);
      mWaitFl.setLayoutParams(lp);
    }
    mWaitFl.setVisibility(focArea != null ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onPicTaken(Bitmap bm) {
    mCamSV.setVisibility(View.GONE);
    mWaitFl.setVisibility(View.GONE);
    mImgVw.setVisibility(View.VISIBLE);
    mImgVw.setImageBitmap(bm);
    if (bm != null)
      Toast.makeText(this, "" + bm.getWidth() + "x" + bm.getHeight(), Toast.LENGTH_LONG).show();
  }
}
