package com.theassembly.locationaware;

import android.graphics.Color;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapPolygon;
import com.here.android.mpa.search.DiscoveryRequest;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.DiscoveryResultPage;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest2;
import com.here.android.mpa.search.GeocodeResult;
import com.here.android.mpa.search.PlaceLink;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.SearchRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Map map = null;
    private MapFragment mapFragment = null;
    private PositioningManager positioningManager = null;
    private PositioningManager.OnPositionChangedListener positionListener;
    private GeoCoordinate currentPosition = new GeoCoordinate(37.7397, -121.4252);
    private GeoCoordinate oldPosition = new GeoCoordinate(37.7397, -121.4252);

    public void makeRequest(GeoCoordinate coords) {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,"https://gfe.api.here.com/2/search/proximity.json?layer_ids=4711&app_id=APP_ID_HERE&app_code=APP_CODE_HERE&proximity=" + coords.getLatitude() + "," + coords.getLongitude() + "&key_attribute=NAME", null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray geometries = response.getJSONArray("geometries");
                    if(geometries.length() > 0) {
                        if(geometries.getJSONObject(0).getJSONObject("attributes").getString("NAME").equals("PIZZA_HUT")) {
                            Log.d("HERE", "Don't eat at Pizza Hut, eat at Dominos.");
                        } else {
                            Log.d("HERE", "Don't eat at Dominos, eat at Pizza Hut.");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("HERE", error.getMessage());
            }
        });
        queue.add(request);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {

                    map = mapFragment.getMap();
                    map.setCenter(new GeoCoordinate(37.7394, -121.4366, 0.0), Map.Animation.NONE);
                    map.setZoomLevel(18);
                    //map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                    positioningManager = PositioningManager.getInstance();
                    positionListener = new PositioningManager.OnPositionChangedListener() {

                        @Override
                        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position, boolean isMapMatched) {
                            currentPosition = position.getCoordinate();
                            if(!currentPosition.equals(oldPosition)) {
                                makeRequest(new GeoCoordinate(position.getCoordinate().getLatitude(), position.getCoordinate().getLongitude()));
                                map.setCenter(position.getCoordinate(), Map.Animation.NONE);
                                oldPosition = currentPosition;
                            }
                        }
                        /*@Override
                        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position, boolean isMapMatched) {
                            currentPosition = position.getCoordinate();
                            if(!currentPosition.equals(oldPosition)) {
                                map.setCenter(position.getCoordinate(), Map.Animation.NONE);
                                oldPosition = currentPosition;
                            }
                        }*/
                        @Override
                        public void onPositionFixChanged(PositioningManager.LocationMethod method, PositioningManager.LocationStatus status) { }
                    };
                    try {
                        positioningManager.addListener(new WeakReference<>(positionListener));
                        if(!positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                            Log.e("HERE", "PositioningManager.start: Failed to start...");
                        }
                    } catch (Exception e) {
                        Log.e("HERE", "Caught: " + e.getMessage());
                    }
                    map.getPositionIndicator().setVisible(true);

                    List<GeoCoordinate> pizzahutShape = new ArrayList<GeoCoordinate>();
                    pizzahutShape.add(new GeoCoordinate(37.73954861759737, -121.4367556774805, 0.0));
                    pizzahutShape.add(new GeoCoordinate(37.73954227917106, -121.43642310582709, 0.0));
                    pizzahutShape.add(new GeoCoordinate(37.73925593087524, -121.43643380257618, 0.0));
                    pizzahutShape.add(new GeoCoordinate(37.73924735532325, -121.43674763281797, 0.0));

                    List<GeoCoordinate> dominosShape = new ArrayList<GeoCoordinate>();
                    dominosShape.add(new GeoCoordinate(37.739442604238896, -121.43717674042182, 0.0));
                    dominosShape.add(new GeoCoordinate(37.739442604238896, -121.43687095484256, 0.0));
                    dominosShape.add(new GeoCoordinate(37.73916470683655, -121.43688173999453, 0.0));
                    dominosShape.add(new GeoCoordinate(37.739149792814075, -121.43720352649598, 0.0));

                    GeoPolygon polygon = new GeoPolygon(pizzahutShape);
                    MapPolygon mapPolygon = new MapPolygon(polygon);
                    mapPolygon.setFillColor(Color.argb(70, 0, 255, 0));
                    GeoPolygon geoDominosPolygon = new GeoPolygon(dominosShape);
                    MapPolygon mapDominosPolygon = new MapPolygon(geoDominosPolygon);
                    mapDominosPolygon.setFillColor(Color.argb(70, 0, 0, 255));
                    map.addMapObject(mapPolygon);
                    map.addMapObject(mapDominosPolygon);
                }
            }
        });
    }

}

