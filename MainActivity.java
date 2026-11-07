package com.crashdrive.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout.LayoutParams;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        FrameLayout root = new FrameLayout(this);
        GameView game = new GameView(this);
        root.addView(game);
        TextView help = new TextView(this);
        help.setText("CRASHDRIVE MOBILE  •  ЛЕВАЯ ЧАСТЬ: РУЛЬ  •  ПРАВАЯ: ГАЗ / ТОРМОЗ");
        help.setTextColor(Color.WHITE); help.setTextSize(12); help.setGravity(Gravity.CENTER);
        help.setBackgroundColor(0x66000000);
        LayoutParams hp = new LayoutParams(-1, 42, Gravity.TOP);
        root.addView(help, hp);
        setContentView(root);
    }

    static class GameView extends GLSurfaceView {
        final DriveRenderer renderer;
        float steer = 0, throttle = 0;
        public GameView(Activity a) {
            super(a);
            setEGLContextClientVersion(2);
            renderer = new DriveRenderer();
            setRenderer(renderer);
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX() / getWidth(), y = e.getY() / getHeight();
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                steer = 0; throttle = 0;
            } else {
                if (x < 0.48f) steer = (x - 0.24f) / 0.24f;
                else throttle = y > 0.55f ? 1f : -0.45f;
            }
            renderer.setInput(Math.max(-1, Math.min(1, steer)), throttle);
            return true;
        }
    }
}
