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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class CamActivity extends Activity{
  private SurfaceView mCamSV;    // these two views are switched
  private ImageView   mImgVw;    // these two views are switched

  @Override
  protected void onCreate(Bundle bundle) { super.onCreate(bundle);
    if (UT.init(getApplicationContext(), this) == null ||
       !getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ) {
      Toast.makeText(this, R.string.err_init, Toast.LENGTH_LONG).show();
      finish();  return; //---suicide ------------------->>>
    }

    int rot = UT.lockRot(this, UT.getOri(this));  //ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    setContentView(R.layout.activity_main);

    mCamSV = (SurfaceView)findViewById(R.id.sv_cam);
    mCamSV.setVisibility(View.VISIBLE);
    mImgVw = (ImageView)findViewById(R.id.iv_img);
    mImgVw.setVisibility(View.GONE);
    new CamVw(this, mCamSV, rot);
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
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.act_set) {
      if (mCamSV.isShown()) {                                     //UT.lg("CAM layout visible ");
        mImgVw.setImageBitmap(null);
        CamMgr.snapPic();
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  public void onPicTaken(Bitmap bm) {
    mCamSV.setVisibility(View.GONE);
    mImgVw.setVisibility(View.VISIBLE);
    mImgVw.setImageBitmap(bm);
    if (bm != null)
      Toast.makeText(this, "" + bm.getWidth() + "x" + bm.getHeight(), Toast.LENGTH_LONG).show();
  }
}
