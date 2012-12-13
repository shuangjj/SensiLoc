package edu.temple.cis8590.sensiloc;


import java.util.Timer;

import java.util.TimerTask;

import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.os.BatteryManager;
import android.os.Handler;

import edu.temple.cis8590.sensiloc.services.*;
public class SensiLoc extends Activity implements OnSharedPreferenceChangeListener {
    public static final String LOG_TAG = "SensiLoc";
    public static final String KEY_METHOD = "sensiloc.methods";
    public static final String KEY_UTFREQ = "sensiloc.utfreq";
    public static final String KEY_RDFREQ = "sensiloc.rdfreq";
    public static final String KEY_TURN_DELAY = "sensiloc.turn_delay";
   
    public static final String KEY_TURN_ANGLE = "sensiloc.turn_angle";
    
    public static final String KEY_MAIN_HANDLE = "sensiloc.main_handle";
    // Layout views 
    EditText et_time;
    EditText et_utfreq;
    EditText et_rdfreq;
    EditText et_turn_delay;
    TextView tv_result;
    Spinner method_spinner;
    Spinner spinner_turning_angle;
    Button but_turn;
    
    String method;
    long exper_time = 0;
    
    boolean enNetworkRequired;
    boolean enGPSRequired;
    // Service intents and status
    private Intent sensiServiceIntent = null;
    private Intent locateServiceIntent=null;
    boolean sensiServiceStarted = false;
    boolean locateServiceStarted = false;
    // Timer for experiment time
    Timer main_timer = null;
    // Battery
    int startLevel = 0;
    int endLevel = 0;
    public Handler mainHandler = null;
    
    Ringtone r = null;
    SharedPreferences sensiPref = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        

		// Ring tone setup
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	    r = RingtoneManager.getRingtone(getApplicationContext(), notification);
	   	
	    
	    
	    Intent prefIntent = new Intent(this, SettingsActivity.class);
		startActivity(prefIntent);
	    // Setup onPreferenceChanged listener for SettingsActivity
	    sensiPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	    sensiPref.registerOnSharedPreferenceChangeListener(this);
	    // Popup setup
    	
	    
       /* // Spinner listing methods population
        method_spinner = (Spinner)findViewById(R.id.spinner_method);
        ArrayAdapter<String> methods = 
        		new ArrayAdapter(this, android.R.layout.simple_spinner_item, this.getResources().getStringArray(R.array.method));
        methods.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        method_spinner.setAdapter(methods);
        
        // Time & Frequency
        et_time = (EditText)findViewById(R.id.editText_time);
        et_utfreq = (EditText)findViewById(R.id.editText_utfreq);
        et_rdfreq = (EditText)findViewById(R.id.editText_rdfreq);
        et_turn_delay = (EditText)findViewById(R.id.editText_turn_delay);
        tv_result = (TextView)findViewById(R.id.textView_result);
        // turning angle spinner adapter setup
        spinner_turning_angle = (Spinner)findViewById(R.id.spinner_turning_angle);
        ArrayAdapter<String> angles = 
        		new ArrayAdapter(this, android.R.layout.simple_spinner_item, 
        				this.getResources().getStringArray(R.array.turning_angles));
        angles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_turning_angle.setAdapter(angles);*/
        // Start button
        Button but_start = (Button)findViewById(R.id.button_start);
        but_start.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
/*				// Experiment time
				if( (et_time !=null) && !et_time.getText().toString().trim().equals("")) {
					exper_time = Integer.valueOf(et_time.getText().toString().trim()).longValue();
				} else {
					// TODO: ~~
					return;
				}
				// Location update frequency and record frequency
				int utfreq = 0;
				if((et_utfreq != null) && !et_utfreq.getText().toString().trim().equals("")) {
					utfreq = Integer.valueOf(et_utfreq.getText().toString().trim()).intValue();
					
				} else {
					// TODO: AlartDialog prompt user set frequency
					return;
				}
				
				int rdfreq = 0;
				if((et_rdfreq != null) && !et_rdfreq.getText().toString().trim().equals("")) {
					rdfreq = Integer.valueOf(et_rdfreq.getText().toString().trim()).intValue();
					
				} else {
					// TODO: AlartDialog prompt user set frequency
					return;
				}
				// Locating method
				method = method_spinner.getSelectedItem().toString();
				// Turning delay time
				int turn_delay = 0;
				int turn_angle = 0;
				if(method.equals("Adaptive")) {
					if((et_turn_delay != null) && !et_turn_delay.getText().toString().trim().equals("")) {
						turn_delay = Integer.valueOf(et_turn_delay.getText().toString().trim()).intValue();
						turn_angle = Integer.valueOf(spinner_turning_angle.getSelectedItem().toString());
					} else {
						// TODO: AlartDialog prompt user set frequency
						return;
					}
				}*/
				// General Preferences
				Boolean manul_enabled = sensiPref.getBoolean("pref_manual_turn", false);
				// Experiment Preferences
				exper_time = Integer.parseInt(sensiPref.getString("pref_exper_time", "0"));
				int utfreq = Integer.parseInt( sensiPref.getString("pref_update_freq", "0") );
				int rdfreq = Integer.parseInt( sensiPref.getString("pref_record_freq", "0") );
				method = sensiPref.getString("pref_list_methods", "GPS");
				// Adaptive Preferences
				int turn_delay = Integer.parseInt( sensiPref.getString("pref_turn_delay", "0") );
				int turn_angle = Integer.parseInt( sensiPref.getString("pref_turn_angle_list", "0") );
				
