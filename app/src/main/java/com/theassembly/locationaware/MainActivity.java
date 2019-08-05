package com.theassembly.locationaware;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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

import static org.locationtech.jts.util.Debug.print;

public class MainActivity extends AppCompatActivity {

    private Map map = null;
    private MapFragment mapFragment = null;
    private PositioningManager positioningManager = null;
    private PositioningManager.OnPositionChangedListener positionListener;
    private GeoCoordinate currentPosition = new GeoCoordinate(25.107785, 55.165478);
    private GeoCoordinate oldPosition = new GeoCoordinate(37.7397, -121.4252);


    public void makeRequest(GeoCoordinate coords) {
        RequestQueue queue = Volley.newRequestQueue(this);
        final String code;
        Log.d("Debug", "MakeRequest");
        print("Coordinates: "+coords);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,"https://gfe.api.here.com/2/search/proximity.json?layer_ids=4711&app_id=14tVYqHaHTOGwQTUno2z&app_code=xTSD3c7c3rkh1MjUNMnZtg&proximity=" + coords.getLatitude() + "," + coords.getLongitude() + "&key_attribute=NAME", null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d("tag", "OnResponseCalled");
                    JSONArray geometries = response.getJSONArray("geometries");
                    if(geometries.length() >0 ){
                    if(geometries.getJSONObject(0).getJSONObject("attributes").getString("NAME").equals("PIZZA_HUT")){
                        Log.d("tag", "IN5");
                       String code1 = "A" ;
                        Notification(code1);

                      //  Notification();
                    }

                       else {
                        String code1 = "B" ;
                        Notification(code1);
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
                    map.setCenter(new GeoCoordinate(25.107785, 55.165478, 0.0), Map.Animation.NONE);
                    map.setZoomLevel(18);
                    //map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                    positioningManager = PositioningManager.getInstance();
                    positionListener = new PositioningManager.OnPositionChangedListener() {

                        @Override
                        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position, boolean isMapMatched) {
                            currentPosition = position.getCoordinate();
                            Log.d("Debug", "OnPositionUpdated");
                            if(!currentPosition.equals(oldPosition)) {
                                Log.d("Debug", "Inside Function");
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

                    List<GeoCoordinate> pizzahutShape = new ArrayList<GeoCoordinate>();

                    pizzahutShape.add(new GeoCoordinate(25.107785, 55.165478, 0.0));
                    pizzahutShape.add(new GeoCoordinate(25.108073, 55.165254, 0.0));
                    pizzahutShape.add(new GeoCoordinate(25.107751, 55.164961, 0.0));
                    pizzahutShape.add(new GeoCoordinate(25.107561, 55.165163, 0.0));


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


    public void  Notification( String locationcode) {

        Log.d("Debug", "Notification");

        if(locationcode == "A"){
            Toast.makeText(this,"Welcome To IN- 5", Toast.LENGTH_LONG).show();
        }
        else if(locationcode == "B"){
            Toast.makeText(this,"Welcome To Random", Toast.LENGTH_LONG).show();
        }
        else    {
            Toast.makeText(this,"Location Not Known ", Toast.LENGTH_LONG).show();
        }


    }
}

