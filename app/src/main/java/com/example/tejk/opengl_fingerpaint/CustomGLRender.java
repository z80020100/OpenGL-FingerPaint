package com.example.tejk.opengl_fingerpaint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
/**
 * Created by tej on 7/20/16.
 */
public class CustomGLRender implements GLSurfaceView.Renderer {
    // Our matrices
    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];
    ConcurrentLinkedQueue<Mesh> mMeshes;
    ConcurrentLinkedQueue<MousePoint> mMousePoints;
    float[] mousePoints;
    Bitmap mTexture;
    Smoother mSmoother = new Smoother();
    // Our screenresolution
    float mScreenWidth = 1280;
    float mScreenHeight = 768;
    // Misc
    Context mContext;
    long mLastTime;
    int mProgram;
    CustomGLSurface mSurface;
    private SwipeMesh swipeMesh1;
    private SwipeMesh swipeMesh2;

    public CustomGLRender(Context c, CustomGLSurface surface) {
        mContext = c;
        mLastTime = System.currentTimeMillis() + 100;
        mSurface = surface;
    }

    public static float InterpolateHermite4pt3oX(float x0, float x1, float x2, float x3, float t) {
        Log.d("<^>", String.valueOf(t));
        float c0 = x1;
        float c1 = .5F * (x2 - x0);
        float c2 = x0 - (2.5F * x1) + (2 * x2) - (.5F * x3);
        float c3 = (.5F * (x3 - x0)) + (1.5F * (x1 - x2));
        return (((((c3 * t) + c2) * t) + c1) * t) + c0;
    }

    public void onPause() {
        /* Do stuff to pause the renderer */
    }

    public void onResume() {
        /* Do stuff to resume the renderer */
        mLastTime = System.currentTimeMillis();
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Get the current time
        long now = System.currentTimeMillis();
        // We should make sure we are valid and sane
        if (mLastTime > now) return;
        // Get the amount of time the last frame took.
        long elapsed = now - mLastTime;
        // Update our example
        // Render our example
        Render(mtrxProjectionAndView);
        // Save the current time to see how long it took <img src="http://androidblog.reindustries.com/wp-includes/images/smilies/icon_smile.gif" alt=":)" class="wp-smiley"> .
        mLastTime = now;
    }

    private void Render(float[] m) {
        // clear Screen and Depth Buffer, we have set the clear color as black.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//        GLES20.glUniform4f(colorHandle, 1, 0, 0, 1);
        int mtrxhandle = GLES20.glGetUniformLocation(CustomShader.sp_Image, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

        swipeMesh1.draw(m);
        swipeMesh2.draw(m);


/*
        if (mMeshes != null && !mMeshes.isEmpty()) {
            for (Mesh mesh : mMeshes) {
                if (mesh.getTop() - mesh.getBottom() < 30 || mesh.getRight() - mesh.getLeft() < 30) {
                    mMeshes.poll();
                    continue;
                }
                mesh.draw(m);
            }
        }
        */
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // We need to know the current width and height.
        mScreenWidth = width;
        mScreenHeight = height;
        swipeMesh1.setScreenHeight(height);
        swipeMesh2.setScreenHeight(height);
        // Redo the Viewport, making it fullscreen.
        GLES20.glViewport(0, 0, (int) mScreenWidth, (int) mScreenHeight);
        // Clear our matrices
        for (int i = 0; i < 16; i++) {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }
        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0f, mScreenWidth, 0.0f, mScreenHeight, 0, 50);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);
        mousePoints = new float[4];
    }
//    public void TranslateSprite()
//    {
//        vertices = new float[]
//                {image.left, image.top, 0.0f,
//                        image.left, image.bottom, 0.0f,
//                        image.right, image.bottom, 0.0f,
//                        image.right, image.top, 0.0f,
//                };
//        // The vertex buffer.
//        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
//        bb.order(ByteOrder.nativeOrder());
//        vertexBuffer = bb.asFloatBuffer();
//        vertexBuffer.put(vertices);
//        vertexBuffer.position(0);
//    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int id = mContext.getResources().getIdentifier("drawable/particle_solid", null,
                                                       mContext.getPackageName());
        // Temporary create a bitmap
        mTexture = BitmapFactory.decodeResource(mContext.getResources(), id);
