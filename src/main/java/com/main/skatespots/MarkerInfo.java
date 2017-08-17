package com.main.skatespots;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;

public class MarkerInfo extends Activity {
    // maximum size of read image. Will hold in memory.
    private final long ONE_MEGABYTE = 1024 * 1024;

    private String id;
    private String name;
    private String desc;
    private String type;

    private TextView tName;
    private TextView tDesc;
    private TextView tType;
    private ImageView image;

    private String text;

    DatabaseReference db;
    StorageReference storageRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.markerinfo);

        Intent intent = getIntent();

        // firebase storage
        storageRef = FirebaseStorage.getInstance().getReference("spots");

        // Get marker info
        id = (String)intent.getSerializableExtra("spotId");
        name = (String)intent.getSerializableExtra("spotName");
        desc = (String)intent.getSerializableExtra("spotDescription");
        type = (String)intent.getSerializableExtra("spotType");

        // set title
        this.setTitle(name);

        // Get views
        tName = (TextView)findViewById(R.id.infoName);
        tDesc = (TextView)findViewById(R.id.infoDesc);
        tType = (TextView)findViewById(R.id.infoType);
        image = (ImageView)findViewById(R.id.imageView);

        // Set Name text
        text = String.format(getString(R.string.markername), name);
        tName.setText(text);
        tName.setTextColor(Color.parseColor("#12591d"));

        // Set Description text
        text = String.format(getString(R.string.markerdesc), desc);
        tDesc.setText(text);

        // Set Type text
        text = String.format(getString(R.string.markertype), type);
        tType.setText(text);

        // Loads image into imageView
        loadImg();

    }

    /**
     * ReportBtnOnClick(View) deletes the spot in the database. Loads the database, asks for
     * confirmation, then if yes will delete that spot.
     * @param v is the button
     */
    public void ReportBtnOnClick(View v) {
        db = FirebaseDatabase.getInstance().getReference("spots");

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Delete " + name)
                .setMessage("Are you sure you want to delete this spot?")
                .setPositiveButton("Yes", (dialog, idnum) -> {
                    db.child(id).removeValue();     //remove from database
                    storageRef.child(id).delete();  //remove image from storage

                    Intent intent = new Intent(this, MapsActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * loadImg() gets the bitmap from Firebase Storage then decodes it and sets the imageView
     * to that bitmap. Called within onCreate().
     */
    public void loadImg() {
        storageRef.child(id).getBytes(ONE_MEGABYTE).addOnSuccessListener(data -> {
            Bitmap b = BitmapFactory.decodeByteArray(data,0,data.length);
            image.setImageBitmap(b);
        }).addOnFailureListener(exception -> {
            Toast.makeText(getApplicationContext(), "Unable to load spot image.", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * ImageOnClick(View) launches the intent to open a new activity which loads the image
     * using the entire screen.
     * @param v is the ImageView
     */
    public void ImageOnClick(View v) {
        v.buildDrawingCache();
        Bitmap image = v.getDrawingCache();

        Bundle extras = new Bundle();
        Intent intent = new Intent(this, MarkerImage.class);

        extras.putParcelable("imagebitmap", image);
        intent.putExtras(extras);
        startActivity(intent);
    }

    /**
     * EditBtnOnClick(View) opens up the edit form with option to update the current firebase
     * entry.
     * @param v is Edit button
     */
    public void EditBtnOnClick(View v) {
        Intent getIntent = getIntent();
        Intent intent = new Intent(this, EditForm.class);

        intent.putExtra("spotId", id);
        intent.putExtra("spotName", name);
        intent.putExtra("spotDescription", desc);
        intent.putExtra("spotType", type);
        intent.putExtra("spotLat", (double)getIntent.getSerializableExtra("spotLat"));
        intent.putExtra("spotLng", (double)getIntent.getSerializableExtra("spotLng"));

        startActivity(intent);
    }
}
