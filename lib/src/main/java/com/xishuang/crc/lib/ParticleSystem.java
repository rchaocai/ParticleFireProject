package com.xishuang.crc.lib;

import android.opengl.GLES30;
import android.support.annotation.NonNull;

import static com.xishuang.crc.lib.CustomGLSurfaceView.eyeX;
import static com.xishuang.crc.lib.CustomGLSurfaceView.eyeY;
import static com.xishuang.crc.lib.CustomGLSurfaceView.eyeZ;
import static com.xishuang.crc.lib.ParticleDataConstant.BLEND_FUNC;
import static com.xishuang.crc.lib.ParticleDataConstant.CURR_INDEX;
import static com.xishuang.crc.lib.ParticleDataConstant.DST_BLEND;
import static com.xishuang.crc.lib.ParticleDataConstant.END_COLOR;
import static com.xishuang.crc.lib.ParticleDataConstant.GROUP_COUNT;
import static com.xishuang.crc.lib.ParticleDataConstant.LIFE_SPAN_STEP;
import static com.xishuang.crc.lib.ParticleDataConstant.MAX_LIFE_SPAN;
import static com.xishuang.crc.lib.ParticleDataConstant.RADIS;
import static com.xishuang.crc.lib.ParticleDataConstant.SRC_BLEND;
import static com.xishuang.crc.lib.ParticleDataConstant.START_COLOR;
import static com.xishuang.crc.lib.ParticleDataConstant.THREAD_SLEEP;
import static com.xishuang.crc.lib.ParticleDataConstant.VY;
import static com.xishuang.crc.lib.ParticleDataConstant.X_RANGE;
import static com.xishuang.crc.lib.ParticleDataConstant.Y_RANGE;
import static com.xishuang.crc.lib.ParticleDataConstant.lock;

/**
 * Author:xishuang
 * Date:2017.10.07
 * Des:火焰粒子系统的总控制类，对所有粒子位置
 * 的计算以及该位置所对应的 6 个顶点坐标值的计算，同时还实现了定时更新粒子位置以及根据摄
 * 像机位置计算火焰朝向
 */
public class ParticleSystem implements Comparable<ParticleSystem> {

    /**
     * 粒子系统的绘制位置x坐标
     */
    private final float positionX;
    /**
     * 粒子系统的绘制位置y坐标
     */
    private final float positionZ;
    /**
     * 粒子起始颜色
     */
    private final float[] startColor;
    /**
     * 粒子终止颜色
     */
    private final float[] endColor;
    /**
     * 源混合因子
     */
    private final int srcBlend;
    /**
     * 目标混合因子
     */
    private final int dstBlend;
    /**
     * 混合方式
     */
    private final int blendFunc;
    /**
     * 每个粒子的最大生命期
     */
    private final float maxLifeSpan;
    /**
     * 每个粒子的生命步进
     */
    private final float lifeSpanStep;
    /**
     * 每批喷发的粒子数
     */
    private final int groupCount;
    /**
     * 线程的休眠时间
     */
    private final int sleepSpan;
    /**
     * 此粒子系统的中心点x坐标
     */
    private final int sx;
    /**
     * 此粒子系统的中心点y坐标
     */
    private final int sy;
    /**
     * 粒子距离中心点x方向的最大距离
     */
    private final float xRange;
    /**
     * 粒子距离中心点y方向的最大距离
     */
    private final float yRange;
    /**
     * 粒子的x方向运动速度
     */
    private final int vx;
    /**
     * 粒子的y方向运动速度
     */
    private final float vy;
    /**
     * 粒子系统的粒子半径
     */
    private final float halfSize;
    /**
     * 此粒子系统的旋转角度
     */
    private float yAngle = 0;
    /**
     * 存放顶点数据的数组-每个粒子对应6个顶点，每个顶点包含4个值
     */
    private final float[] points;
    private ParticleForDraw fpfd;
    /**
     * 线程工作的标志位
     */
    private boolean flag = true;