				Boolean music_enabled = sensiPref.getBoolean("music_checkbox", true);
				Log.d(LOG_TAG, String.format("exper_time: %d\nupdate frequencty: %d\n" +
						"record frequency: %d\nmethod: %s\nturn delay: %d\nturn angle: %d\n" +
						"manual_checkbox: %s\nmusic_checkbox: %s",  
						exper_time, utfreq, rdfreq, method, turn_delay, turn_angle, manul_enabled.toString(),
						music_enabled.toString()));
				
				
				/* Add extras and start location record service */
				locateServiceIntent = new Intent(v.getContext(), LocateService.class);
				Bundle locateBundle = new Bundle();
				
				locateBundle.putInt(KEY_UTFREQ, utfreq);
				locateBundle.putInt(KEY_RDFREQ, rdfreq);
				if(method.equals("Adaptive")) {
					locateBundle.putInt(KEY_TURN_DELAY, turn_delay);
				}
				locateBundle.putString(KEY_METHOD, method);
				locateServiceIntent.putExtras(locateBundle);
				
				/* sensiService */
				sensiServiceIntent = new Intent(v.getContext(), SensiService.class);
				Bundle sensiBundle = new Bundle();
				if(method.equals("Adaptive")) {
					sensiBundle.putInt(KEY_TURN_ANGLE, turn_angle);
				}
				sensiServiceIntent.putExtras(sensiBundle);
				
				/* Check Network and GPS allowance */
				LocationManager lm = (LocationManager) v.getContext().getSystemService(Context.LOCATION_SERVICE);
				boolean isNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
				boolean isGPS = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
				
				enNetworkRequired = !isNetwork && (method.equals("Network") || method.equals("Adaptive"));
				//boolean enNetworkRequired = !isNetwork;
				enGPSRequired = !isGPS && (method.equals("GPS") || method.equals("Adaptive"));
				
