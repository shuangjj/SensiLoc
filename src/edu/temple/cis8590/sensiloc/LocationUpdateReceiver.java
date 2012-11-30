package edu.temple.cis8590.sensiloc;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

public class LocationUpdateReceiver extends BroadcastReceiver {
	public LocationUpdateReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO: This method is called when the BroadcastReceiver is receiving
		// an Intent broadcast.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
