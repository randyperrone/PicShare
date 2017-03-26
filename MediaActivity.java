package com.example.randy.picshare.Activities;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.randy.picshare.Manifest;
import com.example.randy.picshare.Model.ImageGetterFromDevice;
import com.example.randy.picshare.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MediaActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };



    final int PERMISSION_READ_EXTERNAL = 111;
    private ArrayList<ImageGetterFromDevice> imageList = new ArrayList<>();
    private ImageView selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_media);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });


        selectedImage = (ImageView)findViewById(R.id.selected_image);
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.content_images);
        ImageAdapter adapter = new ImageAdapter(imageList);
        recyclerView.setAdapter(adapter);
        GridLayoutManager layout = new GridLayoutManager(getBaseContext(), 4);
        layout.setOrientation(GridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layout);

        //ADD PERMISSIONS TO ACCESS IMAGES ON PHONE
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL);
        }else{
            retrieveAndSetImages();
        }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_READ_EXTERNAL:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    retrieveAndSetImages();
                }
            }
        }
    }

    public void retrieveAndSetImages(){

        //Run in background from the UI...
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                imageList.clear();
                Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
                if(cursor !=  null){
                    cursor.moveToFirst();

                    for(int i = 0; i < cursor.getCount(); i++){
                        cursor.moveToPosition(i);
                        ImageGetterFromDevice image = new ImageGetterFromDevice(Uri.parse(cursor.getString(1)));
                        imageList.add(image);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //TODO recyclerview set images
                    }
                });
            }
        });

    }

    public class ImageAdapter extends RecyclerView.Adapter<ImageViewHolder>{
        private ArrayList<ImageGetterFromDevice> imagesList;

        public ImageAdapter(ArrayList<ImageGetterFromDevice> imagesList) {
            this.imagesList = imagesList;
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            final ImageGetterFromDevice image = imagesList.get(position);
            holder.UpdateUI(image);

            final ImageViewHolder viewHolder = holder;
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedImage.setImageDrawable(viewHolder.image.getDrawable());
                }
            });
        }

        @Override
        public int getItemCount() {
            return imagesList.size();
        }

        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_image, parent, false);
            return new ImageViewHolder(card);
        }
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder{
        private ImageView image;

        public ImageViewHolder(View itemView) {
            super(itemView);
            image = (ImageView)findViewById(R.id.image_thumb);
        }

        public void UpdateUI(ImageGetterFromDevice deviceImage){
            DecodeBitmapBackground task = new DecodeBitmapBackground(image, deviceImage);
            task.execute();
        }
    }

    //This class decodes the image in the background from the UI.
    class DecodeBitmapBackground extends AsyncTask<Void, Void, Bitmap>{
        private final WeakReference<ImageView> imageViewWeakReference;
        private ImageGetterFromDevice imageGetterFromDevice;

        public DecodeBitmapBackground(ImageView image, ImageGetterFromDevice imageGetterFromDevice) {
            this.imageViewWeakReference = new WeakReference<ImageView>(image);
            this.imageGetterFromDevice = imageGetterFromDevice;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            return decodeURI(imageGetterFromDevice.getImageResourseUri().getPath());
        }

        protected void onPostExecute(Bitmap bitmap){
            super.onPostExecute(bitmap);
            final ImageView img = imageViewWeakReference.get();

            if(img != null){
                img.setImageBitmap(bitmap);
            }
        }
    }

    //This class converts the image and scales it down to the correct size.
    public Bitmap decodeURI(String filepath){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        Boolean scaleByHeight = Math.abs(options.outHeight - 100) >= Math.abs(options.outWidth - 100);
        if(options.outHeight * options.outWidth * 2 > 16384){
            // Load, scaling to smallest power of 2 that'll get it <= desired dimensions
            //adjust 1000 to make scaling to correct look!!!
            double sampleSize = scaleByHeight
                    ? options.outHeight / 1000
                    : options.outWidth / 1000;
            options.inSampleSize =
                    (int)Math.pow(2d, Math.floor(
                            Math.log(sampleSize)/Math.log(2d)));
        }


        options.inJustDecodeBounds = false;
        options.inTempStorage = new byte[512];
        Bitmap output = BitmapFactory.decodeFile(filepath, options);
        return output;
    }




    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
