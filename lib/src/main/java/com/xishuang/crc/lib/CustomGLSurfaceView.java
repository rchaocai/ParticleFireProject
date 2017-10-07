package com.xishuang.crc.lib;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.xishuang.crc.lib.ParticleDataConstant.CURR_INDEX;
import static com.xishuang.crc.lib.ParticleDataConstant.RADIS;

/**
 * Author:xishuang
 * Date:2017.10.06
 * Des:自定义的CustomGLSurfaceView，触摸事件处理
 */
public class CustomGLSurfaceView extends GLSurfaceView {
    /**
     * 渲染器
     */
    private final SceneRender mSceneRender;
    List<ParticleSystem> fps = new ArrayList<>();
    private ParticleForDraw[] fpfd;
    /**
     * 循环控制标志位
     */
    private boolean flag = true;
    private float mOffset = 20;
    private int screen_width;
    private int screen_height;
    /**
     * 摄像机每次转动的角度
     */
    static final float DEGREE_SPAN = (float) (3.0 / 180.0f * Math.PI);
    /**
     * 视线的方向
     */
    static float mDirection = 0;
    /**
     * 摄像机x坐标
     */
    static float eyeX = 0;
    /**
     * 摄像机y坐标
     */
    static float eyeY = 18;
    /**
     * 摄像机z坐标
     */
    static float eyeZ = 20;
    /**
     * 观察目标点x坐标
     */
    static float centerX = 0;
    /**
     * 观察目标点y坐标
     */
    static float centerY = 5;
    /**
     * 观察目标点z坐标
     */
    static float centerZ = 0;
    /**
     * 摄像机UP向量X分量
     */
    static float upX = 0;
    /**
     * 摄像机UP向量Y分量
     */
    static float upY = Math.abs((eyeX * centerX + eyeZ * centerZ - eyeX * eyeX - eyeZ * eyeZ) / (centerY - eyeY));
    /**
     * 摄像机UP向量Z分量
     */
    static float upZ = 0;
    private int count;
    private Context mContext;
    /**
     * 火焰的对应纹理ID
     */
    private int textureIdFire;

    public CustomGLSurfaceView(Context context, int screen_width, int screen_height) {
        super(context);
        mContext = context;
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        //使用OpenGL ES3.0版本
        setEGLContextClientVersion(3);
        //创建场景渲染器
        mSceneRender = new SceneRender();
        //设置对于渲染器
        setRenderer(mSceneRender);
        //设置渲染模式为主动渲染
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float currentX = event.getX();
        final float currentY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                flag = true;
                new Thread() {
                    @Override
                    public void run() {
                        while (flag) {
                            if (currentX > screen_width / 4 && currentX < 3 * screen_width / 4 && currentY > 0 && currentY < screen_height / 2) {    //向前
                                if (Math.abs(mOffset - 0.5f) > 25 || Math.abs(mOffset - 0.5f) < 15) {
                                    return;
                                }
                                mOffset = mOffset - 0.5f;
                            } else if (currentX > screen_width / 4 && currentX < 3 * screen_width / 4 && currentY > screen_height / 2 && currentY < screen_height) {    //向后
                                if (Math.abs(mOffset + 0.5f) > 25 || Math.abs(mOffset + 0.5f) < 15) {
                                    return;
                                }
                                mOffset = mOffset + 0.5f;
                            } else if (currentX < screen_width / 4) {
                                //顺时针旋转
                                mDirection = mDirection - DEGREE_SPAN;
                            } else if (currentX > screen_width / 4) {
                                //逆时针旋转
                                mDirection = mDirection + DEGREE_SPAN;
                            }
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
                break;
            case MotionEvent.ACTION_UP:
                flag = false;
                break;

        }
        //设置新的摄像机XZ坐标
        eyeX = (float) (Math.sin(mDirection) * mOffset);//摄像机x坐标
        eyeZ = (float) (Math.cos(mDirection) * mOffset);//摄像机z坐标
        //重新计算Up向量
        upX = -eyeX;//摄像机UP向量x坐标
        upY = Math.abs((eyeX * centerX + eyeZ * centerZ - eyeX * eyeX - eyeZ * eyeZ) / (centerY - eyeY));//摄像机UP向量y坐标
        upZ = -eyeZ;//摄像机UP向量z坐标
        //计算粒子的朝向
        for (int i = 0; i < count; i++) {
            fps.get(i).calculateBillboardDirection();
        }
        //根据粒子与摄像机的距离进行排序
        Collections.sort(fps);
        //重新设置摄像机的位置
        MatrixState.setCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        return true;
    }

    private class SceneRender implements GLSurfaceView.Renderer {
        int countt = 0;//计算帧速率的时间间隔次数--计算器
        long timeStart = System.nanoTime();//开始时间

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            count = ParticleDataConstant.START_COLOR.length;
            fpfd = new ParticleForDraw[count];//4组绘制着，4种颜色
            //创建粒子系统
            for (int i = 0; i < count; i++) {
                CURR_INDEX = i;
                fpfd[i] = new ParticleForDraw(CustomGLSurfaceView.this, RADIS[CURR_INDEX]);
                //创建对象,将火焰的初始位置传给构造器
                fps.add(new ParticleSystem(ParticleDataConstant.positionFireXZ[i][0],
                        ParticleDataConstant.positionFireXZ[i][1], fpfd[i], ParticleDataConstant.COUNT[i]));
            }
            //设置屏幕背景色RGBA
            GLES30.glClearColor(0.6f, 0.3f, 0.0f, 1.0f);
            //打开深度检测
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            //初始化纹理
            textureIdFire = TextureUtil.loadTexture(mContext, R.drawable.fire);
            //关闭背面剪裁
            GLES30.glDisable(GLES30.GL_CULL_FACE);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            //设置视窗大小及位置
            GLES30.glViewport(0, 0, width, height);
            //计算GLSurfaceView的宽高比
            float ratio = (float) width / height;
            //调用此方法计算产生透视投影矩阵
            MatrixState.setProjectFrustum(-0.3f * ratio, 0.3f * ratio, -1 * 0.3f, 1 * 0.3f, 1, 100);
            //调用此方法产生摄像机9参数位置矩阵
            MatrixState.setCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
            //初始化变换矩阵
            MatrixState.setInitStack();
            //初始化光源位置
            MatrixState.setLightLocation(0, 15, 0);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            //每十次一计算帧速率
            if (countt == 19) {
                //结束时间
                long timeEnd = System.nanoTime();
                //计算帧速率
                float ps = (float) (1000000000.0 / ((timeEnd - timeStart) / 20));
                System.out.println("pss=" + ps);
                countt = 0;//计算器置0
                timeStart = timeEnd;//起始时间置为结束时间
            }
            //清除深度缓冲与颜色缓冲
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
            MatrixState.pushMatrix();
//            MatrixState.translate(0, 2.5f, 0);
//            for (int i = 0; i < count; i++) {
//                MatrixState.pushMatrix();
//                MatrixState.translate(ParticleDataConstant.positionBrazierXZ[i][0], -2f, ParticleDataConstant.positionBrazierXZ[i][1]);
//                //若加载的物体部位空则绘制物体
//                if (brazier != null) {
//                    brazier.drawSelf(textureIdbrazier);
//                }
//                MatrixState.popMatrix();
//            }
            MatrixState.translate(0, 0.65f, 0);
            for (int i = 0; i < count; i++) {
                MatrixState.pushMatrix();
                fps.get(i).drawSelf(textureIdFire);
                MatrixState.popMatrix();
            }
            MatrixState.popMatrix();
        }
    }
}
