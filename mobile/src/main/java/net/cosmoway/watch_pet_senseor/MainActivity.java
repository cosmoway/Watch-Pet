package net.cosmoway.watch_pet_senseor;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks ,
        GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener{

    private GoogleApiClient mClient = null;

    private SensorManager mSensorManager;
    private Sensor mStepDetectorSensor;
    private Sensor mStepConterSensor;

    private TextView mTimeview;
    final Date date = new Date(System.currentTimeMillis());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClient = new GoogleApiClient
                .Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mClient.connect();

        //センサーマネージャを取得
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //センサマネージャから TYPE_STEP_DETECTOR についての情報を取得する
        mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        //センサマネージャから TYPE_STEP_COUNTER についての情報を取得する
        mStepConterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // LocationManagerを取得
        LocationManager mLocationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Criteriaオブジェクトを生成
        Criteria criteria = new Criteria();

        // Accuracyを指定(低精度)
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);

        // PowerRequirementを指定(低消費電力)
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        // ロケーションプロバイダの取得
        String provider = mLocationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        startService(new Intent(getBaseContext(),SensorService.class));
    }


    @Override
    protected void onResume() {
        super.onResume();

        mClient.connect();

    }

    @Override
    protected void onPause() {
        super.onPause();

        if( mClient != null && mClient.isConnected()){
            mClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("handheld", "onDestroy");
    }

    private void sendDataByMessageApi(final String text , final int flg){
        Log.d("send","ほ");
        final int mPathFlg = flg;
        final String s = ""+System.currentTimeMillis()/1000;
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mClient).await();
                for(Node node : nodes.getNodes()){
                    if( mPathFlg == 0 ) {
                        Log.d("send","歩数");
                        Wearable.MessageApi.sendMessage(mClient, node.getId(), "/step", text.getBytes());
                        Wearable.MessageApi.sendMessage(mClient, node.getId(), "/time", s.getBytes());
                    }else if ( mPathFlg == 1 ){
                        Log.d("flg1","場所");
                        Wearable.MessageApi.sendMessage(mClient, node.getId(), "/location1", text.getBytes());
                    }else if ( mPathFlg == 2 ){
                        Log.d("flg2","場所");
                        Wearable.MessageApi.sendMessage(mClient, node.getId(), "/location2", text.getBytes());
                    }
                }
            }
        }).start();
        mTimeview = (TextView)findViewById(R.id.time);
        Log.d("time",""+System.currentTimeMillis()/1000);
        mTimeview.setText(""+
                (long)(System.currentTimeMillis()/1000/ 60/60)+":"+
                (System.currentTimeMillis()/1000/ 60 %60)+":"+
                (System.currentTimeMillis()/1000% 60));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.MessageApi.addListener(mClient, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }
}
