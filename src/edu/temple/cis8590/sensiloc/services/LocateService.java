package edu.temple.cis8590.sensiloc.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.BatteryManager;
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
	private int utfreq;			// Location update frequency
	int rdfreq; 				// Record frequency;
	int turn_delay; 	// Turning delay time
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
			
			// Record time stamp
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			record.timestamp = ts.toString();
			// Location provider
			record.provider = (!method.equals("Adaptive")) ? method : (curStatus == MovingStatus.STRAIGHT) ? "Network" : 
				"GPS";
			// Current battery level
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent battery = getApplicationContext().registerReceiver(null, ifilter);
			int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			record.curBatteryLevel = level;
			// Get location object
			if(method.equals("GPS")) {
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
			if(location != null) {
				record.providerAvailable = true;
				record.location = location;	
			} else {
				record.providerAvailable = false;
				Log.d(SensiLoc.LOG_TAG, "getLastKnownLocation return null");
			}
			// Send request to handler for location record
			Message msg = new Message();
			msg.what = 0;
			msg.obj = (Object)record;
			sdhelper.handler.sendMessage(msg);
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
		if(bundle.containsKey(SensiLoc.KEY_METHOD)) {
			// Get Intent extras
			utfreq = bundle.getInt(SensiLoc.KEY_UTFREQ);
			rdfreq = bundle.getInt(SensiLoc.KEY_RDFREQ);
			method = bundle.getString(SensiLoc.KEY_METHOD);
			String filename = bundle.getString(SensiLoc.KEY_RECORD_FILE);
			Log.d(SensiLoc.LOG_TAG, "LocateService get filename " + filename);
			
			if(method.equals("Adaptive")) {
				turn_delay = bundle.getInt(SensiLoc.KEY_TURN_DELAY);
			}
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
				
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, utfreq*1000, 0, listener);
				curMethod = LocateMethod.LOCATE_GPS;
				
			} else if(method.equals("Network")) {
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, utfreq*1000, 0, listener);
				curMethod = LocateMethod.LOCATE_NETWORK;
				
			} else { // Adaptive 
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, utfreq*1000, 0, listener);
				
				AdaptLocationThread thread = new AdaptLocationThread();
				thread.start();
				curMethod = LocateMethod.LOCATE_ADAPTIVE;
			}
			// Create new record file and record start level
			sdhelper.createFiles(filename);
		/*	Toast.makeText(this, "Locate service:\n\tUpdate frequency: " + utfreq
					+ "\n\tmethod: " + method, Toast.LENGTH_SHORT).show();*/
			
			// Start time task for periodically recording location
			if(locateTimer==null) {
				
				locateTimer = new Timer();
				locateTimer.schedule(locateTask, (utfreq+1)*1000, rdfreq*1000);
				//locateTimer.schedule(locateTask, 0, rdfreq*1000);
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
						
					}, turn_delay*1000);
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
						break;
					case MSG_GPS:
						lm.removeUpdates(listener);
						lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, utfreq*1000, 0, listener);
						Toast.makeText(getApplicationContext(), "get update by GPS", Toast.LENGTH_SHORT).show();
						break;
					case MSG_NETWORK:
						lm.removeUpdates(listener);
						lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, utfreq*1000, 0, listener);
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
		public String provider;
		public Location location;
		public String where;
		public String timestamp;
		public int curBatteryLevel;
		public boolean providerAvailable;
		//public String update_timestamp;
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
		public boolean createFiles(String filename) {
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
					
				/*	fp = new File(sensiDir.getAbsoluteFile()+((curMethod == LocateMethod.LOCATE_GPS) ? "/GPS_Record.txt" :
						(curMethod == LocateMethod.LOCATE_NETWORK) ? "/Network_Record.txt" : "/Adapt_Record.txt") );*/
					fp = new File(sensiDir.getAbsoluteFile() + "/" + filename);
					if(fp.exists()) {
						// Delete existing file
						fp.delete();
					}
					try {
						fp.createNewFile();
						// Record start battery level
						IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
						Intent battery = getApplicationContext().registerReceiver(null, ifilter);
						int startLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
						int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
						FileWriter fw = new FileWriter(fp, true);
						fw.append(String.format("Battery Level: %d: %d\n", startLevel, scale));
						fw.flush();
						
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
			buf.append(record.timestamp); buf.append("\t");		// record time
			buf.append(record.provider); buf.append("\t");		// provider

			buf.append(record.providerAvailable ? latitude : 0); buf.append("\t");				// latitude
			buf.append(record.providerAvailable ? longitude : 0); buf.append("\t");			// longitude

			buf.append(new Timestamp(location.getTime()).toString()); buf.append("\t");		// location fix time
			buf.append(record.curBatteryLevel);		// battery level
			buf.append("\n");
		
			if(fp == null) {
				fp = new File(SDRecordHelper.SD_DIR+((curMethod == LocateMethod.LOCATE_GPS) ? "/GPS_Record.txt" :
					(curMethod == LocateMethod.LOCATE_NETWORK) ? "/Network_Record.txt" : "/Adapt_Record.txt") );
			}
			// Writing records
			if(fp.exists() && fp.canWrite()) {
				//FileOutputStream fos = null;
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
			// Record stop battery level
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent battery = getApplicationContext().registerReceiver(null, ifilter);
			int stoptLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			try {
				FileWriter fw = new FileWriter(fp, true);
				fw.append(String.format("Battery Level: %d: %d\n", stoptLevel, scale));
				fw.flush();
			} catch(IOException e) {
				Log.e(SensiLoc.LOG_TAG, "Exception while writing stop battery level", e);
			}
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
