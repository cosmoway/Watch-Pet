package net.cosmoway.watch_pet_senseor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;

/**
 * Created by susaki on 2016/06/23.
 */
public class SensorService extends Service implements SensorEventListener,GoogleApiClient.ConnectionCallbacks ,
        GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener ,LocationListener {

    final static String TAG = "MyService";

    private GoogleApiClient mClient = null;

    private SensorManager mSensorManager;
    private Sensor mStepDetectorSensor;
    private Sensor mStepConterSensor;

    final Date date = new Date(System.currentTimeMillis());

    private long mTime = (int) (System.currentTimeMillis()/1000/ 60);
    private long mTimeGap = 0;
    private int mStep = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("サービス開始", "onCreate");

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

        // LocationListenerを登録
        mLocationManager.requestLocationUpdates(provider, 0, 0, this);

        mSensorManager.registerListener (this,
                mStepConterSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(this,
                mStepDetectorSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("サービス開始", "onStartCommand");
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("サービス停止", "onDestroy");

        if( mClient != null && mClient.isConnected()){
            mClient.disconnect();
        }

        mSensorManager.unregisterListener(this,mStepConterSensor);
        mSensorManager.unregisterListener(this,mStepDetectorSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("センサー動いた","change");
        Sensor sensor = event.sensor;
        float[] values = event.values;
        long timestamp = event.timestamp;
        //TYPE_STEP_COUNTER
        if(sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            // sensor からの値を取得するなどの処理を行う
            Log.d("サービスステップ",String.valueOf(values[0]));
            if(mTime < (int)(System.currentTimeMillis()/1000/ 60)){
//                mTimeGap = (System.currentTimeMillis()/1000/ 60) - mTime;
//                mTime = (int)(System.currentTimeMillis()/1000/ 60);
//                sendDataByMessageApi(String.valueOf(((values[0] - mStep)/mTimeGap)) , 0);
//                mStep = (int) values[0];
//                Log.d("歩数しょうさい","分＝"+mTime+":ギャップ＝"+mTimeGap+":歩数＝"+mStep);
                sendDataByMessageApi(String.valueOf(values[0]) , 0);
            }
        }
    }

    private void sendDataByMessageApi(final String text , final int flg){
        final int mPathFlg = flg;
        final String s = ""+System.currentTimeMillis()/1000;
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mClient).await();
                for(Node node : nodes.getNodes()){
                    if( mPathFlg == 0 ) {
                        Log.d("サービスsend","歩数");
                        Wearable.MessageApi.sendMessage(mClient, node.getId(), "/step", text.getBytes());
                        Wearable.MessageApi.sendMessage(mClient, node.getId(), "/time", s.getBytes());
                    }else if ( mPathFlg == 1 ){
                        Log.d("サービスflg1","場所");
                        Wearable.MessageApi.sendMessage(mClient, node.getId(), "/location1", text.getBytes());
                    }else if ( mPathFlg == 2 ){
                        Log.d("サービスflg2","場所");
                        Wearable.MessageApi.sendMessage(mClient, node.getId(), "/location2", text.getBytes());
                    }
                }
            }
        }).start();
        Log.d("サービスtime",""+
                (long)(System.currentTimeMillis()/1000/ 60/60/24)+":"+
                (System.currentTimeMillis()/1000/ 60 %60)+":"+
                (System.currentTimeMillis()/1000% 60));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //センサーの精度がかわったとき
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

    @Override
    public void onLocationChanged(Location location) {
        Log.d("サービス位置","かわった");
        Log.d("サービスフラグ１","送信");
        sendDataByMessageApi(String.valueOf(location.getLatitude()) , 1);

        // 経度の表示
        Log.d("サービスフラグ２","送信");
        sendDataByMessageApi(String.valueOf(location.getLongitude()) , 2);

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