    public ParticleSystem(float positionx, float positionz, ParticleForDraw particleForDraw, int count) {
        this.positionX = positionx;
        this.positionZ = positionz;
        this.startColor = START_COLOR[CURR_INDEX];
        this.endColor = END_COLOR[CURR_INDEX];
        this.srcBlend = SRC_BLEND[CURR_INDEX];
        this.dstBlend = DST_BLEND[CURR_INDEX];
        this.blendFunc = BLEND_FUNC[CURR_INDEX];
        this.maxLifeSpan = MAX_LIFE_SPAN[CURR_INDEX];
        this.lifeSpanStep = LIFE_SPAN_STEP[CURR_INDEX];
        this.groupCount = GROUP_COUNT[CURR_INDEX];
        this.sleepSpan = THREAD_SLEEP[CURR_INDEX];
        this.sx = 0;
        this.sy = 0;
        this.xRange = X_RANGE[CURR_INDEX];
        this.yRange = Y_RANGE[CURR_INDEX];
        this.vx = 0;
        this.vy = VY[CURR_INDEX];
        this.halfSize = RADIS[CURR_INDEX];
        //初始化粒子群的绘制者
        this.fpfd = particleForDraw;
        //初始化粒子所对应的所有顶点数据数组
        this.points = initPoints(count);
        //调用初始化顶点坐标与纹理坐标数据的方法
        fpfd.initVertexData(points);
        //创建粒子的更新线程
        new Thread() {
            public void run() {
                while (flag) {
                    update();//调用update方法更新粒子状态
                    try {
                        Thread.sleep(sleepSpan);//休眠一定的时间
                    } catch (InterruptedException e) {
                        e.printStackTrace();//打印异常信息
                    }
                }
            }
        }.start();//启动线程
    }

    /**
     * 初始化粒子所对应的所有顶点数据的方法
     *
     * @param zcount 粒子数量
     */
    private float[] initPoints(int zcount)//
    {
        float[] points = new float[zcount * 4 * 6];//临时存放顶点数据的数组-每个粒子对应6个顶点，每个顶点包含4个值
        for (int i = 0; i < zcount; i++) {//循环遍历所有粒子
            //在中心附近产生产生粒子的位置------**/
            float px = (float) (sx + xRange * (Math.random() * 2 - 1.0f));//计算粒子位置的x坐标
            float py = (float) (sy + yRange * (Math.random() * 2 - 1.0f));//计算粒子位置的y坐标
            float vx = (sx - px) / 150;//计算粒子的x方向运动速度

            points[i * 4 * 6] = px - halfSize / 2;//粒子对应的第一个点的x坐标
            points[i * 4 * 6 + 1] = py + halfSize / 2;//粒子对应的第一个点的y坐标
            points[i * 4 * 6 + 2] = vx;//粒子对应的第一个点的x方向运动速度
            points[i * 4 * 6 + 3] = 10.0f;//粒子对应的第一个点的当前生命期--10代表粒子处于未激活状态

            points[i * 4 * 6 + 4] = px - halfSize / 2;
            points[i * 4 * 6 + 5] = py - halfSize / 2;
            points[i * 4 * 6 + 6] = vx;
            points[i * 4 * 6 + 7] = 10.0f;

            points[i * 4 * 6 + 8] = px + halfSize / 2;
            points[i * 4 * 6 + 9] = py + halfSize / 2;
            points[i * 4 * 6 + 10] = vx;
            points[i * 4 * 6 + 11] = 10.0f;

            points[i * 4 * 6 + 12] = px + halfSize / 2;
            points[i * 4 * 6 + 13] = py + halfSize / 2;
            points[i * 4 * 6 + 14] = vx;
            points[i * 4 * 6 + 15] = 10.0f;

            points[i * 4 * 6 + 16] = px - halfSize / 2;
            points[i * 4 * 6 + 17] = py - halfSize / 2;
            points[i * 4 * 6 + 18] = vx;
            points[i * 4 * 6 + 19] = 10.0f;

            points[i * 4 * 6 + 20] = px + halfSize / 2;
            points[i * 4 * 6 + 21] = py - halfSize / 2;
            points[i * 4 * 6 + 22] = vx;
            points[i * 4 * 6 + 23] = 10.0f;
        }
        for (int j = 0; j < groupCount; j++) {//循环遍历第一批的粒子
            points[4 * j * 6 + 3] = lifeSpanStep;//设置粒子生命期，不为10时，表示粒子处于活跃状态
            points[4 * j * 6 + 7] = lifeSpanStep;//设置粒子生命期，不为10时，表示粒子处于活跃状态
            points[4 * j * 6 + 11] = lifeSpanStep;//设置粒子生命期，不为10时，表示粒子处于活跃状态
            points[4 * j * 6 + 15] = lifeSpanStep;//设置粒子生命期，不为10时，表示粒子处于活跃状态
            points[4 * j * 6 + 19] = lifeSpanStep;//设置粒子生命期，不为10时，表示粒子处于活跃状态
            points[4 * j * 6 + 23] = lifeSpanStep;//设置粒子生命期，不为10时，表示粒子处于活跃状态

        }
        return points;//返回所有粒子顶点属性数据数组
    }

