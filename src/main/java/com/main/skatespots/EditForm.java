package com.main.skatespots;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditForm extends Activity {

    // Firebase var
    private DatabaseReference db;

    // Fields
    private String id;
    private EditText nameText;
    private EditText descText;
    private Spinner typeOption;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editform);

        Intent intent = getIntent();

        // Database initialization
        db = FirebaseDatabase.getInstance().getReference("spots");

        // Fields initialize
        nameText = (EditText)findViewById(R.id.nameText);
        descText = (EditText)findViewById(R.id.descriptionText);
        typeOption = (Spinner)findViewById(R.id.typeText);
        id = (String)intent.getSerializableExtra("spotId");

        // Set fields with old data
        nameText.setText((String)intent.getSerializableExtra("spotName"));
        descText.setText((String)intent.getSerializableExtra("spotDescription"));
        typeOption.setSelection(((ArrayAdapter)typeOption.getAdapter()).getPosition(intent.getSerializableExtra("spotType")));
    }

    /**
     *   BackBtnOnClick(View) exits the form when the Back button is pressed
     *   @param v is Button
     */
    public void BackBtnOnClick(View v) {
        // Return back to map
        finish();
    }


    /**
     * SubBtnOnClick(View) is called when submitting the edited spot. It will delete the old one
     * and replace with a new one.
     * @param v is Button
     */
    public void SubBtnOnClick(View v) {
        Intent intent = getIntent();

        // DELETE THE SPOT
        db.child(id).removeValue();     //remove from database

        // Get the field data
        EditText entName = (EditText)findViewById(R.id.nameText);
        EditText entDescription = (EditText)findViewById(R.id.descriptionText);
        Spinner entType = (Spinner)findViewById(R.id.typeText);


        // Convert field data into types
        String name = entName.getText().toString();
        double lat = (double)intent.getSerializableExtra("spotLat");
        double lng = (double)intent.getSerializableExtra("spotLng");
        String type = entType.getSelectedItem().toString();
        String description = entDescription.getText().toString();

        // Check for required fields
        if (name.equals("") || type.equals("Select spot type..."))
            Toast.makeText(this, "Fill in all required fields before submitting.", Toast.LENGTH_LONG).show();
        else {
            // Make spot object with all fields
            Spot spot = new Spot(id, name, description, lat, lng, type);

            // Add the spot into the database. It auto generates a key
            // based on time so it will be in order by time added.
            db.child(id).setValue(spot);

            // Return to map
            Intent nintent = new Intent(this, MapsActivity.class);
            startActivity(nintent);
        }
    }

}
