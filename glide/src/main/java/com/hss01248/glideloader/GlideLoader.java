package com.hss01248.glideloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.renderscript.RSRuntimeException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.github.piasy.biv.BigImageViewer;
import com.github.piasy.biv.view.BigImageView;
import com.hss01248.glideloader.big.GlideBigLoader;
import com.hss01248.image.ImageLoader;
import com.hss01248.image.MyUtil;
import com.hss01248.image.config.GlobalConfig;
import com.hss01248.image.config.ScaleMode;
import com.hss01248.image.config.ShapeMode;
import com.hss01248.image.config.SingleConfig;
import com.hss01248.image.interfaces.FileGetter;
import com.hss01248.image.interfaces.ILoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import jp.wasabeef.glide.transformations.internal.FastBlur;
import jp.wasabeef.glide.transformations.internal.RSBlur;

/**
 * Created by Administrator on 2017/3/27 0027.
 * 参考:
 * https://mrfu.me/2016/02/28/Glide_Series_Roundup/
 */

public class GlideLoader implements ILoader {
    @Override
    public void init(Context context, int cacheSizeInM) {//glide默认最大容量250MB的文件缓存
        Glide.get(context).setMemoryCategory(MemoryCategory.NORMAL);
        BigImageViewer.initialize(GlideBigLoader.with(context,MyUtil.getClient(GlobalConfig.ignoreCertificateVerify)));
    }

