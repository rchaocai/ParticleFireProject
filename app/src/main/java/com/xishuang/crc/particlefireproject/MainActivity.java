package com.xishuang.crc.particlefireproject;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;

import com.xishuang.crc.lib.CustomGLSurfaceView;

public class MainActivity extends AppCompatActivity {
    public int screen_width;
    public int screen_height;
    private CustomGLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏设置
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //获取屏幕宽高
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (metrics.widthPixels > metrics.heightPixels) {
            screen_width = metrics.widthPixels;
            screen_height = metrics.heightPixels;
        } else {
            screen_width = metrics.heightPixels;
            screen_height = metrics.widthPixels;
        }
        //设置横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mGLSurfaceView = new CustomGLSurfaceView(this, screen_width, screen_height);
        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }
}
