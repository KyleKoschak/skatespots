package com.main.skatespots;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class Form extends AppCompatActivity {

    // latlng passed in when form starts
    public static LatLng latlng;

    // Firebase vars
    private DatabaseReference db;
    private StorageReference storageRef;
    private UploadTask uploadTask;

    // Image
    private String mCurrentPhotoPath;
    private ImageView imageView;
    private Button imageBtn;

    static final int CAMERA_RESULT = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form);

        // Database initialization
        db = FirebaseDatabase.getInstance().getReference();

        imageView = (ImageView)findViewById(R.id.image);
        imageBtn = (Button)findViewById(R.id.imgBtn);
    }

    /**
    *   BackBtnOnClick(View) exits the form when the Back button is pressed
     *   @param v is View
    */
    public void BackBtnOnClick(View v) {
        // Return back to map
        finish();
    }

    /**
     * ImgBtnOnClick(View) will launch the camera so the user can take a picture of the spot.
     * First checks for permissions and if permission not set it will open a dialog to ask, if
     * it is set then the camera will open.
     * @param v is button clicked
     */
    public void ImgBtnOnClick(View v) {
        // Check android version
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (Exception ignored) {}
        int MyVersion = pInfo.versionCode;

        // If it requires runtime permissions
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            // If permission not given
            if (!checkIfAlreadyhavePermission()) {
                // Request permission with dialog box
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);
            }
        } else {
            dispatchTakePictureIntent();    // Start camera
        }
    }

    /**
     * checkIfAlreadyhavePermission() checks if permission has already been given for camera
     * @return whether permission has been granted already.
     */
    private boolean checkIfAlreadyhavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        return (result == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * onActivityResult(int, int, Intent) overrides the startActivityForResult so that it
     * will display the picture taken from the camera as the thumbnail.
     * @param requestCode int
     * @param resultCode int
     * @param data Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_RESULT && resultCode == RESULT_OK) {
            // Initial Bitmap
            Bitmap bmp = decodeSampledBitmapFromFile(mCurrentPhotoPath, 1000, 700);

            // Use EcifInterface to rotate image
            ExifInterface exif = null;
            try { exif = new ExifInterface(mCurrentPhotoPath); }
            catch (IOException e) { e.printStackTrace(); }
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            // Get rotated image so its in landscape
            Bitmap bmRotated = rotateBitmap(bmp, orientation);

            // Set ImageView with picture
            imageView.setImageBitmap(bmRotated);
            imageView.setDrawingCacheEnabled(true);
            imageView.buildDrawingCache();

            // Change button text
            imageBtn.setText("Image loaded!");
            imageBtn.setTextColor(Color.parseColor("#12591d"));
        }
    }

    /**
     * dispatchTakePictureIntent() launches the camera app waiting for result.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Error creating the file", Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_RESULT);
            }
        }
    }

    /**
     * createImageFile() creates the recentspot.jpg image inside private app folder.
     * @return image file
     * @throws IOException .
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "recentspot";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * roateBitMap(Bitmap, int) gets passed in Bitmap and rotates it based on orientation/
     * @param bitmap original bitmap
     * @param orientation image orientation
     * @return rotated Bitmap
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        //First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize, Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        int inSampleSize = 1;

        if (height > reqHeight) {
            inSampleSize = Math.round((float)height / (float)reqHeight);
        }
        int expectedWidth = width / inSampleSize;

        if (expectedWidth > reqWidth) {
            //if(Math.round((float)width / (float)reqWidth) > inSampleSize) // If bigger SampSize..
            inSampleSize = Math.round((float)width / (float)reqWidth);
        }

        options.inSampleSize = inSampleSize;

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path, options);
    }


    /**
     * onRequestPermissionsResult is called after the dialog has been closed. Checks to see if
     * permission has been granted for camera use or not and if it has, open the camera.
     * @param requestCode int
     * @param permissions String
     * @param grantResults int[]
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If permission granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Start camera and get result
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null)
                        startActivityForResult(takePictureIntent, CAMERA_RESULT);

                } else {
                    // Permission not granted
                    Toast.makeText(getApplicationContext(), "Permission denied to use the camera", Toast.LENGTH_SHORT).show();
                }
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
    *   SubBtnOnClick(View) adds the data that is in the form into spots table of the database
    *   then returns back to the map.
     *   @param v is View
    */
    public void SubBtnOnClick(View v) {

        // Get the field data
        EditText entName = (EditText)findViewById(R.id.nameText);
        EditText entDescription = (EditText)findViewById(R.id.descriptionText);
        Spinner entType = (Spinner)findViewById(R.id.typeText);


        // Convert field data into types
        String id = db.push().getKey();
        String name = entName.getText().toString();
        double lat = latlng.latitude;
        double lng = latlng.longitude;
        String type = entType.getSelectedItem().toString();
        String description = entDescription.getText().toString();

        // Check for required fields
        if (name.equals("") || type.equals("Select spot type...") || imageView.getDrawable() == null)
            Toast.makeText(this, "Fill in all required fields before submitting.", Toast.LENGTH_LONG).show();
        else {
            // Make spot object with all fields
            Spot spot = new Spot(id, name, description, lat, lng, type);

            // Add the spot into the database. It auto generates a key
            // based on time so it will be in order by time added.
            db.child("spots").child(id).setValue(spot);

            // Send image to storage
            loadBitmapToStorage(id);

            // Return to map
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        }
    }

    /**
     * loadBitmapToStorage(String) grabs image from imageView and stores that into storage
     * by using the filename as the title of the image. Called in SubBtnOnClick(View).
     * @param imgName name used for image in storage database. Same as spot id.
     */
    public void loadBitmapToStorage(String imgName) {
        // Get the bitmap data for storage
        Bitmap bitmap = imageView.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imgData = baos.toByteArray();

        // set name of new image
        storageRef = FirebaseStorage.getInstance().getReference("spots").child(imgName);

        // push to storage
        uploadTask = storageRef.putBytes(imgData);
        uploadTask.addOnFailureListener(exception ->
                Toast.makeText(getApplicationContext(), exception.toString(), Toast.LENGTH_LONG).show())
                .addOnSuccessListener(taskSnapshot -> {

        });

        // delete image from sd card
        File f = new File(mCurrentPhotoPath);

        if (f.exists())
            f.delete();
    }
}
