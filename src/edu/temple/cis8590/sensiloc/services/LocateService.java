package edu.temple.cis8590.sensiloc.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import edu.temple.cis8590.sensiloc.*;
public class LocateService extends Service {
	private static final String LOG_TAG = "LocateService";
	// User setup from SensiLoc Activity
	private int freq;
	private String method;
	
	
	
	private static int PRECISION = 3;


	SDRecordHelper sdhelper = null;
	
	private LocationManager lm;
	private Timer locateTimer = null;
	
	private Handler adaptHandler = null;
	// Moving status key and value for extras
	public static final String KEY_MOVING_STATUS = "locateservice.moving";
	public static final int VAL_TURNING = 1;
	// Messages sent to AdaptThread Handler
	public static final int MSG_QUIT = 0;
	public static final int MSG_GPS = 1;
	public static final int MSG_NETWORK = 2;
	// Moving status enumeration
	enum MovingStatus{
		TURNING,
		STRAIGHT
		}; 
	// Localization method enumeration
	enum LocateMethod {
		LOCATE_GPS,
		LOCATE_NETWORK,
		LOCATE_ADAPTIVE
	}
	
	private MovingStatus curStatus;
	LocateMethod curMethod;
	
	public LocateService() {
	}
	Timer turn_timer;
	// Running location recording task
	public LocationListener listener = new LocationListener() {
		
		@Override
		public void onLocationChanged(Location curLocation) {

			Log.d(LOG_TAG, "Location updated "+curLocation.getLongitude()+":"+curLocation.getLatitude());
			/*Toast.makeText(getApplicationContext(), "New location " + curLocation.getLongitude() + ":"
					+ curLocation.getLatitude(), Toast.LENGTH_LONG).show();*/
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String provider, int status,
				Bundle extras) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	/*
	 * Timer Task
	 * Periodically get last known location from respective location provider
	 */
	TimerTask locateTask = new TimerTask() {
		@Override
		public void run() {
			Location location = null;
			MyLocationRecord  record = new MyLocationRecord();
			if(method.equals("GPS")) {
				//Intent gpsUpdateIntent = new Intent();
				//final PendingIntent launchIntent = PendingIntent.getBroadcast(LocateService.this, 5000, gpsUpdateIntent, 0);
				//lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, );
				//lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, );

				location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				
				
			} else if(method.equals("Network")) {
				location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				
			} else if(method.equals("Adaptive")) {
				// Get location based on the current moving status
				if(curStatus == MovingStatus.STRAIGHT) {
					location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				} else if(curStatus == MovingStatus.TURNING) {
					location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				}
				
			}
			// Send request for location record
			if(location != null) {
				record.location = location;
				record.where = method;
				Message msg = new Message();
				msg.what = 0;
				msg.obj = (Object)record;
				sdhelper.handler.sendMessage(msg);
				
			} else {
				
			}
			
		}
	};
	/*
	 * Override onCreate initializing LocateService
	 */
	@Override
	public void onCreate() {
		
		//
		sdhelper = new SDRecordHelper();
		
		// Location Manager
		lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		// Get available location providers
		List<String> providers = lm.getProviders(true);
		Log.i(LOG_TAG, "Available location providers "+providers.size());
		for(String provider : providers) {
			Log.i(LOG_TAG, "provider: " + provider);
		}
		// Initialize current moving status
		curStatus = MovingStatus.STRAIGHT;
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
	/*
	 * Override onStartCommand to process incoming intents
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle bundle = intent.getExtras();
		
		// Intents from Main Activity
		if(bundle.containsKey(SensiLoc.KEY_FREQ)) {
			// Get Intent extras
			freq = bundle.getInt(SensiLoc.KEY_FREQ);
			method = bundle.getString(SensiLoc.KEY_METHOD);
			
			// Start HandlerThread for location recording
			if(sdhelper == null) {
				sdhelper = new SDRecordHelper();
				//sdhelper.createFiles();
			}
			if(!sdhelper.t.isAlive()) {
				sdhelper.t.start();
			}
			// Create record files and  request update
			if(method.equals("GPS")) {
				
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, freq, 0, listener);
				curMethod = LocateMethod.LOCATE_GPS;
				
			} else if(method.equals("Network")) {
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, freq, 0, listener);
				curMethod = LocateMethod.LOCATE_NETWORK;
				
			} else { // Adaptive 
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
				
				AdaptLocationThread thread = new AdaptLocationThread();
				thread.start();
				curMethod = LocateMethod.LOCATE_ADAPTIVE;
			}
			
			sdhelper.createFiles();
			Toast.makeText(this, "Start service: Frequency: "+freq+", method: "+method, Toast.LENGTH_SHORT).show();
			
			
			
			// Start time task for periodically recording location
			if(locateTimer==null) {
				
				locateTimer = new Timer();
				locateTimer.schedule(locateTask, freq, freq);
			}
		} else {
			// Intent from SensiService
			// Change location source
			int moving_status = bundle.getInt(KEY_MOVING_STATUS);
			Log.i(LOG_TAG, "SensiService moving status " + moving_status);
			
			if(method.equals("Adaptive") && (moving_status == VAL_TURNING)) {
				
				if(curStatus == MovingStatus.STRAIGHT) {
					curStatus = MovingStatus.TURNING;
					adaptHandler.sendEmptyMessage(MSG_GPS);
					
					turn_timer = new Timer();
					// Recover to request location update by network after 2 minutes
					turn_timer.schedule(new TimerTask() {
	
						@Override
						public void run() {
							adaptHandler.sendEmptyMessage(MSG_NETWORK);
							curStatus = MovingStatus.STRAIGHT;
						}
						
					}, 1*1000*60);
				}
			} // VAL_TURNING
		}
		
		super.onStartCommand(intent, flags, startId);
		return START_REDELIVER_INTENT;
	}

	/*
	 * Thread for handling changing location update source
	 */
	class AdaptLocationThread extends Thread {
		@Override
		public void run() {
			Looper.prepare();
			// Handler handling messages from TimerTask
			Log.d(LOG_TAG, "AdaptionLocationUpdate Thread run");
			
			adaptHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch(msg.what) {
					case MSG_QUIT:
						Looper.myLooper().quit();
						Log.i(LOG_TAG, "Looper quit()");
						break;
					case MSG_GPS:
						lm.removeUpdates(listener);
						lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
						Toast.makeText(getApplicationContext(), "get update by GPS", Toast.LENGTH_SHORT).show();
						break;
					case MSG_NETWORK:
						lm.removeUpdates(listener);
						lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
						Toast.makeText(getApplicationContext(), "get update by Network", Toast.LENGTH_SHORT).show();
						break;
					default:
						;	
					}
				}
			};
			Looper.loop();
		}
	}

