package com.crashdrive.mobile;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class DriveRenderer implements GLSurfaceView.Renderer {
    private final float[] projection = new float[16], view = new float[16], vp = new float[16];
    private int program, aPos, uMvp, uColor;
    private FloatBuffer cube;
    private volatile float steering, throttle;
    private float carX, speed, carZ, damage;
    private boolean bumperGone, doorGone, wheelGone;
    private long last = System.nanoTime();

    public void setInput(float s, float t) { steering = s; throttle = t; }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.48f, 0.72f, 0.91f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        String vs = "uniform mat4 uMvp; attribute vec3 aPos; void main(){ gl_Position=uMvp*vec4(aPos,1.0); }";
        String fs = "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }";
        program = GLES20.glCreateProgram();
        int v = shader(GLES20.GL_VERTEX_SHADER, vs), f = shader(GLES20.GL_FRAGMENT_SHADER, fs);
        GLES20.glAttachShader(program, v); GLES20.glAttachShader(program, f); GLES20.glLinkProgram(program);
        aPos = GLES20.glGetAttribLocation(program, "aPos");
        uMvp = GLES20.glGetUniformLocation(program, "uMvp");
        uColor = GLES20.glGetUniformLocation(program, "uColor");
        float[] vertices = {
          -1,-1,-1, 1,-1,-1, 1,1,-1, -1,1,-1,
          -1,-1,1, 1,-1,1, 1,1,1, -1,1,1
        };
        cube = ByteBuffer.allocateDirect(vertices.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        cube.put(vertices).position(0);
    }
    private int shader(int type, String src) {
        int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s); return s;
    }
    @Override public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0,0,w,h);
        Matrix.perspectiveM(projection,0,60f,(float)w/Math.max(1,h),0.1f,250f);
    }
    @Override public void onDrawFrame(GL10 gl) {
        long now=System.nanoTime(); float dt=Math.min(0.04f,(now-last)/1e9f); last=now;
        speed += throttle*10f*dt; speed *= (float)Math.pow(0.985, dt*60);
        speed = Math.max(0, Math.min(38, speed));
        carX += steering * speed * 0.055f * dt;
        carX = Math.max(-5.5f, Math.min(5.5f, carX));
        carZ += speed*dt;
        if (carZ > 28f && carZ < 31f && Math.abs(carX) < 1.8f && speed > 5f) {
            damage += speed*dt*2.2f; speed *= 0.35f;
            if (damage > 2.5f) bumperGone=true;
            if (damage > 5f) doorGone=true;
            if (damage > 8f) wheelGone=true;
        }
        if (carZ > 45f) { carZ=0; carX=0; damage=0; bumperGone=doorGone=wheelGone=false; }
        Matrix.setLookAtM(view,0, carX,3.4f,-8.5f, carX,0.5f,8f, 0,1,0);
        Matrix.multiplyMM(vp,0,projection,0,view,0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        // road and lane markings
        box(0,-1.15f,18,7,0.15f,65,0.18f,0.19f,0.21f);
        for(int i=0;i<18;i++) box(0,-0.96f,i*4f,0.12f,0.025f,1.8f,0.95f,0.9f,0.75f);
        // roadside barriers
        box(-7,-0.35f,18,0.25f,0.8f,65,0.75f,0.75f,0.78f);
        box(7,-0.35f,18,0.25f,0.8f,65,0.75f,0.75f,0.78f);
        // crash obstacle
        box(0,-0.2f,28-carZ,2.1f,0.85f,0.8f,0.8f,0.18f,0.12f);
        // vehicle body
        box(carX,0,0,1.05f,0.48f,2.0f,0.08f,0.32f,0.85f);
        box(carX,0.58f,-0.25f,0.78f,0.42f,0.95f,0.12f,0.48f,0.95f);
        if(!bumperGone) box(carX,-0.08f,1.98f,1.12f,0.16f,0.18f,0.06f,0.06f,0.07f);
        if(!doorGone) box(carX-0.99f,0.12f,0.1f,0.12f,0.38f,0.9f,0.04f,0.2f,0.55f);
        // four wheels, simplified as dark blocks
        if(!wheelGone) {
            box(carX-1.08f,-0.35f,1.15f,0.22f,0.38f,0.48f,0.025f,0.025f,0.03f);
            box(carX+1.08f,-0.35f,1.15f,0.22f,0.38f,0.48f,0.025f,0.025f,0.03f);
        }
        box(carX-1.08f,-0.35f,-1.15f,0.22f,0.38f,0.48f,0.025f,0.025f,0.03f);
        box(carX+1.08f,-0.35f,-1.15f,0.22f,0.38f,0.48f,0.025f,0.025f,0.03f);
    }
    private void box(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b) {
        float[] model=new float[16], mvp=new float[16];
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,model,0);
        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
        GLES20.glUniform4f(uColor,r,g,b,1f);
        // Draw cube faces as triangles
        float[] v={
          -1,-1,-1, 1,-1,-1, 1,1,-1, -1,-1,-1, 1,1,-1, -1,1,-1,
          -1,-1,1, 1,1,1, 1,-1,1, -1,-1,1, -1,1,1, 1,1,1,
          -1,-1,-1, -1,1,-1, -1,1,1, -1,-1,-1, -1,1,1, -1,-1,1,
          1,-1,-1, 1,-1,1, 1,1,1, 1,-1,-1, 1,1,1, 1,1,-1,
          -1,1,-1, 1,1,-1, 1,1,1, -1,1,-1, 1,1,1, -1,1,1,
          -1,-1,-1, -1,-1,1, 1,-1,1, -1,-1,-1, 1,-1,1, 1,-1,-1
        };
        FloatBuffer fb=ByteBuffer.allocateDirect(v.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(v).position(0);
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,fb);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36);
        GLES20.glDisableVertexAttribArray(aPos);
    }
}