				if( enNetworkRequired || enGPSRequired ) {
					// Prompt user to open settings to allow GPS and Network 
					AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext()) 
					.setMessage(((enNetworkRequired && enGPSRequired)?"Network and GPS are disabled":
						(enGPSRequired?"GPS is disabled":"Network is disabled."))+" Would you like to enable?")
					.setCancelable(false) 
					.setPositiveButton("Yes", 
						new DialogInterface.OnClickListener() {	
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Redirect user to setup window
								 //final ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings");
								 final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
								 //intent.addCategory(Intent.CATEGORY_LAUNCHER);
								 //intent.setComponent(toLaunch);
								 //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								 startActivityForResult(intent, 100);
								 dialog.cancel();
								
							}
						}
					)
					.setNegativeButton("No", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							dialog.cancel();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
					
				}else { // !(enNetworkRequired || enGPSRequired)
					
					// Get start battery level
					IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
					Intent battery = getApplicationContext().registerReceiver(null, ifilter);
					startLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
					int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
					
					Log.d(LOG_TAG, String.format("Battery level at start %d: %d", startLevel, scale));
					// Start services
					startService(locateServiceIntent);
					locateServiceStarted = true;
			        
			        if((method != null) && method.equals("Adaptive")) {
			        	startService(sensiServiceIntent);
			        	sensiServiceStarted = true;
			        	// Trigger onSharedPreferenceChanged listener immediately with the preference's default value
			        	onSharedPreferenceChanged(sensiPref, "pref_manual_turn");
			        }
			        
			        // Start timer
			        main_timer = new Timer();
			        main_timer.schedule(new TimerTask() {

						@Override
						public void run() {
							Looper.prepare();
							// Get end battery level
							IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
							Intent battery = getApplicationContext().registerReceiver(null, ifilter);
							endLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
							int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
							
							Log.d(LOG_TAG, String.format("Battery level at end %d: %d ", endLevel, scale));
							//  Stop all running services
							if(locateServiceStarted) {
					    		stopService(locateServiceIntent);	
					    		locateServiceStarted = false;
					    	}
						  	if(sensiServiceStarted) {
						  		stopService(sensiServiceIntent);
						  		sensiServiceStarted = false;
						  	}
						  	if(mainHandler != null) {
						  		mainHandler.sendEmptyMessage(0);
						  	}
						  	//tv_result.setText("Test is over, used battery level " + (endLevel-startLevel));
							Toast.makeText(getApplicationContext(), "Time out, used battery level "
									+ (startLevel-endLevel), Toast.LENGTH_LONG).show();

							Looper.loop();
							
						}
			        	
			        }, exper_time*60*1000);
			        
			        tv_result.setText("Running...");
				}				
				
			}
        	
        });
        // Stop Button
        Button but_stop = (Button)findViewById(R.id.button_stop);
        but_stop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(main_timer != null)
					main_timer.cancel();
			  	if(locateServiceStarted) {
		    		stopService(locateServiceIntent);	
		    		locateServiceStarted = false;
		    	}
			  	if(sensiServiceStarted) {
			  		stopService(sensiServiceIntent);
			  		sensiServiceStarted = false;
			  	}
			  	tv_result.setText("Stopped");
			  	// Test codes
			  /*	SharedPreferences musicPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			  	tv_result.append("\t" + musicPref.getString("example_text", ""));*/
			  	Toast.makeText(v.getContext(), "Services stopped", Toast.LENGTH_SHORT).show();
			}
        	
        });
        // Turn Button
        but_turn = (Button)findViewById(R.id.button_turn);
        
        // Set turning button to invisible on initialization
        but_turn.setVisibility(Button.INVISIBLE);
        but_turn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				 // Check manual options
				 if(!sensiPref.getBoolean("manual_checkbox", true)) {
					 return;
				 }
				// TODO Auto-generated method stub
				// Notify locateService about the  direction changing event
				Intent notifyLocateServiceIntent = new Intent(getApplicationContext(), LocateService.class);
				
				Bundle extras = new Bundle();	
				extras.putInt(LocateService.KEY_MOVING_STATUS, LocateService.VAL_TURNING);
				
				notifyLocateServiceIntent.putExtras(extras);
				startService(notifyLocateServiceIntent);
				// Play notification
				Toast.makeText(getApplicationContext(), "Change direction", Toast.LENGTH_SHORT).show();
				
				if(sensiPref.getBoolean("music_checkbox", true)) {
					if(!r.isPlaying())
						r.play();
				} else {
					Log.d(SensiLoc.LOG_TAG, "music checkbox shared preference returned false");
				}
				
			}
        	
        });
        
        tv_result = (TextView)findViewById(R.id.textView_result);
        // Handler for GUI updates
        mainHandler = new Handler() {
        	@Override
        	public void handleMessage(Message msg) {
        		switch(msg.what) {
        		case 0:
        			tv_result.setText("Test over, battery level used " + (startLevel - endLevel));
        			break;
        		default:
        			;
        		}
        	}
        	
        };
    }
    @Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
    	
		if(key.equals("pref_manual_turn")) {
			if(!sensiServiceStarted) return;
			Boolean manualEnabled = sharedPreferences.getBoolean("pref_manual_turn", false);
			if(manualEnabled) {
				but_turn.setVisibility(Button.VISIBLE);
			} else {
				but_turn.setVisibility(Button.INVISIBLE);
			}
		}
		
	}
    @Override
    public void onResume() 
    {
    	Log.d(LOG_TAG, "SensiLoc activity resumed");
    	// After start LOCATION_SOURCE_SETTING activity, restart sensiService
    	//startService(sensiServiceIntent);

    	super.onResume();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode==100) {
    		if(resultCode==0) {
    			// Check setting results
    			LocationManager lm = (LocationManager) this.getSystemService(Service.LOCATION_SERVICE);
    			boolean isNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
				boolean isGPS = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    			if(enGPSRequired && !isGPS) {
    				Toast.makeText(this, "GPS required!!", Toast.LENGTH_LONG).show();
    				return;
    			}
    			if(enNetworkRequired && !isNetwork) {
    				Toast.makeText(this, "Netwrok required!!", Toast.LENGTH_LONG).show();
    				return;
    			}
    			
    			Toast.makeText(this, "Start services", Toast.LENGTH_SHORT).show();
    			
    			// Get start battery level
				IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
				Intent battery = getApplicationContext().registerReceiver(null, ifilter);
				startLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				
				Log.d(LOG_TAG, String.format("Battery level at start %d: %d", startLevel, scale));
				
				// Start services 
				startService(locateServiceIntent);
				locateServiceStarted = true;
				if((method != null) && method.equals("Adaptive")) {
					
					startService(sensiServiceIntent);
					sensiServiceStarted = true;
					// Trigger onSharedPreferenceChanged listener immediately with the preference's default value
		        	onSharedPreferenceChanged(sensiPref, "manual_checkbox");
					
				}
				
				// Start timer
		        main_timer = new Timer();
		        main_timer.schedule(new TimerTask() {
					@Override
					public void run() {
						Looper.prepare();
						// Get end battery level
						IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
						Intent battery = getApplicationContext().registerReceiver(null, ifilter);
						endLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
						int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
						
						Log.d(LOG_TAG, String.format("Battery level at end %d: %d ", endLevel, scale));
						//  Stop all running services
						if(locateServiceStarted) {
				    		stopService(locateServiceIntent);	
				    		locateServiceStarted = false;
				    	}
					  	if(sensiServiceStarted) {
					  		stopService(sensiServiceIntent);
					  		sensiServiceStarted = false;
					  	}
					  	if(mainHandler != null) {
					  		mainHandler.sendEmptyMessage(0);
					  	}
					  	//tv_result.setText("Test is over, used battery level " + (endLevel-startLevel));
						Toast.makeText(getApplicationContext(), "Test over, used battery level "
								+ (startLevel-endLevel), Toast.LENGTH_LONG).show();
						
						Looper.loop();
						
					}
		        	
		        }, exper_time*60*1000);
		        
		        tv_result.setText("Running...");
    		} else {
    			Toast.makeText(this, "start GPS failed, returned " + resultCode, Toast.LENGTH_SHORT).show();
    		}
    	}
    	super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onStop() {
    	
    	super.onStop();
    }
    @Override
    public void onDestroy() {
    	// Clean up services created
    	if(sensiServiceStarted) {
    		stopService(sensiServiceIntent);
    		sensiServiceStarted = false;
    	}
    	if(locateServiceStarted) {
    		stopService(locateServiceIntent);
    		locateServiceStarted = false;
    	}
    	// remove shared preference value for manual checkbox
    	if(sensiPref != null) {
    		sensiPref.edit().clear().commit();
    	}
    	super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
       
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.menu_settings:
    		Intent prefIntent = new Intent(this, SettingsActivity.class);
    		startActivity(prefIntent);
    		break;
    	default:
    		return false;
    	}
    	return true;
    }

	
}
