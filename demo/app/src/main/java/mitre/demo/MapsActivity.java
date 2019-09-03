package mitre.demo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private Handler handler = new Handler();
    double latitude, longitude;
    GPSTracker gps;
    LatLng curPos;
    SQLitehelper mHelper = new SQLitehelper(this);

    private static final String TAG = "MapsActivity";
    private static final float DEFAULT_ZOOM = 18f;
    private static final String permissionTag = "permission";
    private static final int PERMISSION_ALL = 1;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    //Widgets
    private EditText mSearchText;
    private ImageView mGps;
    private Spinner spinner;
    private ImageView mRotate;

    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;

    private WifiManager mainWifi;
    private Map<String, Signal> wifiSignals;
    private Map<String, Signal> newWifiSignals;
    private SignalReceiver signalReceiver;

    private List<PriorityQueue<SignalInfo>> top5list = new ArrayList<>();
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // get user's permission to get access to locations
        if(!hasPermissions(this, PERMISSIONS)){
            requestLocationPermission();
        } else {
            Log.i(permissionTag,"permissions granted at start");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mSearchText = findViewById(R.id.input_search);
        mGps = findViewById(R.id.ic_gps);
        mRotate = findViewById(R.id.ic_rotate);

        init();
        setSignalReceiver();
        startSignalSearch();
        spinner();
        startBtn();

        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        String currentDateandTime = sdf.format(new Date());

        // Night Mode from 20 pm - 7 am
        if (Integer.parseInt(currentDateandTime) > 20 || Integer.parseInt(currentDateandTime) < 7) {
            try {
                // Customize the styling of the base map using a JSON object defined in a raw resource file.
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                this, R.raw.map_style));

                if (!success) {
                    Log.e(TAG, "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Can't find style. Error: ", e);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getDeviceLocation();
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        wifiSignals = new HashMap<>();
        newWifiSignals = new HashMap<>();

        try {
            populateHeatmap();
        } catch (JSONException e) {
            Toast.makeText(this, "Problem getting data from db.", Toast.LENGTH_LONG).show();
        }
    }

    //Get return value from PopWindow Activity by Xintian
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1000) {
            if(resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra("result");
                if (result.equals("1")) {
                    if (mOverlay != null) mOverlay.remove();
                    //draw a heatmap
                    int[] colors = setColors(102, 225, 0, 255, 0, 0);
                    float[] startPoints = setStartPoints(0.2f, 1f);
                    Gradient gradient = setGradient(colors, startPoints);
                    drawHeatmap(getHeatmap(top5list.get(0)), gradient, 15, 0.7);
                    // Convert pq to JSON String
                    try {
                        String json = makeJSON(top5list.get(0));
                        saveBtn(json);
                    } catch (JSONException e) {
                        Log.i("Error", "Json convert failed.");
                    }
                }
                if (result.equals("2")) {
                    if (mOverlay != null) mOverlay.remove();
                    //draw a heatmap
                    int[] colors = setColors(0, 0, 255, 255, 255, 0);
                    float[] startPoints = setStartPoints(0.2f, 1f);
                    Gradient gradient = setGradient(colors, startPoints);
                    drawHeatmap(getHeatmap(top5list.get(1)), gradient, 15, 0.7);
                    // Convert pq to JSON String
                    try {
                        String json = makeJSON(top5list.get(1));
                        saveBtn(json);
                    } catch (JSONException e) {
                        Log.i("Error", "Json convert failed.");
                    }
                }
                if (result.equals("3")) {
                    if (mOverlay != null) mOverlay.remove();
                    //draw a heatmap
                    int[] colors = setColors(128, 128,128, 255, 0, 0);
                    float[] startPoints = setStartPoints(0.2f, 1f);
                    Gradient gradient = setGradient(colors, startPoints);
                    drawHeatmap(getHeatmap(top5list.get(2)), gradient, 15, 0.7);
                    // Convert pq to JSON String
                    try {
                        String json = makeJSON(top5list.get(2));
                        saveBtn(json);
                    } catch (JSONException e) {
                        Log.i("Error", "Json convert failed.");
                    }
                }
            }
        }
    }

    //Initialize search bar by Xinyue
    private void init() {
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                        keyEvent.getAction() == KeyEvent.ACTION_DOWN || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER)
                {
                    //execute our method for searching
                    geoLocate();
                }
                return false;
            }
        });

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });

        mRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(curPos)
                        .zoom(18)
                        .bearing(45)
                        .tilt(90)                   // Tilt can only be 0 - 90
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
    }

    //Get searched location by Xinyue
    private void geoLocate() {
        String searchString = mSearchText.getText().toString();
        Geocoder geocoder = new Geocoder(this);
        List<Address> list = new ArrayList<>();

        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }

        if(list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG,"geoLocate: found a location:" + address.toString());
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM);
        } else {
            Log.d(TAG, "geoLocate: list size == 0");
        }
    }

    //Enable start Button by Xinyue
    private void startBtn() {
        final Button mark = findViewById(R.id.startBtn);
        final ImageView save = findViewById(R.id.saveBtn);
        // Create the gradient.
        final int[] colors = setColors(102, 255, 0, 255, 0, 0);
        final float[] startPoints = setStartPoints(0.2f, 1f);
        final Gradient gradient = setGradient(colors, startPoints);

        mark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save.setVisibility(View.INVISIBLE);
                if(mark.getText().equals("Start")) {
                    mark.setText("Stop");
                    if(mOverlay != null) {
                        mOverlay.remove();
                    }
                    // Updating all signals every other second
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            gps = new GPSTracker(MapsActivity.this);
                            latitude = gps.getLatitude();
                            longitude = gps.getLongitude();
                            curPos = new LatLng(latitude, longitude);
                            Log.i("CurPos", curPos.toString());
                            //getDeviceLocation();
                            wifiSignals = updateWifi();
                            getBestKLoc(wifiSignals);
                            getBestKWifi();
                            //draw a heat map
                            if (!top5list.isEmpty()) {
                                drawHeatmap(getHeatmap(top5list.get(0)), gradient, 15, 0.7);
                            }

                            // delete heat map every 2 seconds
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mOverlay.remove();
                                }
                            }, 2000);
                            handler.postDelayed(this, 2000);
                        }
                    };

                    handler.postDelayed(runnable, 1000);
                }
                else {
                    try {
                        if (!top5list.isEmpty()) {
                            makeJSON(top5list.get(0));
                        }
                    } catch (JSONException e) {
                        Log.i("makeJson", "Sorry failed to print.");
                    }

                    //pop-window
                    for (int i = 0; i < top5list.size(); i++) {
                        Log.i("top_" + (i + 1) + ": ", top5list.get(i).toString());
                    }
                    Intent intent = new Intent(MapsActivity.this, PopWindow.class);
                    intent.putExtra("name1", top5list.get(0).peek().wifiName);
                    intent.putExtra("strength1", top5list.get(0).peek().strength);

                    intent.putExtra("name2", top5list.get(1).peek().wifiName);
                    intent.putExtra("strength2", top5list.get(1).peek().strength);

                    intent.putExtra("name3", top5list.get(2).peek().wifiName);
                    intent.putExtra("strength3", top5list.get(2).peek().strength);

                    startActivityForResult(intent, 1000);
                    mark.setText("View");
                    handler.removeCallbacks(runnable);
                }
            }
        });
    }

    //Enable Save Button by Xintian
    private void saveBtn(String s) {
        final ImageView saveBtn = findViewById(R.id.saveBtn);
        final String data = s;
        saveBtn.setVisibility(View.VISIBLE); //To set visible
        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(MapsActivity.this)
                        .setMessage("Are you sure to save this heat map?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Save data to DB
                                mHelper.addData(data);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
            }
        });
    }

    //Enable drop down list by Xintian
    private void spinner() {
        spinner = findViewById(R.id.spinner);

        List<String> types = new ArrayList<>();
        types.add("Normal");
        types.add("Hybrid");
        types.add("Satellite");
        types.add("Terrain");

        //Style and populate the spinner
        ArrayAdapter<String> dataAdapter;
        dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);

        //Dropdown layout style
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //Attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getItemAtPosition(position).equals("Normal")) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                } else if (parent.getItemAtPosition(position).equals("Hybrid")) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                } else if (parent.getItemAtPosition(position).equals("Satellite")) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else if (parent.getItemAtPosition(position).equals("Terrain")) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                }
                //on selecting a spinner item
                String item = parent.getItemAtPosition(position).toString();

                //show selected spinner item
                Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //TODO Auto-generated method

            }
        });
    }

    //Get Current Location using Google Play Service by Xinyue
    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            if (currentLocation != null) {
                                curPos = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                moveCamera(curPos, DEFAULT_ZOOM);
                            }
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException:" + e.getMessage());
        }
    }

    //Move camera by Xinyue
    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG,"moveCamera: moving the camera to : lat:" + latLng.latitude + ", lng:"+latLng.longitude);
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoom)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    //Get heatmap data from db by Xintian
    private void populateHeatmap() throws JSONException {
        Log.d("MyDB", "populateHeatmap: Displaying data in Heatmap.");
        //get data from db
        Cursor data = mHelper.getData();
        ArrayList<WeightedLatLng> list = new ArrayList<>();
        while (data.moveToNext()) {
            //get Json String data
            Log.i("getData", data.getString(1));

            String json = data.getString(1);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                double lat = object.getDouble("lat");
                double lng = object.getDouble("lng");
                int strength = object.getInt("strength");

                list.add(new WeightedLatLng(new LatLng(lat, lng), strength));
            }
        }
        if (list == null || list.isEmpty()) return;
        //draw heatmap
        mProvider = new HeatmapTileProvider.Builder()
                .weightedData(list)
                .build();
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }

    //Generate heatmap data from pq by Xintian
    private List<WeightedLatLng> getHeatmap(PriorityQueue<SignalInfo> pq){
        List<WeightedLatLng> list = new ArrayList<>();
        for (SignalInfo info : pq){
            double lat = info.latitude;
            double lng = info.longitude;
            list.add(new WeightedLatLng(new LatLng(lat, lng), info.strength));
            Log.i("Location ", "( " + lat + " , " + lng + " )");
        }
       // Log.i("Size: ", "" + pq.size());
        Log.i("size:","" + pq.size());
        return list;
    }

    //Draw heatmap by Xintian
    private void drawHeatmap(List<WeightedLatLng> data, Gradient gradient, int radius, double opacity) {
        // Create the tile provider.
        mProvider = new HeatmapTileProvider.Builder()
                .weightedData(data)
                .gradient(gradient)
                .radius(radius)
                .opacity(opacity)
                .build();

        // Add the tile overlay to the map.
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }

    //Set Heatmap Colors by Xintian
    private int[] setColors(int r1, int g1, int b1, int r2, int g2, int b2) {
        int[] colors = {
                Color.rgb(r1, g1, b1), //Outer Color
                Color.rgb(r2, g2, b2)  //Inner Color
        };
        return colors;
    }

    //Set Heatmap StartPoints by Xintian
    private float[] setStartPoints(float a, float b){
        float[] startPoints = {a , b};
        return startPoints;
    }

    //Set Heatmap Gradients by Xintian
    private Gradient setGradient(int[] colors, float[] startPoints) {
        Gradient gradient = new Gradient(colors, startPoints);
        return gradient;
    }

    // Reconstruction when refresh top 5 wifi pq && pq_now by Yingkai & Xinyue
    private void get5WifiPQ(){
        PriorityQueue<SignalInfo> top5Wifi=new PriorityQueue<>(new Comparator<SignalInfo>() {
            @Override
            public int compare(SignalInfo o1, SignalInfo o2) {
                return o2.strength  - o1.strength;
            }
        });
        Set<String> set = new HashSet<>();
        HashMap<String, Signal> updatedWifi = new HashMap<>();
        for (Signal signal : wifiSignals.values()) {
            if (newWifiSignals.containsKey(signal.getId())) {
                updatedWifi.put(signal.getName(), signal);
            }
        }
        for (Signal signal : newWifiSignals.values()) {
            if (updatedWifi.containsKey(signal.getName())) {
                Signal cur = updatedWifi.get(signal.getName());
                cur.update(signal.getLevel());
            } else {
                updatedWifi.put(signal.getName(), signal);
            }

            if (!set.contains(signal.getName())) {
                top5Wifi.add(new SignalInfo(signal.getId(), signal.getName(), signal.getStrength(),
                        latitude, longitude));
                set.add(signal.getName());
            }
            while(top5Wifi.size() > 5){
                top5Wifi.remove();
            }
        }
        initPQ(top5Wifi);
    }

    // Initial at beginning by Yingkai & Xinyue
    private void initPQ(PriorityQueue<SignalInfo> pqInfo){
        while(!pqInfo.isEmpty()){
            PriorityQueue<SignalInfo> pq = new PriorityQueue<>(new Comparator<SignalInfo>() {
                @Override
                public int compare(SignalInfo o1, SignalInfo o2) {
                    return o2.strength - o1.strength;
                }
            });
            pq.add(pqInfo.poll());
            top5list.add(pq);
        }
    }

    // Update top 5 wifi by Yingkai & Xinyue
    private void getBestKWifi(){
        PriorityQueue<Signal> temp = new PriorityQueue<>(new Comparator<Signal>() {
            @Override
            public int compare(Signal o1, Signal o2) {
                return o2.getStrength() - o1.getStrength();
            }
        });
        HashSet<String> set = new HashSet<>();
        for (Signal signal : newWifiSignals.values()) {
            Log.i(signal.getName(), "newwifi: ");
            for (PriorityQueue<SignalInfo> pq:top5list )
                if(pq.peek().wifiName.equals(signal.getName())){
                    if(!set.contains(signal.getName())){
                        set.add(signal.getName());
                        temp.add(signal);
                    }

                }
        }
        top5list.sort(new PQComparator());
    }

    // Comparator to sort List by Yingkai
    class PQComparator implements Comparator <PriorityQueue<SignalInfo>> {
        // Overriding compare()method of Comparator
        public int compare(PriorityQueue<SignalInfo> p1, PriorityQueue<SignalInfo> p2) {
            return findMedian(p2) - findMedian(p1);
        }
    }

    // Find median of pq by Yingkai
    private Integer findMedian(PriorityQueue<SignalInfo> pq){
        List<SignalInfo> temp = new ArrayList<>();
        int half = pq.size()/2;
        int res = 0;
        while (half > 0){
            SignalInfo curr = pq.poll();
            if (curr != null) {
                temp.add(curr);
                res = curr.strength;
            }
            half--;
        }
        for(SignalInfo s:temp){
            pq.add(s);
        }
        return res;
    }

    // Get strongest 100 signal strength sources by Yingkai & Xinyue
    private void getBestKLoc(Map<String, Signal> map) {
        String wifiId;
        String wifiName;

        if (top5list.size() < 1) {
            get5WifiPQ();

        } else {
            for (Map.Entry<String, Signal> entry : map.entrySet()) {
                String name = entry.getValue().getName();
                for (int i = 0; i < top5list.size(); i++) {
                    PriorityQueue<SignalInfo> info = new PriorityQueue<>(new Comparator<SignalInfo>() {
                        @Override
                        public int compare(SignalInfo o1, SignalInfo o2) {
                            return o2.strength - o1.strength;
                        }
                    });

                    for( SignalInfo s: top5list.get(i)){
                        info.add(s);
                    }

                    if (info.peek().wifiName.equals(name)) {
                        int strength = entry.getValue().getStrength();
                        wifiId = entry.getValue().getId();
                        wifiName = entry.getValue().getName();
                        info.add(new SignalInfo(wifiId, wifiName, strength, latitude, longitude));
                        top5list.set(i,info);
                    }
                }
            }
        }
    }

    // Convert data to JSON by Yingkai
    private String makeJSON(PriorityQueue<SignalInfo> pq) throws JSONException {
        JSONArray array = new JSONArray();
        for (SignalInfo element : pq) {
            JSONObject jsonElement = new JSONObject();
            jsonElement.put("name", element.wifiName);
            jsonElement.put("lat", element.latitude);
            jsonElement.put("lng", element.longitude);
            jsonElement.put("strength", element.strength);
            array.put(jsonElement);
        }
        return array.toString();
    }

    /*** PLEASE DO NOT ALTER THE FOLLOWINGS !!! ***/

    /** Start: checks if permissions are granted by SP18 Team **/
    public static boolean hasPermissions(Context context, String[] permissions) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        } else if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("permission", "Location permission has not been granted yet");
            return false;
        }
        return true;
    }

    public void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("Location permission is needed to get signals in area")
                    .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MapsActivity.this, PERMISSIONS, PERMISSION_ALL);
                        }
                    })
                    .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Toast.makeText(MapsActivity.this, "Enable the location permission in settings if you want to use this app", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        if (requestCode == PERMISSION_ALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                onMapReady(mMap);
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                requestLocationPermission();
            }
        }

    }
    /** End: checks if permissions are granted by SP18 Team **/

    /** Start: Initilize wifi search by SP18 Team **/
    private HashMap<String, Signal> updateWifi() {
        HashMap<String, Signal> updatedWifi = new HashMap<>();
        for (Signal signal : wifiSignals.values()) {
            if (newWifiSignals.containsKey(signal.getId())) {
                updatedWifi.put(signal.getId(), signal);
            }
        }
        for (Signal signal : newWifiSignals.values()) {
            if (updatedWifi.containsKey(signal.getId())) {
                Signal cur = updatedWifi.get(signal.getId());
                cur.update(signal.getLevel());
            } else {
                updatedWifi.put(signal.getId(), signal);
            }
        }
        return updatedWifi;
    }

    private void wifiReceived() {
        //Log.i(wifiTag, "\n ========wifi search complete========== \n");

        HashMap<String, Signal> tempWifiSignals = new HashMap<>();
        List<ScanResult> wifiScanResults = mainWifi.getScanResults();
        for (ScanResult wifi: wifiScanResults) {
            Signal wifiSignal = new Signal(wifi);
            tempWifiSignals.put(wifi.BSSID, wifiSignal);
            //Log.i(wifiTag,wifi.SSID + " - " + wifi.BSSID + " - " + wifi.level);
        }
        newWifiSignals = tempWifiSignals;
        //Log.i(wifiTag, "Starting another search again");

        mainWifi.startScan();
    }

    public class SignalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                wifiReceived();
            }
        }
    }

    /* set up broadcast receiver to identify signals */
    private void setSignalReceiver() {
        signalReceiver = new SignalReceiver();
        IntentFilter signalIntent = new IntentFilter();
        signalIntent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
//        signalIntent.addAction(BluetoothDevice.ACTION_FOUND);
//        signalIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(signalReceiver, signalIntent);
    }

    /* start seaching for signals in area signals */
    private void startSignalSearch() {
        // for wifi
        mainWifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if(mainWifi.isWifiEnabled()) {
            mainWifi.startScan();
        }
    }
    /** End: Initilize wifi search by SP18 Team **/
}
