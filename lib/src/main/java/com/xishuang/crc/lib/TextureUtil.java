package com.xishuang.crc.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Author:xishuang
 * Date:2017.10.06
 * Des:纹理加载工具类
 */
public class TextureUtil {
    /**
     * 加载对于图片的纹理
     *
     * @param res_id 图片资源id、
     * @return 返回纹理对象
     */
    public static int loadTexture(Context context, int res_id) {
        //生成纹理id
        int[] textures = new int[1];
        //生成纹理id数量，回调的纹理，偏移量
        GLES30.glGenTextures(1, textures, 0);
        int texture_id = textures[0];
        //纹理配置
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture_id);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        //加载资源图片
        InputStream is = context.getResources().openRawResource(res_id);
        Bitmap bitmapTmp;
        try {
            bitmapTmp = BitmapFactory.decodeStream(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //纹理加载
        GLUtils.texImage2D(
                GLES30.GL_TEXTURE_2D, //纹理类型，在OpenGL ES中必须为GL_TEXTURE_2D
                0,                    //纹理的层次，0表示基本图像层，可以理解为直接贴图
                bitmapTmp,            //纹理图像
                0);                   //纹理边框尺寸

        //纹理加载成功后释放图片
        bitmapTmp.recycle();
        return texture_id;
    }
}
