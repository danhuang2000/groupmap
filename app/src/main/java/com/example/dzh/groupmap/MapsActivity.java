package com.example.dzh.groupmap;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, LocationListener {

    private final static String USERNAME = "username";
    private final static long PERIOD_IN_MILLISECONDS = 30000;
    private final static long MIN_TIME_MILLISECONDS = 30000;
    private final static float MIN_DISTANCE_METERS = 20;

    private GoogleMap mMap;
    private LocationManager mLocMgr;
    private Handler mTimerHandler;
    private String mStatus;


    @Override
    protected void onStart() {
        super.onStart();
        mLocMgr = (LocationManager) this.getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        mTimerHandler = new Handler(this.getBaseContext().getMainLooper());
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            mLocMgr.removeUpdates(this);
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = this.getPreferences(Context.MODE_PRIVATE);

        // Is it coming from RegisterActivity?
        Bundle bundle = this.getIntent().getExtras();
        String username = (bundle == null) ? null : bundle.getString(USERNAME, null);
        if (username == null) {
            username = prefs.getString("username", null);
        }
        else {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", username);
            editor.commit();
        }

        if (username == null) {
            Intent reg = new Intent(this.getBaseContext(), RegisterActivity.class);
            this.startActivity(reg);
        }
        else {
            setContentView(R.layout.activity_maps);
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
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

        try {
            mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_MILLISECONDS, MIN_DISTANCE_METERS, this);
            mLocMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_MILLISECONDS, MIN_DISTANCE_METERS, this);
        }
        catch (SecurityException e) {
            AlertDialog.Builder bdr = new AlertDialog.Builder(this);
            bdr.setMessage("Please turn on location permission")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dlg, int id) {
                            System.exit(0);
                        }
                    }).create().show();
        }

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    URL url = new URL(AppConstants.STATUS_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    mStatus = rd.readLine();
                    conn.disconnect();
                    mTimerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateMap();
                        }
                    });
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, 10, PERIOD_IN_MILLISECONDS);
    }

    @Override
    public void onLocationChanged(final Location location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String name = getPreferences(Context.MODE_PRIVATE).getString(USERNAME, null);
                    double x = location.getLatitude();
                    double y = location.getLongitude();
                    URL url = new URL(AppConstants.UPDATE_URL + name + "&x=" + x + "&y=" + y);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    int retval = conn.getInputStream().read();
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void updateMap() {
        if (mStatus == null) return;
        JSONObject jo = null;
        try {
            jo = new JSONObject(mStatus);
        }
        catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        mMap.clear();
        String username = getPreferences(Context.MODE_PRIVATE).getString(USERNAME, null);
        for (Iterator<String> iter = jo.keys(); iter.hasNext(); ) {
            String key = iter.next();
            try {
                JSONObject itm = (JSONObject)jo.get(key);
                String xo = itm.getString("x");
                String yo = itm.getString("y");

                if ((xo != null) && (yo != null)) {
                    addMarker(Double.parseDouble(xo), Double.parseDouble(yo), key, username);
                }
            }
            catch (JSONException e) {
                // do nothing, skip to the next ittem
            }
        }
    }

    private void addMarker(double x, double y, String label, String mainLabel) {
        LatLng place = new LatLng(x, y);
        mMap.addMarker(new MarkerOptions().position(place).title(label));
        if (label.equals(mainLabel))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 10));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
