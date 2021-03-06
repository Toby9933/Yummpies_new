package com.example.VEXT;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1234 ;
    private GoogleMap mMap;
    private String TAG = "My Activity";
    private static final String EATERY_NAME = "name";
    private static final String EATERY_LOCATION = "location";
    private static final String EATERY_DESCRIPTION = "description";




    private FirebaseUser user;

    private Location currentLocation;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Eateries");

    private DocumentReference noteRef = db.collection("Eateries").document("Eatery");

    private static String name;
    private static String description;
    private static GeoPoint gp;
    private static LatLng varpos;

    private String marker_id, id;

    Map<LatLng,String> hashmap = new HashMap<LatLng,String>();

    private FusedLocationProviderClient fusedLocationProviderClient;

    GeoPoint saved_geopoint = null;




    private String USER_LIST = "Users";

    private static final float DEFAULT_ZOOM = 15;



    SearchView searchView;


    Map user_instance = new HashMap<>();


    LinearLayout linear;


    @Override
    protected void onStart() {
        super.onStart();
        getLocation();

        if(saved_geopoint!=null)
        {
            LatLng latLng = new LatLng(saved_geopoint.getLatitude(),saved_geopoint.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10F));
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        searchView = findViewById(R.id.searchView);
        user = getIntent().getParcelableExtra("user");
        id = user.getUid();

        linear = findViewById(R.id.linearLayout2);





        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString();
                List<Address> addressList = null;
                if(location!=null || !location.equals(""))
                {
                    Geocoder geocoder = new Geocoder(MapsActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                        if(addressList.size()!=0)
                        {

                            Address address = addressList.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(),address.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10F));
                            saved_geopoint = new GeoPoint(latLng.latitude, latLng.longitude);
                        }
                        else
                        {

                            Snackbar.make(linear, "No such Location found!", Snackbar.LENGTH_INDEFINITE)
                                    .setAction("Close", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {

                                        }
                                    })
                                    .setActionTextColor(getResources().getColor(R.color.colorAccent)).show();
                        }

                    }
                    catch (Exception r)
                    {
                        r.printStackTrace();
                    }
                    if(addressList!=null)
                    {

                    }

                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });



    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
                mMap.setMyLocationEnabled(true);


        collectionReference.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for(QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots)
                {
                    name = documentSnapshot.getString(EATERY_NAME);
                    gp = documentSnapshot.getGeoPoint(EATERY_LOCATION);
                    description = documentSnapshot.getString(EATERY_DESCRIPTION);
                    if(gp!=null)
                        varpos = new LatLng(gp.getLatitude(), gp.getLongitude());

                    MarkerOptions marker = new MarkerOptions().position(varpos).title(name).snippet(description);
                    mMap.addMarker(marker);

                    hashmap.put(varpos ,documentSnapshot.getId().toString());

                    mMap.setOnInfoWindowClickListener(MapsActivity.this);

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MapsActivity.this, "FAILED", Toast.LENGTH_SHORT).show();
                Snackbar.make(linear, "No such Location found!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Close", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                            }
                        })
                        .setActionTextColor(getResources().getColor(R.color.colorAccent)).show();
            }
        });

    }





    private void moveCamera(LatLng latLng, float zoom)
    {
        Log.d(TAG, "moveCamer: moving camera to lat and long");
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }



    void getLocation() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            final Task location = fusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onComplete(@NonNull Task task) {
               if(task.isSuccessful())
               {
                   if(task.getResult()!=null)
                   {
                       currentLocation = (Location) task.getResult();

                       if(saved_geopoint==null)
                       {
                           mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM));
                       }
                       else
                       {
                           moveCamera(new LatLng(saved_geopoint.getLatitude(), saved_geopoint.getLongitude()), 10F);
                       }
//                       moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);
                   }
                   else
                   {
                       LocationListener locationListener;
                       locationListener = new LocationListener() {
                           @Override
                           public void onLocationChanged(Location location) {
                               currentLocation = location;
                           }

                           @Override
                           public void onStatusChanged(String s, int i, Bundle bundle) {

                           }

                           @Override
                           public void onProviderEnabled(String s) {

                           }

                           @Override
                           public void onProviderDisabled(String s) {
                               Snackbar.make(linear, "GPS Not Enabled!", Snackbar.LENGTH_INDEFINITE)
                                       .setAction("Close", new View.OnClickListener() {
                                           @Override
                                           public void onClick(View view) {

                                           }
                                       })
                                       .setActionTextColor(getResources().getColor(R.color.colorAccent)).show();
                           }
                       };

                       LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                       locationManager.requestLocationUpdates("gps", 5000,0, locationListener);
                       Snackbar.make(linear, "Please Turn on Location Services!", Snackbar.LENGTH_INDEFINITE)
                               .setAction("Close", new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {

                                   }
                               })
                               .setActionTextColor(getResources().getColor(R.color.colorAccent)).show();
                   }
               }
               else
               {

                   Snackbar.make(linear, "location not Found !", Snackbar.LENGTH_INDEFINITE)
                           .setAction("Close", new View.OnClickListener() {
                               @Override
                               public void onClick(View view) {

                               }
                           })
                           .setActionTextColor(getResources().getColor(R.color.colorAccent)).show();

               }
                }
            });
        }
        catch (Exception e)
        {

        }

    }

    @Override
    public void onInfoWindowClick(Marker marker) {


        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("GPS not enabled")
                    .setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                        }

                    })
                    .setNegativeButton("Cancel",null)
                    .show();

        }


        if(gps_enabled && network_enabled)
        {
            LatLng b = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);

            marker_id = hashmap.get(b);


            //Intent a = new Intent(MapsActivity.this, Eatery.class);

            //a.putExtra("user",   user);

            //a.putExtra("marker", marker_id);

           // MapsActivity.this.startActivity(a);
        }






    }




}
