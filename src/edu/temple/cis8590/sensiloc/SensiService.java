package edu.temple.cis8590.sensiloc;

import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class SensiService extends Service implements SensorEventListener{
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
    int angleDelta = 0;
	@Override
	public IBinder onBind(Intent i) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void onCreate() {
		// Sensors initialization
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> sens = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        mAcceler = (sens.size() == 0) ? null : sens.get(0);
        
        sens = sm.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
        mMagnetic = (sens.size() == 0) ? null : sens.get(0);
        
        if( (mAcceler != null) && (mMagnetic != null) ){
        	
        	sm.registerListener(this, mAcceler, SensorManager.SENSOR_DELAY_NORMAL);
        	sm.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        	Log.d(SensiLoc.LOG_TAG, "regiseter event listener for acelerometer and magnetic");
        } else {
        	Log.d(SensiLoc.LOG_TAG, String.format("Sensor(s) missing:\n\taccelerometer: ") + mAcceler + 
        			String.format("\n\tmagnetic field:") + mMagnetic);
        	
        }
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
		}
		if(mLastAccelerSet && mLastMagneticSet) {
			SensorManager.getRotationMatrix(mRotationMatrix, null, mLastAccelerValue, mLastMagneticValue);
			//SensorManager.getRotationMatrix(mTempMatrix, null, mLastAccelerValue, mLastMagneticValue);
			//SensorManager.remapCoordinateSystem(mTempMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mRotationMatrix);
			SensorManager.getOrientation(mRotationMatrix, mOrientation);
			int azimuth = Math.round(mOrientation[0]*rad2deg);
			if(azimuth<0) {
				azimuth = 360+azimuth;
			}
			if(lastOrientationSet) {
				int delta = azimuth-lastAzimuth;
				angleDelta += delta;
				if(Math.abs(angleDelta)>THRESHOLD) {
					angleDelta = 0;
					Toast.makeText(this, "Change direction", Toast.LENGTH_SHORT).show();
					// Notify locateService about the  direction changing event
					Intent notifyLocateServiceIntent = new Intent(this, LocateService.class);
					
					Bundle extras = new Bundle();	
					extras.putInt(LocateService.KEY_MOVING_STATUS, LocateService.VAL_TURNING);
					
					notifyLocateServiceIntent.putExtras(extras);
					startService(notifyLocateServiceIntent);
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