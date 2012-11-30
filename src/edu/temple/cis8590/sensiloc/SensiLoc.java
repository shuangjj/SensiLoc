package edu.temple.cis8590.sensiloc;


import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SensiLoc extends Activity {
    public static final String LOG_TAG = "SensiLoc";
    public static final String KEY_METHOD = "sensiloc.methods";
    public static final String KEY_FREQ = "sensiloc.freq";
  
    
    private Intent sensiServiceIntent = null;
    private Intent locateServiceIntent=null;
    // Layout views 
    EditText et_time;
    EditText et_freq;
    Spinner method_spinner;
    
    boolean enNetworkRequired;
    boolean enGPSRequired;
    
    String method;
    
    boolean sensiServiceStarted = false;
    boolean locateServiceStarted = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        

        
        // Spinner listing methods population
        method_spinner = (Spinner)findViewById(R.id.spinner_method);
        ArrayAdapter<String> methods = 
        		new ArrayAdapter(this, android.R.layout.simple_spinner_item, this.getResources().getStringArray(R.array.method));
        methods.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        method_spinner.setAdapter(methods);
        
        // Time & Frequency
        et_time = (EditText)findViewById(R.id.editText_time);
        et_freq = (EditText)findViewById(R.id.editText_freq);
        
        // Start button
        Button but_start = (Button)findViewById(R.id.button_start);
        but_start.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO: validate inputs
				int freq = 0;
				if((et_freq != null) && !et_freq.getText().toString().trim().equals("")) {
					Log.d(SensiLoc.LOG_TAG, "Freq: "+et_freq.getText().toString().trim()+".");
					freq = new Integer(et_freq.getText().toString());
				}
				
				method = method_spinner.getSelectedItem().toString();
				Log.d(SensiLoc.LOG_TAG, "Put (freq, "+freq+") (method, "+method+") to Intent");
				
				/* Add extras and start location record service */
				locateServiceIntent = new Intent(v.getContext(), LocateService.class);
				Bundle locateBundle = new Bundle();
				locateBundle.putInt(KEY_FREQ, freq);
				locateBundle.putString(KEY_METHOD, method);
				locateServiceIntent.putExtras(locateBundle);
				/* sensiService */
				sensiServiceIntent = new Intent(v.getContext(), SensiService.class);
				
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
								 final ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings");
								 final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
								 intent.addCategory(Intent.CATEGORY_LAUNCHER);
								 intent.setComponent(toLaunch);
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
					
				}else {
					
					startService(locateServiceIntent);
					locateServiceStarted = true;
			        // Start orientation service
			        if((method != null) && method.equals("Adaptive")) {
			        	startService(sensiServiceIntent);
			        	sensiServiceStarted = true;
			        }
				}				
				
			}
        	
        });
        // Stop Button
        Button but_stop = (Button)findViewById(R.id.button_stop);
        but_stop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
			  	if(locateServiceStarted) {
		    		stopService(locateServiceIntent);	
		    		locateServiceStarted = false;
		    	}
			  	if(sensiServiceStarted) {
			  		stopService(sensiServiceIntent);
			  		sensiServiceStarted = false;
			  	}
			  	Toast.makeText(v.getContext(), "Services stopped", Toast.LENGTH_SHORT).show();
			}
        	
        });
        
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
    			// TODO: check setting results
    			if(enGPSRequired)
    				;
    			String providers = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
    			Log.i(LOG_TAG, "providers allowed by user "+providers);		
    			Toast.makeText(this, "start GPS", Toast.LENGTH_SHORT).show();
				startService(locateServiceIntent);
				locateServiceStarted = true;
				if((method != null) && method.equals("Adaptive")) {
					startService(sensiServiceIntent);
					sensiServiceStarted = false;
				}
    		} else {
    			Toast.makeText(this, "start GPS failed, returned " + resultCode, Toast.LENGTH_SHORT).show();
    		}
    	}
    	super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onStop() {
    	// Clean up services created
    	if(sensiServiceStarted) {
    		stopService(sensiServiceIntent);
    		sensiServiceStarted = false;
    	}
    	if(locateServiceStarted) {
    		stopService(locateServiceIntent);
    		locateServiceStarted = false;
    	}
    	super.onStop();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
       
    }
}
