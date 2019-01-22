package com.widyz.speedgpsvsaccelerometer;

import android.Manifest;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.text.DecimalFormat;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

  @BindView(R.id.main_activity_start_button) Button startButton;
  @BindView(R.id.main_activity_stop_button) Button stopButton;
  @BindView(R.id.main_activity_text_gps_speed) TextView gpsSpeed;
  @BindView(R.id.main_activity_text_accelerometer_speed) TextView accelerometerSpeed;
  @BindView(R.id.main_activity_text_time) TextView time;
  @BindView(R.id.main_activity_chronometer) Chronometer chronometer;

  private SensorManager mSensorManager;
  private Sensor mAccelerometer;
  private float gravity[] = {0, 0, 0};
  private float linear_acceleration[] = {0, 0, 0};
  float v0 = 0.0f;
  private long pauseOffset;
  private boolean running;
  Location gps = new Location("provider");

  // 1 - STATIC DATA FOR GPS
  private static final String PERMS = Manifest.permission.ACCESS_FINE_LOCATION;
  private static final int RC_GPS_PERMS = 100;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.askPermissionGPS();
    v0 = 0.0f;
    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this); //Configure Butterknife
    chronometer.setFormat("Time: %s");
    chronometer.setBase(SystemClock.elapsedRealtime());

    chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
      @Override
      public void onChronometerTick(Chronometer chronometer) {
        if ((SystemClock.elapsedRealtime() - chronometer.getBase()) >= 10000) {
          chronometer.setBase(SystemClock.elapsedRealtime());
          Toast.makeText(MainActivity.this, "Bing!", Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    // Forward results to EasyPermissions
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
  }


    @AfterPermissionGranted(RC_GPS_PERMS)
    public void askPermissionGPS() {
      if (!EasyPermissions.hasPermissions(this, PERMS)) {
        EasyPermissions.requestPermissions(this,
            getString(R.string.popup_title_permission_files_access), RC_GPS_PERMS, PERMS);
        return;
      }
      Toast.makeText(this, "Vous avez le droit d'accéder au GPS !", Toast.LENGTH_SHORT).show();

  }


  public void startChronometer(View v) {
    if (!running) {
      chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
      chronometer.start();
      running = true;
      v0 = 0.0f;
    }
  }

  public void pauseChronometer(View v) {
    if (running) {
      chronometer.stop();
      pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
      running = false;
    }
  }

  protected void onResume() {
    super.onResume();
    mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
  }

  protected void onPause() {
    super.onPause();
    mSensorManager.unregisterListener(this);
  }

  @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  public void onSensorChanged(SensorEvent event){
    // alpha is calculated as t / (t + dT) with t, the low-pass filter's time-constant and dT, the event delivery rate

    final float alpha = 0.8f;

    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

    linear_acceleration[0] = event.values[0] - gravity[0];
    linear_acceleration[1] = event.values[1] - gravity[1];
    linear_acceleration[2] = event.values[2] - gravity[2];

    DecimalFormat df = new DecimalFormat("#.##");
    long realTime = (SystemClock.elapsedRealtime() - chronometer.getBase())/1000;
    float speedAccelerometer = linear_acceleration[0] + linear_acceleration[1] + linear_acceleration[2] + v0;
    float speed = (realTime*speedAccelerometer) + v0;

    gpsSpeed.setText(gps.getSpeed() + " km/h");
    accelerometerSpeed.setText(df.format(speedAccelerometer)+" m/s² " + df.format(speedAccelerometer*3.6f)+ " km/h \n" + df.format(linear_acceleration[0])+ " " + df.format(linear_acceleration[1]) + " " + df.format(linear_acceleration[2]));
    time.setText(realTime + " secondes" );
    v0 = speedAccelerometer;
  }


}