/*public class MainActivity extends AppCompatActivity {

    private Map map = null;
    private MapFragment mapFragment = null;
    private EditText editText = null;
    private ArrayList<MapObject> markers=null;
    private PositioningManager positioningManager = null;
    private PositioningManager.OnPositionChangedListener positionListener;
    private GeoCoordinate currentPosition = new GeoCoordinate(25.117098499999997, 55.17071);
    private GeoCoordinate oldPosition = new GeoCoordinate(25.117098499999997, 55.17071);
    private MapMarker marker;


*//*    public void dropMarker(String query) {
        if(marker != null) {
            map.removeMapObject(marker);
        }
        GeoCoordinate tracy = new GeoCoordinate( 25.117098499999997, 55.17071);
        GeocodeRequest2 request = new GeocodeRequest2(query).setSearchArea(tracy, 5000);
        request.execute(new ResultListener<List<GeocodeResult>>() {
            @Override
            public void onCompleted(List<GeocodeResult> results, ErrorCode error) {
                if (error != ErrorCode.NONE) {
                    Log.e("HERE", error.toString());
                } else {
                    for (GeocodeResult result : results) {
                        marker = new MapMarker();
                        marker.setCoordinate(new GeoCoordinate(result.getLocation().getCoordinate().getLatitude(), result.getLocation().getCoordinate().getLongitude(), 0.0));
                        map.addMapObject(marker);
                    }
                }
            }
        });
    }*//*

    public void search(String query) {
        if(!markers.isEmpty()) {
            map.removeMapObjects(markers);
            markers.clear();
        }
        try {
            //GeoCoordinate tracy = new GeoCoordinate(25.117098499999997, 55.17071);
            DiscoveryRequest request = new SearchRequest(query).setSearchCenter(currentPosition);
            request.setCollectionSize(5);
            ErrorCode error = request.execute(new ResultListener<DiscoveryResultPage>() {
                @Override
                public void onCompleted(DiscoveryResultPage discoveryResultPage, ErrorCode error) {
                    if (error != ErrorCode.NONE) {
                        Log.e("HERE", error.toString());
                    } else {
                        for(DiscoveryResult discoveryResult : discoveryResultPage.getItems()) {
                            if(discoveryResult.getResultType() == DiscoveryResult.ResultType.PLACE) {
                                PlaceLink placeLink = (PlaceLink) discoveryResult;
                                MapMarker marker = new MapMarker();
                                marker.setCoordinate(placeLink.getPosition());
                                markers.add(marker);
                                map.addMapObjects(markers);
                            }
                        }
                    }
                }
            });
            if( error != ErrorCode.NONE ) {
                Log.e("HERE", error.toString());
            }
        } catch (IllegalArgumentException ex) {
            Log.e("HERE", ex.getMessage());
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        markers = new ArrayList<MapObject>();
        editText = (EditText) findViewById(R.id.query);

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    map = mapFragment.getMap();
                    map.setCenter(new GeoCoordinate(25.117098499999997, 55.17071, 0.0), Map.Animation.NONE);
                    map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                *//*MapMarker defaultMarker = new MapMarker();
                defaultMarker.setCoordinate(new GeoCoordinate(37.7397, -121.4252, 0.0));
                map.addMapObject(defaultMarker);*//*
                    positioningManager = PositioningManager.getInstance();
                    positionListener = new PositioningManager.OnPositionChangedListener() {
                        @Override
                        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position, boolean isMapMatched) {
                            currentPosition = position.getCoordinate();
                            map.setCenter(position.getCoordinate(), Map.Animation.NONE);
                        }

                        @Override
                        public void onPositionFixChanged(PositioningManager.LocationMethod method, PositioningManager.LocationStatus status) {
                        }
                    };
                    try {
                        positioningManager.addListener(new WeakReference<>(positionListener));
                        if (!positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                            Log.e("HERE", "PositioningManager.start: Failed to start...");
                        }
                    } catch (Exception e) {
                        Log.e("HERE", "Caught: " + e.getMessage());
                    }
                    map.getPositionIndicator().setVisible(true);
                }
            }
        });
        editText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    //dropMarker(editText.getText().toString());
                    search(editText.getText().toString());
                    editText.setText("");
                    return true;
                }
                return false;
            }
        });


    }*/

/*    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.query);
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        editText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    dropMarker(editText.getText().toString());
                    editText.setText("");
                    return true;
                }
                return false;
            }
        });
    }*/
        /*editText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    search(editText.getText().toString());
                    editText.setText("");
                    return true;
                }
                return false;
            }
        });*/


