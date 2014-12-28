package com.fedorvlasov.lazylist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.ImageView;

import com.ternaryop.photoshelf.R;
import com.ternaryop.utils.ImageUtils;
import com.ternaryop.utils.URLUtils;

public class ImageLoader {
    public final static String IMAGE_PREFIX_DIRECTORY = "images" + File.separator;
    
    private final MemoryCache memoryCache = new MemoryCache();
    private final FileCache fileCache;
    private final Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private final ExecutorService executorService;
    private final Handler handler = new Handler(); // handler to display images in UI thread
    private final int stub_id = R.drawable.stub;
    private final Context context;

    public ImageLoader(Context context, String prefix) {
        this.context = context;
        this.fileCache = new FileCache(context, IMAGE_PREFIX_DIRECTORY + prefix);            
        executorService = Executors.newFixedThreadPool(5);
    }
    
    public void displayImage(String url, ImageView imageView) {
        displayImage(url, imageView, false);
    }

    public void displayImage(String url, ImageView imageView, boolean scaleForDefaultDisplay) {
        imageViews.put(imageView, url);
        Bitmap bitmap = memoryCache.get(url);
        if (bitmap != null) {
            if (scaleForDefaultDisplay) {
                bitmap = ImageUtils.scaleBitmapForDefaultDisplay(context, bitmap);
            }
            imageView.setImageBitmap(bitmap);
        } else {
            queuePhoto(url, imageView, scaleForDefaultDisplay);
            imageView.setImageResource(stub_id);
        }
    }
    
    public void displayDrawable(final String url, final ImageLoaderCallback callback) {
        displayDrawable(url, callback, true);
    }
    
    public void displayDrawable(final String url, final ImageLoaderCallback callback, final boolean scaleForDefaultDisplay) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                final Drawable icon = drawableFromUrl(url, scaleForDefaultDisplay);
                if (icon != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.display(icon);
                        }
                    });
                }
            }
        });
    }
    
    public void displayIcon(final String url, final MenuItem menuItem) {
        displayDrawable(url, new ImageLoaderCallback() {
            @Override
            public void display(Drawable drawable) {
                menuItem.setIcon(drawable);
            }
        });
    }
        
    private void queuePhoto(String url, ImageView imageView, boolean scaleForDefaultDisplay)
    {
        PhotoToLoad p=new PhotoToLoad(url, imageView);
        executorService.submit(new PhotosLoader(p, scaleForDefaultDisplay));
    }
    
    private Bitmap getBitmap(String url)  {
        File f = fileCache.getFile(url);
        
        //from SD cache
        Bitmap b = decodeFile(f);
        if (b != null) {
            return b;
        }

        //from web
        try {
            HttpURLConnection conn = URLUtils.openConnectionFollowingDifferentProtocols(url);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.connect();
            InputStream is = conn.getInputStream();
            OutputStream os = new FileOutputStream(f);
            Utils.CopyStream(is, os);
            os.close();
            conn.disconnect();
            return decodeFile(f);
        } catch (Throwable ex){
           ex.printStackTrace();
           if(ex instanceof OutOfMemoryError)
               memoryCache.clear();
           return null;
        }
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream stream1=new FileInputStream(f);
            BitmapFactory.decodeStream(stream1,null,o);
            stream1.close();
            
            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE=70;
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }
            
            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            FileInputStream stream2=new FileInputStream(f);
            Bitmap bitmap=BitmapFactory.decodeStream(stream2, null, o2);
            stream2.close();
            return bitmap;
        } catch (FileNotFoundException ignored) {
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    //Task for the queue
    private class PhotoToLoad
    {
        public final String url;
        public final ImageView imageView;
        public PhotoToLoad(String u, ImageView i){
            url=u; 
            imageView=i;
        }
    }
    
    class PhotosLoader implements Runnable {
        final PhotoToLoad photoToLoad;
        private final boolean scaleForDefaultDisplay;
        PhotosLoader(PhotoToLoad photoToLoad, boolean scaleForDefaultDisplay) {
            this.photoToLoad = photoToLoad;
            this.scaleForDefaultDisplay = scaleForDefaultDisplay;
        }
        
        @Override
        public void run() {
            try {
                if (imageViewReused(photoToLoad)) {
                    return;
                }
                Bitmap bmp = getBitmap(photoToLoad.url);
                memoryCache.put(photoToLoad.url, bmp);
                if (imageViewReused(photoToLoad)) {
                    return;
                }
                BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad, scaleForDefaultDisplay);
                handler.post(bd);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }
    
    boolean imageViewReused(PhotoToLoad photoToLoad){
        String tag=imageViews.get(photoToLoad.imageView);
        return tag == null || !tag.equals(photoToLoad.url);
    }
    
    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable {
        Bitmap bitmap;
        final PhotoToLoad photoToLoad;
        private final boolean scaleForDefaultDisplay;

        public BitmapDisplayer(Bitmap b, PhotoToLoad p, boolean scaleForDefaultDisplay) {
            bitmap = b;
            photoToLoad = p;
            this.scaleForDefaultDisplay = scaleForDefaultDisplay;
        }

        public void run()
        {
            if (imageViewReused(photoToLoad)) {
                return;
            }
            if (bitmap != null) {
                if (scaleForDefaultDisplay) {
                    bitmap = ImageUtils.scaleBitmapForDefaultDisplay(context, bitmap);
                }
                photoToLoad.imageView.setImageBitmap(bitmap);
            } else {
                photoToLoad.imageView.setImageResource(stub_id);
            }
        }
    }

    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
    }

    public static void clearImageCache(Context context) {
        FileCache.clearCache(context, IMAGE_PREFIX_DIRECTORY);
    }
    
    protected Drawable drawableFromUrl(String url, boolean scaleForDefaultDisplay) {
        HttpURLConnection conn = null;
        OutputStream os = null;
        try {
            File f = fileCache.getFile(url);
            Bitmap bitmap = Utils.decodeFile(f);

            if (bitmap == null) {
                conn = URLUtils.openConnectionFollowingDifferentProtocols(url);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.connect();
                os = new FileOutputStream(f);
                Utils.CopyStream(conn.getInputStream(), os);
                
                bitmap = Utils.decodeFile(f);
            }
            if (scaleForDefaultDisplay) {
                bitmap = ImageUtils.scaleBitmapForDefaultDisplay(context, bitmap);
            }
            return new BitmapDrawable(null, bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (os != null) os.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
        }
        return null;
    }

    public interface ImageLoaderCallback {
        public void display(Drawable drawable);
    }
}