    private int count = 1;//激活粒子的位置计算器

    /**
     * 更新粒子状态
     */
    public void update() {
        //计算器超过激活粒子位置时重新计数
        if (count >= (points.length / groupCount / 4 / 6)) {
            count = 0;
        }
        //查看生命期以及计算下一位置的相应数据
        for (int i = 0; i < points.length / 4 / 6; i++) {//循环遍历所有粒子
            //当前为活跃粒子时
            if (points[i * 4 * 6 + 3] != 10.0f) {
                points[i * 4 * 6 + 3] += lifeSpanStep;//计算当前生命期
                points[i * 4 * 6 + 7] += lifeSpanStep;//计算当前生命期
                points[i * 4 * 6 + 11] += lifeSpanStep;//计算当前生命期
                points[i * 4 * 6 + 15] += lifeSpanStep;//计算当前生命期
                points[i * 4 * 6 + 19] += lifeSpanStep;//计算当前生命期
                points[i * 4 * 6 + 23] += lifeSpanStep;//计算当前生命期
                //当前生命期大于最大生命期时---重新设置该粒子参数
                if (points[i * 4 * 6 + 3] > this.maxLifeSpan) {
                    float px = (float) (sx + xRange * (Math.random() * 2 - 1.0f));//计算粒子位置x坐标
                    float py = (float) (sy + yRange * (Math.random() * 2 - 1.0f));//计算粒子位置y坐标
                    float vx = (sx - px) / 150;//计算粒子x方向的速度

                    points[i * 4 * 6] = px - halfSize / 2;//粒子对应的第一个顶点的x坐标
                    points[i * 4 * 6 + 1] = py + halfSize / 2;//粒子对应的第一个顶点的y坐标
                    points[i * 4 * 6 + 2] = vx;//粒子对应的第一个顶点的x方向运动速度
                    points[i * 4 * 6 + 3] = 10.0f;//粒子对应的第一个顶点的当前生命期--10代表粒子处于未激活状态

                    points[i * 4 * 6 + 4] = px - halfSize / 2;
                    points[i * 4 * 6 + 5] = py - halfSize / 2;
                    points[i * 4 * 6 + 6] = vx;
                    points[i * 4 * 6 + 7] = 10.0f;

                    points[i * 4 * 6 + 8] = px + halfSize / 2;
                    points[i * 4 * 6 + 9] = py + halfSize / 2;
                    points[i * 4 * 6 + 10] = vx;
                    points[i * 4 * 6 + 11] = 10.0f;

                    points[i * 4 * 6 + 12] = px + halfSize / 2;
                    points[i * 4 * 6 + 13] = py + halfSize / 2;
                    points[i * 4 * 6 + 14] = vx;
                    points[i * 4 * 6 + 15] = 10.0f;

                    points[i * 4 * 6 + 16] = px - halfSize / 2;
                    points[i * 4 * 6 + 17] = py - halfSize / 2;
                    points[i * 4 * 6 + 18] = vx;
                    points[i * 4 * 6 + 19] = 10.0f;

                    points[i * 4 * 6 + 20] = px + halfSize / 2;
                    points[i * 4 * 6 + 21] = py - halfSize / 2;
                    points[i * 4 * 6 + 22] = vx;
                    points[i * 4 * 6 + 23] = 10.0f;
                } else { //生命期小于最大生命期时----计算粒子的下一位置坐标
                    points[i * 4 * 6] += points[i * 4 * 6 + 2];//计算粒子对应的第一个顶点的x坐标
                    points[i * 4 * 6 + 1] += vy;//计算粒子对应的第一个顶点的y坐标

                    points[i * 4 * 6 + 4] += points[i * 4 * 6 + 6];
                    points[i * 4 * 6 + 5] += vy;

                    points[i * 4 * 6 + 8] += points[i * 4 * 6 + 10];
                    points[i * 4 * 6 + 9] += vy;

                    points[i * 4 * 6 + 12] += points[i * 4 * 6 + 14];
                    points[i * 4 * 6 + 13] += vy;

                    points[i * 4 * 6 + 16] += points[i * 4 * 6 + 18];
                    points[i * 4 * 6 + 17] += vy;

                    points[i * 4 * 6 + 20] += points[i * 4 * 6 + 22];
                    points[i * 4 * 6 + 21] += vy;
                }
            }
        }
        //循环发射一批激活计数器所指定位置的粒子
        for (int i = 0; i < groupCount; i++) {
            //如果粒子处于未激活状态时
            if (points[groupCount * count * 4 * 6 + 4 * i * 6 + 3] == 10.0f) {
                points[groupCount * count * 4 * 6 + 4 * i * 6 + 3] = lifeSpanStep;//激活粒子--设置粒子当前的生命周期
                points[groupCount * count * 4 * 6 + 4 * i * 6 + 7] = lifeSpanStep;//激活粒子--设置粒子当前的生命周期
                points[groupCount * count * 4 * 6 + 4 * i * 6 + 11] = lifeSpanStep;//激活粒子--设置粒子当前的生命周期
                points[groupCount * count * 4 * 6 + 4 * i * 6 + 15] = lifeSpanStep;//激活粒子--设置粒子当前的生命周期
                points[groupCount * count * 4 * 6 + 4 * i * 6 + 19] = lifeSpanStep;//激活粒子--设置粒子当前的生命周期
                points[groupCount * count * 4 * 6 + 4 * i * 6 + 23] = lifeSpanStep;//激活粒子--设置粒子当前的生命周期
            }
        }
        synchronized (lock) {//加锁--防止在更新顶点坐标数据时，将顶点坐标数据送入渲染管线
            fpfd.updatVertexData(points);//更新顶点坐标数据缓冲的方法
        }
        count++;//下次激活粒子的位置
    }