	/*
	 * Class for location record
	 */
	class MyLocationRecord {
		public Location location;
		public String where;
		public MyLocationRecord() {
			
		}
		
	}
	/*
	 * Helper class for writing location record into SD card
	 */
	class SDRecordHelper  implements Callback {
		
		File fp = null;
		
		HandlerThread t;
		Handler handler;
		static final String SD_DIR = "/sdcard/sensiloc/";
		public SDRecordHelper() {
			
			// Initialize HandlerThread
			t = new HandlerThread("Location Record");
			t.start();
			Looper looper = t.getLooper();
			// TODO: looper ? null
			handler = new Handler(looper, this);
		}
		/*
		 * Create record files on SD card
		 */
		public boolean createFiles() {
			// Create files in SD card for location recording
			File sdDir = new File("/sdcard/");
			if(sdDir.exists() && sdDir.canWrite()) {
				File sensiDir = new File(sdDir.getAbsolutePath()+"/sensiloc/");
				if(sensiDir.isDirectory()) {
					// Delete old files if exist
					/*for(File f:sensiDir.listFiles()) {
						f.delete();
					}*/
					
				}
				else sensiDir.mkdir();
				// Create files and assign corresponding file handler
				if(sensiDir.exists() && sensiDir.canWrite()) {
					
					fp = new File(sensiDir.getAbsoluteFile()+((curMethod == LocateMethod.LOCATE_GPS) ? "/GPS_Record.txt" :
						(curMethod == LocateMethod.LOCATE_NETWORK) ? "/Network_Record.txt" : "/Adapt_Record.txt") );
					
					if(fp.exists()) {
						fp.delete();
					}
					try {
						fp.createNewFile();
					}catch(IOException e) {
						Log.e(LOG_TAG, "Error creating files under "+sensiDir.getPath(), e);
						return false;
					} 

				} else { // sensiDir.exists()
					Log.e(LOG_TAG, "unable to write to /sdcard/sensiloc/");
					return false;
				}
			} else { // sdDir.exists
				Log.e(LOG_TAG, "/sdcard not available");
				return false;
			}
			return true;
		}
		/*
		 * 
		 */
		public void sendLocationRecord(MyLocationRecord record) {

				Message msg = new Message();
				msg.what = 0;
				msg.obj = (Object)record;
				handler.sendMessage(msg);

		}
		private boolean writeLocationRecord(MyLocationRecord record)
		{
			Location location = record.location;
			double longitude = location.getLongitude(); //Math.round(location.getLongitude()*Math.pow(10, PRECISION))/Math.pow(10, PRECISION);
			double latitude = location.getLatitude(); //Math.round(location.getLatitude()*Math.pow(10, PRECISION))/Math.pow(10, PRECISION);
			Log.i(LocateService.LOG_TAG, "get location by "+ (!method.equals("Adaptive") ? method
					: ((curStatus == MovingStatus.STRAIGHT) ? "Network" : "GPS")) + " ("+longitude
					+ ", " + latitude + ")");
			// Fill record buffer
			StringBuffer buf = new StringBuffer();
			buf.append(longitude);
			buf.append("\t");
			buf.append(latitude);
			buf.append("\n");
		
			if(fp == null) {
				fp = new File(SDRecordHelper.SD_DIR+((curMethod == LocateMethod.LOCATE_GPS) ? "/GPS_Record.txt" :
					(curMethod == LocateMethod.LOCATE_NETWORK) ? "/Network_Record.txt" : "/Adapt_Record.txt") );
			}
			// Writing records
			if(fp.exists() && fp.canWrite()) {
				FileOutputStream fos = null;
				FileWriter fw = null;
				BufferedWriter bw = null;
				try {
					fw = new FileWriter(fp, true);
					bw = new BufferedWriter(fw);
					bw.write(buf.toString());
					
				} catch (IOException e) {
					Log.e(SensiLoc.LOG_TAG, "Error writing ...", e);
					return false;
				} finally {
					if(bw != null) {
						try {
							bw.close();
							fw.close();
						} catch(IOException e) {
							// TODO: swallow
						}
					}

					
				}
				
			}else { // gpsFp.exists()
				Log.e(SensiLoc.LOG_TAG, "Error writing to file");
				return false;
			}
			return true;
		}
		
		@Override
		public boolean handleMessage(Message msg) {
			switch(msg.what) {
			case 0:
				MyLocationRecord rd = (MyLocationRecord) msg.obj;
				writeLocationRecord(rd);
				break;
			default:
				return false;
			}
			
			return true;
		}
		/*
		 * Quit handler
		 */
		public void stopHandler() {
			t.quit();
		}
	}
	
	@Override
	public void onDestroy() {
		Log.d(SensiLoc.LOG_TAG, "LocateService to be destroyed");
		locateTimer.cancel();
		locateTimer.purge();
		locateTimer = null;
		
		if(turn_timer != null) {
			turn_timer.cancel();
			turn_timer.purge();
			turn_timer = null;
		}
		
		lm.removeUpdates(listener);
		// Quit looper for changing locating method
		if(adaptHandler!=null)
			adaptHandler.sendEmptyMessage(0);
		// Quit thread handler for recording into SD card
		if(sdhelper != null) {
			sdhelper.stopHandler();
		
		}
	
		super.onDestroy();
	}
}