/*
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                MapMarker defaultMarker = new MapMarker();
                defaultMarker.setCoordinate(oldPosition);
                if (error == OnEngineInitListener.Error.NONE) {
                    map = mapFragment.getMap();
                    map.setCenter(new GeoCoordinate(37.7397, -121.4252, 0.0), Map.Animation.NONE);
                    map.setZoomLevel(18);
                    positioningManager = PositioningManager.getInstance();
                    positionListener = new PositioningManager.OnPositionChangedListener() {
                        @Override
                        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position, boolean isMapMatched) {
                            currentPosition = position.getCoordinate();
                            if(!currentPosition.equals(oldPosition)) {
                                makeRequest(new GeoCoordinate(position.getCoordinate().getLatitude(), position.getCoordinate().getLongitude()));
                                map.setCenter(position.getCoordinate(), Map.Animation.NONE);
                                oldPosition = currentPosition;
                            }
                        }
                        @Override
                        public void onPositionFixChanged(PositioningManager.LocationMethod method, PositioningManager.LocationStatus status) { }
                    };
                    try {
                        positioningManager.addListener(new WeakReference<>(positionListener));
                        if(!positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                            Log.e("HERE", "PositioningManager.start: Failed to start...");
                        }
                    } catch (Exception e) {
                        Log.e("HERE", "Caught: " + e.getMessage());
                    }
                    map.getPositionIndicator().setVisible(true);

                    //CHANGED LIST TO ARRAYLIST
                    ArrayList<GeoCoordinate> pizzahutShape = new ArrayList<GeoCoordinate>();
                    pizzahutShape.add(new GeoCoordinate(37.73954861759737, -121.4367556774805, 0.0));
                    pizzahutShape.add(new GeoCoordinate(37.73954227917106, -121.43642310582709, 0.0));
                    pizzahutShape.add(new GeoCoordinate(37.73925593087524, -121.43643380257618, 0.0));
                    pizzahutShape.add(new GeoCoordinate(36, -124, 0.0));

                    //CHANGED LIST TO ARRAYLIST
                    ArrayList<GeoCoordinate> dominosShape = new ArrayList<GeoCoordinate>();
                    dominosShape.add(new GeoCoordinate(37.739442604238896, -121.43717674042182, 0.0));
                    dominosShape.add(new GeoCoordinate(37.739442604238896, -121.43687095484256, 0.0));
                    dominosShape.add(new GeoCoordinate(37.73916470683655, -121.43688173999453, 0.0));
                    dominosShape.add(new GeoCoordinate(37.739149792814075, -121.43720352649598, 0.0));

                    GeoPolygon polygon = new GeoPolygon(pizzahutShape);
                    MapPolygon mapPolygon = new MapPolygon(polygon);
                    mapPolygon.setFillColor(Color.argb(70, 0, 255, 0));
                    GeoPolygon geoDominosPolygon = new GeoPolygon(dominosShape);
                    MapPolygon mapDominosPolygon = new MapPolygon(geoDominosPolygon);
                    mapDominosPolygon.setFillColor(Color.argb(70, 0, 0, 255));
                    map.addMapObject(mapPolygon);
                    map.addMapObject(mapDominosPolygon);
                }
                map.addMapObject(defaultMarker);
               */
/* try {
                    Image image = new Image();
                    image.setImageResource(R.drawable.markerjagdish);
                    MapMarker customMarker = new MapMarker(new GeoCoordinate(37.7397, -121.4252, 0.0), image);
                    map.addMapObject(customMarker);
                } catch (Exception e) {
                    Log.e("HERE", e.getMessage());
                }*//*

            }
        });

    }
*/





    /*
    public void makeRequest(GeoCoordinate coords) {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,"https://gfe.api.here.com/2/search/proximity.json?layer_ids=4711&app_id=APP_ID_HERE&app_code=APP_CODE_HERE&proximity=" + coords.getLatitude() + "," + coords.getLongitude() + "&key_attribute=NAME", null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray geometries = response.getJSONArray("geometries");
                    if(geometries.length() > 0) {
                        if(geometries.getJSONObject(0).getJSONObject("attributes").getString("NAME").equals("PIZZA_HUT")) {
                            Log.d("HERE", "Don't eat at Pizza Hut, eat at Dominos.");
                        } else {
                            Log.d("HERE", "Don't eat at Dominos, eat at Pizza Hut.");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("HERE", error.getMessage());
            }
        });
        queue.add(request);
    }

*/


