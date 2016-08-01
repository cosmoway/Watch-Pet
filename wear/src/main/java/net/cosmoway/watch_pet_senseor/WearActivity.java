package net.cosmoway.watch_pet_senseor;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;

public class WearActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    private GoogleApiClient mClient = null;

    private TextView mText;
    private ImageView mImage;
    LayoutParams lp = null;
    MarginLayoutParams mlp = null;

    private double mLatitude = 0;
    private double mLongitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_activity_wear);

        mText = (TextView) findViewById(R.id.text);
        mImage = (ImageView)findViewById(R.id.image1);
        lp = mImage.getLayoutParams();
        mlp = (ViewGroup.MarginLayoutParams)lp;

        mClient = new GoogleApiClient
                .Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mClient != null && mClient.isConnected()) {
            Wearable.MessageApi.removeListener(mClient, (MessageApi.MessageListener) this);
            mClient.disconnect();
        }
    }

    public void onMessageReceived(final MessageEvent message){
        Log.d(message.getPath(),new String(message.getData()));
        if( message.getPath().equals("/step")){
            Log.d("OK","テキスト変更");
            String s = new String(message.getData());
            double d = Double.parseDouble(s);
            int i = (int)d;
            this.mText.setText("１分の歩数："+s);
            if ( i % 2 == 0 ){
                mImage.setImageResource(R.drawable.chips);
            }else{
                mImage.setImageResource(R.drawable.stones);
            }
        }

        if( message.getPath().equals("/location1")){
            Log.d("位置情報1 ",new String(message.getData()));
            double d = Double.parseDouble(String.valueOf(ByteBuffer.wrap(message.getData()).getDouble()));
            if ( mLatitude < d) {
                mlp.setMargins(mlp.leftMargin, 15, mlp.rightMargin, 0);
            }else{
                mlp.setMargins(mlp.leftMargin, 0, mlp.rightMargin, 15);
            }
            mLatitude = d;
            //マージンを設定
            mImage.setLayoutParams(mlp);
        }

        if( message.getPath().equals("/location2")){
            Log.d("位置情報2 ",new String(message.getData()));
            double d = Double.parseDouble(String.valueOf(ByteBuffer.wrap(message.getData()).getDouble()));
            if( mLongitude < d ) {
                Log.d("位置情報２","だ");
                mlp.setMargins(mlp.topMargin, 15, mlp.bottomMargin, 0);
            }else{
                Log.d("位置情報２","よ");
                mlp.setMargins(mlp.topMargin, 0, mlp.bottomMargin, 15);
            }
            mLongitude = d;
            //マージンを設定
            mImage.setLayoutParams(mlp);
        }
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
}