//        initTextures();
        mMeshes = new ConcurrentLinkedQueue<>();
        swipeMesh1 = new SwipeMesh(mScreenHeight,mSurface);
        swipeMesh2 = new SwipeMesh(mScreenHeight,mSurface);
        mMousePoints = new ConcurrentLinkedQueue<>();
//        mMesh = new Mesh(mContext,bmp);
        // Set the clear color to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
//        GLES20.glEnable(GLES20.GL_ALPHA_TEST);
//        GLES20.glge
        // Create the shaders
        // Set our shader programm
//        GLES20.glUseProgram(CustomShader.sp_SolidColor);
        // Create the shaders, images
        int vertexShader = CustomShader.loadShader(GLES20.GL_VERTEX_SHADER,
                                                   CustomShader.vs_Image);
        int fragmentShader = CustomShader.loadShader(GLES20.GL_FRAGMENT_SHADER,
                                                     CustomShader.fs_Image);
        CustomShader.sp_Image = GLES20.glCreateProgram();
        GLES20.glAttachShader(CustomShader.sp_Image, vertexShader);
        GLES20.glAttachShader(CustomShader.sp_Image, fragmentShader);
        GLES20.glLinkProgram(CustomShader.sp_Image);
        // Set our shader programm
        GLES20.glUseProgram(CustomShader.sp_Image);
    }

    private void initTextures() {
//        textureIDs = new int[1];
//        GLES20.glGenTextures(1, textureIDs, 0);
//        // Retrieve our image from resources.
//        // Bind texture to texturename
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIDs[0]);
//        // Set filtering
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
//                               GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
//                               GLES20.GL_LINEAR);
//        // Set wrapping mode
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
//                               GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
//                               GLES20.GL_CLAMP_TO_EDGE);
//        // Load the bitmap into the bound texture.
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mTexture, 0);
    }

    public void processTouchEvent(MotionEvent event) {
        int pointerIndex;
        if(event.getPointerCount() <= 2) {
            if (event.findPointerIndex(0) != -1) {
                pointerIndex = event.findPointerIndex(0);
                swipeMesh1.addPoint(new Vector(event.getX(pointerIndex), event.getY(pointerIndex)), 1, 0, 0, 1);
            }
            if (event.findPointerIndex(1) != -1) {
                pointerIndex = event.findPointerIndex(1);
                swipeMesh2.addPoint(new Vector(event.getX(pointerIndex), event.getY(pointerIndex)), 1, 0, 0, 1);
            }
        }
    }

    private Vector findPerp(Vector A, Vector B) {
        Vector dir = Vector.sub(B, A);
        Vector nDir = Vector.normalize(dir);
        return new Vector(-1 * nDir.y, nDir.x);
    }


    /*
   Tension: 1 is high, 0 normal, -1 is low
   Bias: 0 is even,
         positive is towards first segment,
         negative towards the other
*/
    float hermiteInterpolate(
            float pointA, float pointB,
            float pointC, float pointD,
            float mu) {
        float m0, m1, mu2, mu3;
        float a0, a1, a2, a3;
        mu2 = mu * mu;
        mu3 = mu2 * mu;
        m0 = (pointB - pointA) * 1 / 2;
        m0 += (pointC - pointB) * 1 / 2;
        m1 = (pointC - pointB) * 1 / 2;
        m1 += (pointD - pointC) * 1 / 2;
        a0 = 2 * mu3 - 3 * mu2 + 1;
        a1 = mu3 - 2 * mu2 + mu;
        a2 = mu3 - mu2;
        a3 = -2 * mu3 + 3 * mu2;
        return (a0 * pointB + a1 * m0 + a2 * m1 + a3 * pointC);
    }

}
