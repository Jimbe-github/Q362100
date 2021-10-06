package com.teratail.q362100;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SurfaceDrawer implements SurfaceHolder.Callback {
  @SuppressWarnings("UnusedDeclaration")
  private static final String TAG = "SurfaceDrawer";

  private static final float COMPRESSION_RATE = 10f;

  private SurfaceHolder holder;
  private float canvasWidth, canvasVerticalCenter;
  private final Paint pathPaint;
  private final Path path = new Path();

  public SurfaceDrawer(SurfaceView surfaceView) {
    holder = surfaceView.getHolder();
    holder.addCallback(this);

    pathPaint = new Paint();
    pathPaint.setAntiAlias(true);
    pathPaint.setStrokeWidth(2);//線幅
    pathPaint.setStyle(Paint.Style.STROKE);
    pathPaint.setColor(Color.GREEN);
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    //Log.d(TAG, "surfaceCreated");
    Canvas canvas = holder.lockCanvas();
    if(canvas == null) return;

    canvas.drawColor(Color.BLACK);
    holder.unlockCanvasAndPost(canvas);
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
    //Log.d(TAG, "surfaceChanged w="+w+", h="+h);
    canvasWidth = w;
    canvasVerticalCenter = h / 2f;
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    //Log.d(TAG, "surfaceDestroyed");
  }

  public void draw(short[] data) {
    if(data == null || data.length < 2) return;

    Canvas canvas = holder.lockCanvas();
    if(canvas == null) {
      Log.d(TAG, "holder.lockCanvas() is null.");
      return;
    }

    canvas.drawColor(Color.BLACK); //塗り潰し

    path.rewind();
    path.moveTo(0, canvasVerticalCenter + data[0] / COMPRESSION_RATE);
    for(int x=1; x<canvasWidth; x++) {
      path.lineTo(x, canvasVerticalCenter + data[(int)(data.length / canvasWidth * x)] / COMPRESSION_RATE);
    }
    canvas.drawPath(path, pathPaint);

    holder.unlockCanvasAndPost(canvas);
  }
}