    @Override
    public void request(final SingleConfig config) {
        if(config.isAsBitmap()){
            SimpleTarget target = new SimpleTarget<Bitmap>(config.getWidth(),config.getHeight()) {
                @Override
                public void onResourceReady(Bitmap bitmap, GlideAnimation glideAnimation) {
                    // do something with the bitmap
                    // for demonstration purposes, let's just set it to an ImageView
                   // BitmapPool mBitmapPool = Glide.get(BigLoader.context).getBitmapPool();
                    //bitmap = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight())
                    if(config.isNeedBlur()){
                        bitmap = blur(bitmap,config.getBlurRadius(),true);
                    }
                    if(config.getShapeMode() == ShapeMode.OVAL){
                        bitmap = MyUtil.cropCirle(bitmap,true);
                    }else if(config.getShapeMode() == ShapeMode.RECT_ROUND){
                        bitmap = MyUtil.rectRound(bitmap,config.getRectRoundRadius(),0);
                    }

                    config.getBitmapListener().onSuccess(bitmap);
                }

                @Override
                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                    super.onLoadFailed(e, errorDrawable);
                    config.getBitmapListener().onFail();
                }
            };
            RequestManager requestManager =  Glide.with(config.getContext());
            DrawableTypeRequest request = getDrawableTypeRequest(config, requestManager);
            request.override(config.getWidth(),config.getHeight());
           // setShapeModeAndBlur(config, request);
            request.asBitmap().into(target);

        }else {

            if(config.getTarget() instanceof BigImageView){
                MyUtil.viewBigImage(config);
                return;
            }


            RequestManager requestManager =  Glide.with(config.getContext());
            DrawableTypeRequest request = getDrawableTypeRequest(config, requestManager);

            if(request ==null){
                return;
            }
            if(MyUtil.shouldSetPlaceHolder(config)){
                request.placeholder(config.getPlaceHolderResId());
            }

            int scaleMode = config.getScaleMode();
            switch (scaleMode){
                case ScaleMode.CENTER_CROP:
                    request.centerCrop();
                    break;
                case ScaleMode.CENTER_INSIDE:
                    request.centerCrop();
                    break;
                case ScaleMode.FIT_CENTER:
                    request.fitCenter();
                    break;
                case ScaleMode.FIT_XY:
                    request.centerCrop();
                    break;
                case ScaleMode.FIT_END:
                    request.centerCrop();
                    break;
                case ScaleMode.FOCUS_CROP:
                    request.centerCrop();
                    break;
                case ScaleMode.CENTER:
                    request.centerCrop();
                    break;
                case ScaleMode.FIT_START:
                    request.centerCrop();
                    break;

                default:
                    request.centerCrop();
                    break;
            }
            request.override(config.getWidth(),config.getHeight());

            setShapeModeAndBlur(config, request);


                    //.thumbnail( 0.1f );


            if(config.getErrorResId() >0){
                request.error(config.getErrorResId());
            }

            if(config.getTarget() instanceof ImageView){
                request.into((ImageView) config.getTarget());
            }


        }


    }

    @Nullable
    private DrawableTypeRequest getDrawableTypeRequest(SingleConfig config, RequestManager requestManager) {
        DrawableTypeRequest request = null;
        if(!TextUtils.isEmpty(config.getUrl())){
            request= requestManager.load(MyUtil.appendUrl(config.getUrl()));
            //request.diskCacheStrategy(DiskCacheStrategy.SOURCE);//只缓存原图
        }else if(!TextUtils.isEmpty(config.getFilePath())){
            request= requestManager.load(config.getFilePath());
        }else if(!TextUtils.isEmpty(config.getContentProvider())){
            request= requestManager.loadFromMediaStore(Uri.parse(config.getContentProvider()));
        }else if(config.getResId()>0){
            request= requestManager.load(config.getResId());
        }
        return request;
    }

    private void setShapeModeAndBlur(SingleConfig config, DrawableTypeRequest request) {
        int shapeMode = config.getShapeMode();
        List<Transformation> transformations = new ArrayList<>();

        if(config.isCropFace()){
            // transformations.add(new FaceCenterCrop());//脸部识别
        }

        if(config.isNeedBlur()){
            transformations.add(new BlurTransformation(config.getContext(), config.getBlurRadius()));
        }


        switch (shapeMode){
            case ShapeMode.RECT:

                if(config.getBorderWidth()>0){

                }
                break;
            case ShapeMode.RECT_ROUND:
                transformations.add(new RoundedCornersTransformation(config.getContext(),
                        config.getRectRoundRadius(), 0, RoundedCornersTransformation.CornerType.ALL));

                if(config.getBorderWidth()>0){

                }
                if(config.isGif() && config.getRoundOverlayColor()>0){

                }
                break;
            case ShapeMode.OVAL:
                transformations.add( new CropCircleTransformation(config.getContext()));
                if(config.getBorderWidth()>0){

                }
                if(config.isGif() && config.getRoundOverlayColor()>0){

                }
                break;
        }

        if(transformations.size()>0){
            Transformation[] forms = new Transformation[transformations.size()];
            for (int i = 0; i < transformations.size(); i++) {
                forms[i] = transformations.get(i);
            }
            request.bitmapTransform(forms);
        }



    }

    @Override
    public void pause() {
        Glide.with(GlobalConfig.context).pauseRequestsRecursive();

    }

    @Override
    public void resume() {
        Glide.with(GlobalConfig.context).resumeRequestsRecursive();
    }

    @Override
    public void clearDiskCache() {
        Glide.get(ImageLoader.context).clearDiskCache();

       /* File dir = new File(BigLoader.context.getCacheDir(), DiskCache.Factory.DEFAULT_DISK_CACHE_DIR);
        if(dir!=null && dir.exists()){
            MyUtil.deleteFolderFile(dir.getAbsolutePath(),false);
        }*/
    }

    @Override
    public void clearMomoryCache() {
        Glide.get(ImageLoader.context).clearMemory();
    }

    @Override
    public long getCacheSize() {
        return MyUtil.getCacheSize();

    }

    @Override
    public void clearCacheByUrl(String url) {

    }

    @Override
    public void clearMomoryCache(View view) {
        Glide.clear(view);
    }

    @Override
    public void clearMomoryCache(String url) {

    }

    /**
     * glide中只能异步,可以用CacheHitEvent+ url去接收
     * @param url
     * @return
     */
    @Override
    public File getFileFromDiskCache(final String url) {
        return null;
    }

    @Override
    public void getFileFromDiskCache(String url, final FileGetter getter) {
        Glide.with(ImageLoader.context)
                .load(url)
                .downloadOnly(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                        if(resource.exists() && resource.isFile() ){//&& resource.length() > 70
                            getter.onSuccess(resource);
                        }else {
                            getter.onFail();
                        }
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        getter.onFail();
                    }
                });
    }

    /**
     * 无法同步判断
     * 参见:https://github.com/bumptech/glide/issues/639
     * @param url
     * @return
     */
    @Override
    public boolean isCached(String url) {
        return false;
    }

    @Override
    public void trimMemory(int level) {
        Glide.with(GlobalConfig.context).onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        Glide.with(GlobalConfig.context).onLowMemory();
    }



    public static Bitmap blur(Bitmap source,int mRadius,boolean recycleOriginal){
        int mSampling = 1;
        int width = source.getWidth();
        int height = source.getHeight();
        int scaledWidth = width / mSampling;
        int scaledHeight = height / mSampling;
        Bitmap bitmap
         = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(1 / (float) mSampling, 1 / (float) mSampling);
            Paint paint = new Paint();
            paint.setFlags(Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(source, 0, 0, paint);






        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                bitmap = RSBlur.blur(ImageLoader.context, bitmap, mRadius);
            } catch (RSRuntimeException e) {
                bitmap = FastBlur.blur(bitmap, mRadius, true);
            }
        } else {
            bitmap = FastBlur.blur(bitmap, mRadius, true);
        }
        if(recycleOriginal){
            //source.recycle();
        }

        return bitmap;
    }






}