    /**
     * 绘制此粒子系统中所有粒子
     *
     * @param texId 火焰的纹理id
     */
    public void drawSelf(int texId) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);//关闭深度检测
        GLES30.glEnable(GLES30.GL_BLEND); //开启混合
        GLES30.glBlendEquation(blendFunc);//设置混合方式
        GLES30.glBlendFunc(srcBlend, dstBlend); //设置混合因子

        MatrixState.translate(positionX, 1, positionZ);//执行平移变换
        MatrixState.rotate(yAngle, 0, 1, 0);//执行旋转变换

        MatrixState.pushMatrix();//保护现场
        fpfd.drawSelf(texId, startColor, endColor, maxLifeSpan);//绘制粒子群
        MatrixState.popMatrix();//恢复现场

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);//开启深度检测
        GLES30.glDisable(GLES30.GL_BLEND);//关闭混合
    }

    /**
     * 根据摄像机位置计算火焰朝向
     */
    void calculateBillboardDirection() {
        float xspan = positionX - eyeX;
        float zspan = positionZ - eyeZ;

        if (zspan <= 0) {
            yAngle = (float) Math.toDegrees(Math.atan(xspan / zspan));
        } else {
            yAngle = 180 + (float) Math.toDegrees(Math.atan(xspan / zspan));
        }
    }

    @Override
    public int compareTo(@NonNull ParticleSystem another) {
        //重写的比较两个火焰离摄像机距离的方法
        float xs = positionX - eyeX;
        float zs = positionZ - eyeY;

        float xo = another.positionX - eyeX;
        float zo = another.positionZ - eyeY;

        float disA = xs * xs + zs * zs;
        float disB = xo * xo + zo * zo;
        return ((disA - disB) == 0) ? 0 : ((disA - disB) > 0) ? -1 : 1;
    }
}
