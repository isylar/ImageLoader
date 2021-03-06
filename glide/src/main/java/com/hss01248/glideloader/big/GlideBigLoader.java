/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hss01248.glideloader.big;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.github.piasy.biv.event.CacheHitEvent;
import com.github.piasy.biv.event.ErrorEvent;
import com.github.piasy.biv.loader.BigLoader;
import com.github.piasy.biv.progress.ProgressInterceptor;
import com.github.piasy.biv.view.BigImageView;
import com.hss01248.glideloader.R;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.InputStream;

import okhttp3.OkHttpClient;

/**
 * Created by Piasy{github.com/Piasy} on 09/11/2016.
 */

public final class GlideBigLoader implements BigLoader {
    private final RequestManager mRequestManager;
    //private Observable observable;

    private GlideBigLoader(Context context, OkHttpClient okHttpClient) {
        //observable = new Observable();
        //GlideProgressSupport.init(Glide.get(context), okHttpClient);
        OkHttpClient client=  okHttpClient.newBuilder().addNetworkInterceptor(new ProgressInterceptor()).build();
        Glide.get(context).register(GlideUrl.class, InputStream.class,
                new OkHttpUrlLoader.Factory(client));
        mRequestManager = Glide.with(context);
    }

    public static GlideBigLoader with(Context context) {
        return with(context, null);
    }

    public static GlideBigLoader with(Context context, OkHttpClient okHttpClient) {
        return new GlideBigLoader(context, okHttpClient);
    }

    @Override
    public void loadImage(final Uri uri) {
        mRequestManager
                .load(uri)
                //.asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).dontAnimate()
                .downloadOnly(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                        if(resource.exists() && resource.isFile() && resource.length() > 100){
                            Log.e("onResourceReady","onResourceReady  --"+ resource.getAbsolutePath());
                            EventBus.getDefault().post(new CacheHitEvent(resource,uri.toString()));
                        }else {
                            Log.e("onloadfailed","onLoadFailed  --"+ uri.toString());
                            EventBus.getDefault().post(new ErrorEvent(uri.toString()));
                        }

                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        super.onLoadCleared(placeholder);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        Log.e("onloadfailed","onLoadFailed  --"+ uri.toString());
                        if(e!=null)
                        e.printStackTrace();
                        EventBus.getDefault().post(new ErrorEvent(uri.toString()));
                    }

                    /**
                     * 如果资源已经在内存中，则onLoadStarted就不会被调用
                     * @param placeholder
                     */
                    @Override
                    public void onLoadStarted(Drawable placeholder) {
                        super.onLoadStarted(placeholder);
                        //EventBus.getDefault().post(new StartEvent(uri.toString()));
                    }

                    @Override
                    public void onStart() {
                        super.onStart();
                    }

                    @Override
                    public void onStop() {
                        super.onStop();
                    }
                });


        /*new ImageDownloadTarget(uri.toString()) {
                    @Override
                    public void onResourceReady(File image,
                            GlideAnimation<? super File> glideAnimation) {
                        // we don't need delete this image file, so it behaves live cache hit
                        callback.onCacheHit(image);
                    }



                    @Override
                    public void onDownloadStart() {
                        callback.onStart();
                    }

                    @Override
                    public void onProgress(int progress) {
                        callback.onProgress(progress);
                    }

                    @Override
                    public void onDownloadFinish() {
                        callback.onFinish();
                    }
                }*/
    }

    @Override
    public View showThumbnail(BigImageView parent, Uri thumbnail, int scaleType) {
        ImageView thumbnailView = (ImageView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ui_glide_thumbnail, parent, false);
        switch (scaleType) {
            case BigImageView.INIT_SCALE_TYPE_CENTER_CROP:
                thumbnailView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case BigImageView.INIT_SCALE_TYPE_CENTER_INSIDE:
                thumbnailView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            default:
                break;
        }
        mRequestManager
                .load(thumbnail)
                .into(thumbnailView);
        return thumbnailView;
    }

    @Override
    public void prefetch(Uri uri) {
        mRequestManager
                .load(uri)
                .downloadOnly(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(File resource,
                            GlideAnimation<? super File> glideAnimation) {
                        // not interested in result
                    }
                });
    }
}
