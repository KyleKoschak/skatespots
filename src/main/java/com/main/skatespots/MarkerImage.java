package com.main.skatespots;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;


public class MarkerImage extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.markerimage);

        Bundle extras = getIntent().getExtras();
        Bitmap bmp = (Bitmap)extras.getParcelable("imagebitmap");

        ImageView image = (ImageView)findViewById(R.id.imageView);
        image.setImageBitmap(bmp);
    }

    public void FullImageOnClick(View v) {
        finish();
    }
}
