package com.main.skatespots;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraMoveListener,
        LocationListener {

    private GoogleMap mMap;
    private DatabaseReference db;

    // Location variables
    private LatLng myLoc = new LatLng(0,0);  // assign a value or cause crash
    private Marker myMarker;    // location marker
    private Bitmap locationBitmap;  //location marker bitmap
    private Circle circle;  // circle around location
    private boolean doOnceLocation = false;
    private LocationManager locationManager;

    // Lists of spots
    private ArrayList<Spot> spots = new ArrayList<>();          // All spots
    private ArrayList<Spot> filteredspots = new ArrayList<>();  // Spots that are filtered out
    private ArrayList<Spot> drawnSpots = new ArrayList<>();     // Spots that are on map

    private EditText filterbox;
    private Button newSpotBtn;
    private boolean newspot = false;    // check if new spot button is active

    // Permissions
    private final int PERMISSION_REQUEST = 1;
    private String[] PERMISSIONS = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Location manager initialize
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, this);
        } catch (SecurityException e) { Toast.makeText(this, "location error", Toast.LENGTH_SHORT).show(); }


        // Database initialization for running loadSpots()
        db = FirebaseDatabase.getInstance().getReference();

        // Views
        filterbox = (EditText)findViewById(R.id.filtertext);
        newSpotBtn = (Button)findViewById(R.id.newspotbutton);

        // Location marker bitmap
        locationBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier("markerlocation", "mipmap", getPackageName()));
        locationBitmap = Bitmap.createScaledBitmap(locationBitmap, 48, 48, false);

        // Request all permissions not currently granted.
        if(!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST);
        }
    }

     /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;   //this is the map
        loadSpots();    //load the spots

        // Listeners for map
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnCameraMoveListener(this);
        addTextChangeListener(filterbox);
    }

    /**
     * onMapLongClick(LatLng) passes point to Form.latlng and starts a new
     * form to add a marker.
     * @param point LatLng of where you pressed
     */
    @Override
    public void onMapLongClick (LatLng point) {
        // If circle is visible (avoids null circle)
        if (newspot) {
            // Check if click is within the circle
            LatLng center = circle.getCenter();
            double radius = circle.getRadius();
            float[] distance = new float[1];
            Location.distanceBetween(point.latitude, point.longitude, center.latitude, center.longitude, distance);
            boolean clicked = distance[0] < radius;

            // if click is within the circle
            if (clicked) {
                Form.latlng = point;
                Intent intent = new Intent(this, Form.class);
                startActivity(intent);
            }
        }
    }

    /**
     *  onMarkerClick(Marker) starts a new Intent to open MarkerInfo
     *  activity
     * @param marker is the marker clicked on
     * @return boolean
     */
    public boolean onMarkerClick(final Marker marker) {
        Intent intent = new Intent(this, MarkerInfo.class);

        // myMarker unclickable
        if (marker == myMarker)
            return false;

        // pass all the spot info that is clicked on
        for (Spot spot : drawnSpots) {
            if (marker.getPosition().latitude == spot.getLat() &&
                    marker.getPosition().longitude == spot.getLng()) {
                intent.putExtra("spotId", spot.getId());
                intent.putExtra("spotName", spot.getName());
                intent.putExtra("spotDescription", spot.getDescription());
                intent.putExtra("spotType", spot.getType());
                intent.putExtra("spotLat", spot.getLat());
                intent.putExtra("spotLng", spot.getLng());

                // open dialog
                startActivity(intent);
                return true;
            }
        }

        return true;
    }

    /**
     * onCameraMove() called when camera starts moving. Disables the new spot circle to
     * no longer allow creation of a new spot.
     */
    @Override
    public void onCameraMove() {
        if (newspot) {
            drawMarkersOnMap(filteredspots);
            newSpotBtn.setBackgroundResource(R.drawable.backgroundnobottomborder);
            newspot = false;
        }
    }

    /**
     * NewSPotOnClick(View) is called when the new spot button is pressed. Zooms in on location
     * and the draws the circle where you can add a spot.
     * @param v
     */
    public void NewSpotOnClick(View v) {
        if (!newspot) {

            // sets newspot mode
            newspot = true;

            // Change map view
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLoc, 18));

            // Draw circle around location
            circle = mMap.addCircle(new CircleOptions()
                    .center(myLoc)
                    .radius(50)
                    .strokeWidth(0)
                    .fillColor(0x350099ff));

            // Change the button to activated
            newSpotBtn.setBackgroundResource(R.drawable.activated_nobottomborder);
        }
    }

    /**
     * ExpandBtnOnClick(View) will expand the filter window on the map if it is minimized.
     * Otherwise it will shrink it down.
     * @param v is button clicked.
     */
    public void ExpandBtnOnClick(View v) {
        View layout = findViewById(R.id.container);
        ViewGroup.LayoutParams params = layout.getLayoutParams();

        if (params.height == 0)
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        else
            params.height = 0;

        layout.setLayoutParams(params);
    }

    /**
     * randomBtnOnClick(View) grabs a random spot within the spots list and launches that spots info.
     * @param v is button
     */
    public void randomBtnOnClick(View v) {
        int randomNum = (int)Math.floor(Math.random() * drawnSpots.size());
        Intent intent = new Intent(this, MarkerInfo.class);
        Spot spot1 = drawnSpots.get(randomNum);

        // pass all the spot info that is clicked on
        for (Spot spot2 : drawnSpots) {
            if (spot1.getLat() == spot2.getLat() && spot1.getLng() == spot2.getLng()) {
                intent.putExtra("spotId", spot1.getId());
                intent.putExtra("spotName", spot1.getName());
                intent.putExtra("spotDescription", spot1.getDescription());
                intent.putExtra("spotType", spot1.getType());

                // open dialog
                startActivity(intent);
            }
        }
    }

    /**
     * loadSpots() adds ValueEventListener to the database. It then goes to
     *  spots table and loops through each spot and places a marker on the
     *  map. Called only one time once map is ready.
     */
    public void loadSpots() {
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // spots table
                dataSnapshot = dataSnapshot.child("spots");

                spots.clear(); // Clear spots to add all new ones in

                // for each spot add marker to map
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    Spot spot = singleSnapshot.getValue(Spot.class);
                    spots.add(spot);
                }

                // Will draw all markers on map. Default option
                drawMarkersOnMap(filteredspots);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error with database...", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * doCheckBoxFiltering is called when a checkbox is pressed. It draws markers
     * onto the map based on checked boxes.
     * @param v is the checkbox that is pressed
     */
    public void doCheckboxFiltering(View v) {
        drawMarkersOnMap(filteredspots);
    }

    /**
     *  drawMarkersOnMap() creates markers on the map based on checkbox filtering. Uses
     *  ArrayList spots to compare each spot with the type in linear time. Called each time
     *  a checkbox is clicked on and the filterbox is used. Will also draw myMarker to map
     *  that shows current location.
     *  @param filteredSpots is a ArrayList<Spot> of all filtered spots that should not
     *                       show up on the map.
     */
    public void drawMarkersOnMap(ArrayList<Spot> filteredSpots) {
        CheckBox stairsbox = (CheckBox)findViewById(R.id.typestairs);
        CheckBox ledgebox = (CheckBox)findViewById(R.id.typeledge);
        CheckBox railbox = (CheckBox)findViewById(R.id.typerail);
        CheckBox skateparkbox = (CheckBox)findViewById(R.id.typeskatepark);
        CheckBox otherbox = (CheckBox)findViewById(R.id.typeother);

        // Clear map of markers
        mMap.clear();
        drawnSpots.clear();


        // For each spot in spots
        for (Spot spot : spots) {
            if (!filteredSpots.contains(spot)) {
                drawnSpots.add(spot);

                // "Stairs" so GREEN marker
                if (stairsbox.isChecked()) {
                    if (spot.getType().equals("Stairs"))
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(spot.getLat(), spot.getLng()))
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.markerstairs)));
                }
                // "Ledge" so RED marker
                if (ledgebox.isChecked()) {
                    if (spot.getType().equals("Ledge"))
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(spot.getLat(), spot.getLng()))
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.markerledge)));
                }
                // "Rail" so BLUE marker
                if (railbox.isChecked()) {
                    if (spot.getType().equals("Rail"))
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(spot.getLat(), spot.getLng()))
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.markerrail)));
                }
                // "Other" so YELLOW marker
                if (otherbox.isChecked()) {
                    if (spot.getType().equals("Other"))
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(spot.getLat(), spot.getLng()))
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.markerother)));
                }
                // "Skatepark" so ORANGE marker
                if (skateparkbox.isChecked()) {
                    if (spot.getType().equals("Skatepark"))
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(spot.getLat(), spot.getLng()))
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.markerskatepark)));
                }
            }
        }


        // draw myMarker again after mMap is cleared.
        if (myMarker != null)
            myMarker.remove();

        if (myLoc.longitude != 0 )
            myMarker = mMap.addMarker(new MarkerOptions().position(myLoc)
                    .icon(BitmapDescriptorFactory.fromBitmap(locationBitmap)));
    }

    /**
     * addTextChangeListener(EditText) adds a textChangeListener to an EditText. This method is
     * called only once to add the listener.
     * @param text is the EditText which adds the listener
     */
    public void addTextChangeListener(EditText text) {
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            /**
             * onTextChanged(CharSequence, int, int, int) is called every time new text is
             * entered within the EditText field.
             * @param s is the text within the EditText
             * @param start beginning of EditText
             * @param before characters of old text
             * @param count counts characters beginning at start
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // clear filteredspots
                filteredspots.clear();

                // convert all to lowercase
                String st = s.toString().toLowerCase();
                String tokens[] = st.trim().split("\\s+"); // regex to split by spaces

                // check to find matches between spots.getDescription and EditText
                for (int i = 0; i < spots.size(); i++) {
                    for (String token:tokens) {
                        if (!(spots.get(i).getKeywords().contains(token))) {
                            filteredspots.add(spots.get(i));    // No match, so filter spot
                        }
                    }
                }

                // Draw markers to map
                drawMarkersOnMap(filteredspots);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * hasPermissions(Context, String...) returns boolean of whether the passed in string array
     * has any permissions that are granted or not. Called within onCreate().
     * @param context current activity
     * @param permissions string array of permissions
     * @return true if permissions is granted, false otherwise.
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    /*
    ***********************************************************************************************
    ****************************** LOCATION METHODS ***********************************************
    ***********************************************************************************************
     */

    /**
     * onLocationChanged(Location) is called whenever the user has changed locations.
     * It updates myLoc which will move the location marker on the map.
     * @param location new location
     */
    @Override
    public void onLocationChanged(Location location) {
        myLoc = new LatLng(location.getLatitude(), location.getLongitude());

        // Set camera once on launch. Block of code runs only once.
        if (!doOnceLocation) {
            doOnceLocation = true;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLoc, 15));
        }

        // Update marker position
        if (myMarker != null)
            myMarker.remove();

        myMarker = mMap.addMarker(new MarkerOptions().position(myLoc)
                .icon(BitmapDescriptorFactory.fromBitmap(locationBitmap)));
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(getBaseContext(), "Gps is turned on!! ",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
        Toast.makeText(getBaseContext(), "Gps is turned off!! ",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}