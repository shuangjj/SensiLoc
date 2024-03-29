package edu.temple.cis8590.sensiloc.services;

import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import edu.temple.cis8590.sensiloc.*;
public class SensiService extends Service implements SensorEventListener{
	Ringtone r = null;
	private SensorManager sm;
    private Sensor mAcceler;
    private Sensor mMagnetic;

    private float[] mLastAccelerValue = new float[3];
    private float[] mLastMagneticValue = new float[3];
    private boolean mLastAccelerSet = false;
    private boolean mLastMagneticSet = false;
    
    private float[] mRotationMatrix = new float[9];
    private float[] mTempMatrix = new float[9];
    private float[] mOrientation = new float[3];
    
    private int lastAzimuth, lastPitch, lastRoll;
    private boolean lastOrientationSet = false;
    private final float rad2deg = 180/(float)Math.PI;
    private int THRESHOLD = 90;
    int ANGLE_FILTER_HIGH = 250;
    int ANGLE_FILTER_LOW = 5;
    int turn_angle = 0;
    int angleDelta = 0;
    
    SharedPreferences musicPref = null;
	@Override
	public IBinder onBind(Intent i) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void onCreate() {
		// Ringtone setup
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	    r = RingtoneManager.getRingtone(getApplicationContext(), notification);
	    musicPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		// Sensors initialization
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> sens = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        mAcceler = (sens.size() == 0) ? null : sens.get(0);
        
        sens = sm.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
        mMagnetic = (sens.size() == 0) ? null : sens.get(0);
        
        
        
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle bundle = intent.getExtras();
		turn_angle = bundle.getInt(SensiLoc.KEY_TURN_ANGLE);
		Log.d(SensiLoc.LOG_TAG, "Set turn_angle from SensiLoc to " + turn_angle);
		
		
		if( (mAcceler != null) && (mMagnetic != null) ){
        	
        	sm.registerListener(this, mAcceler, SensorManager.SENSOR_DELAY_NORMAL);
        	sm.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        	Log.d(SensiLoc.LOG_TAG, "regiseter event listener for acelerometer and magnetic");
        } else {
        	Log.d(SensiLoc.LOG_TAG, String.format("Sensor(s) missing:\n\taccelerometer: ") + mAcceler + 
        			String.format("\n\tmagnetic field:") + mMagnetic);
        	
        }
		
		super.onStartCommand(intent, flags, startId);
		return START_REDELIVER_INTENT;
	}
	@Override
	public void onDestroy() {
		Log.d(SensiLoc.LOG_TAG, "SensiService ready to be destroyed and unregister sensor listener");
		sm.unregisterListener(this);
		super.onDestroy();
		
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if(event.sensor == mAcceler) {
			System.arraycopy(event.values, 0, mLastAccelerValue, 0, event.values.length);
			mLastAccelerSet = true;
			
		} else if(event.sensor == mMagnetic) {
			System.arraycopy(event.values, 0, mLastMagneticValue, 0, event.values.length);
			mLastMagneticSet = true;
			//Log.d(SensiLoc.LOG_TAG, String.format("Geomagnetic Field Sensor: %f", mLastMagneticValue[1]));
		}
		if(mLastAccelerSet && mLastMagneticSet) {
			boolean ret = SensorManager.getRotationMatrix(mRotationMatrix, null, mLastAccelerValue, mLastMagneticValue);
			if(!ret) {
				Log.e(SensiLoc.LOG_TAG, "getRotationMatrix failed");
				return;
			}
			SensorManager.getRotationMatrix(mTempMatrix, null, mLastAccelerValue, mLastMagneticValue);
			// Remap coordinate system so that y-axis of device coordinate system aligns with the 
			// z-axis of the orientation world coordinate system
			SensorManager.remapCoordinateSystem(mTempMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
			SensorManager.getOrientation(mRotationMatrix, mOrientation);
			
			int azimuth = Math.round(mOrientation[0]*rad2deg);
			if(azimuth<0) {
				azimuth = 360+azimuth;
			}
			if(lastOrientationSet) {
				int delta = azimuth-lastAzimuth;
				// Filter noise
				if(Math.abs(delta) < ANGLE_FILTER_LOW || Math.abs(delta) > ANGLE_FILTER_HIGH) {
					delta = 0;
				}
				angleDelta += delta;
				if(Math.abs(angleDelta)>turn_angle) {
					angleDelta = 0;
					
					if(musicPref.getBoolean("pref_manual_turn", true)) {
						Log.d(SensiLoc.LOG_TAG, "manual_checkbox returned true");
						 return;
					}
					Log.d(SensiLoc.LOG_TAG, "manual_checkbox returned false");
					// Notify locateService about the  direction changing event
					Intent notifyLocateServiceIntent = new Intent(this, LocateService.class);
					
					Bundle extras = new Bundle();	
					extras.putInt(LocateService.KEY_MOVING_STATUS, LocateService.VAL_TURNING);
					
					notifyLocateServiceIntent.putExtras(extras);
					startService(notifyLocateServiceIntent);
					Toast.makeText(this, "Change direction", Toast.LENGTH_SHORT).show();
					// Audio Notification
					if(musicPref.getBoolean("music_checkbox", true)) {
						if(!r.isPlaying())
							r.play();
					} else {
						Log.d(SensiLoc.LOG_TAG, "music checkbox shared preference returned false");
					}
				    
				}
			}
			lastAzimuth = azimuth;//Math.round(mOrientation[0]*rad2deg);
			lastPitch = Math.round(mOrientation[1]*rad2deg);
			lastRoll = Math.round(mOrientation[2]*rad2deg);
			lastOrientationSet = true;
			
			Log.d(SensiLoc.LOG_TAG, String.format("Orientation: %d %d %d, delta: %d", lastAzimuth, 
					lastPitch, lastRoll, angleDelta));
			
		}
	}

}
