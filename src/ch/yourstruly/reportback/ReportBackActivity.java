package ch.yourstruly.reportback;

import java.util.Date;

import ch.yourstruly.reportback.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ReportBackActivity extends Activity {

	private TextView mPilotID;
	private TextView mReportBackNumber;
	private EditText mMessage;
	private EditText mAdditionalPilots;
	private TextView mStatus;
	private TextView mPosition;
	private CheckBox mGoal;
	private CheckBox mRetrieve;
	private ReportBackSender mSender;
	private LocationManager mLocationManager;
	private MyLocationListener mLocationListener;
	private Location mLastLocation;
	private long mLastLocationMillis;
	private boolean mIsGPS_Fix;
	private MyGPS_Listener mGPS_Listener;
	private Button mReportBackButton;
	private SharedPreferences mSettings;

	protected static final long GPS_UPDATE_TIME_INTERVAL = 3000; // millis
	protected static final float GPS_UPDATE_DISTANCE_INTERVAL = 0; // meters
	private static final int REQUEST_CODE_ENABLE_GPS = 0;
	private static final int REQUEST_CODE_SETTINGS = 1;
	protected static final String PILOT_ID = "pilot_id";
	protected static final String REPORT_BACK_NUMBER = "report_back_number";
	protected static final String MESSAGE = "message";
	protected static final String ADDITIONAL_PILOTS = "additional_pilots";
	protected static final String DATE = "date";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		setTitle(R.string.app_name);

		mPilotID = (TextView) findViewById(R.id.v_pilot_id);
		mReportBackNumber = (TextView) findViewById(R.id.v_reportback_number);
		mMessage = (EditText) findViewById(R.id.e_message);
		mAdditionalPilots = (EditText) findViewById(R.id.e_additional_pilots);
		mStatus = (TextView) findViewById(R.id.v_status);
		mPosition = (TextView) findViewById(R.id.v_gps_pos);
		mGoal = (CheckBox) findViewById(R.id.c_goal);
		mRetrieve = (CheckBox) findViewById(R.id.c_retrieve);

		mLocationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			buildAlertMessageNoGps();
		}

		populateFields();
		
		mReportBackButton = (Button) findViewById(R.id.b_reportback);

		mReportBackButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
	            String pilotID = mPilotID.getText().toString();
	            String reportBackNumber = mReportBackNumber.getText().toString();                 
	            if (pilotID.length() > 0 && !pilotID.equals(getText(R.string.not_set).toString()) 
	            	&& reportBackNumber.length() > 0 && !reportBackNumber.equals(getText(R.string.not_set).toString())) 
	            {                
					storeFields();
					mSender = new ReportBackSender((ReportBackActivity)v.getContext());
					mSender.execute(null, null, null);
	            }
	            else 
	            {
	                Toast.makeText(getBaseContext(), getText(R.string.missingInformation), 
	                    Toast.LENGTH_SHORT).show();
	            }
			}
		});
	}

	@Override
	protected void onStop() {
		storeFields();
		
		super.onStop();
	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Yout GPS is disabled, do you want to enable it?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(
									final DialogInterface dialog,
									final int id) {
								Intent intent = new Intent(
										Settings.ACTION_LOCATION_SOURCE_SETTINGS);
								startActivityForResult(intent,
										REQUEST_CODE_ENABLE_GPS);
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						dialog.cancel();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	private void startFetchingLocation() {
		if (mLocationManager != null
				&& mLocationManager
						.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			if (mGPS_Listener == null) {
				mGPS_Listener = new MyGPS_Listener();
			}

			if (mLocationListener == null) {
				mLocationListener = new MyLocationListener();
			}

			mLocationManager.addGpsStatusListener(mGPS_Listener);
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, GPS_UPDATE_TIME_INTERVAL,
					GPS_UPDATE_DISTANCE_INTERVAL, mLocationListener);
		}
	}

	@Override
	protected void onResume() {
		startFetchingLocation();
		super.onResume();
	}

	private void populateFields() {
		mSettings = getPreferences(Context.MODE_PRIVATE);

		mPilotID.setText(mSettings.getString(PILOT_ID, getText(R.string.not_set).toString()));
		mReportBackNumber.setText(mSettings.getString(REPORT_BACK_NUMBER, getText(R.string.not_set).toString()));
		mMessage.setText(mSettings.getString(MESSAGE, ""));
		mAdditionalPilots.setText(mSettings.getString(ADDITIONAL_PILOTS, ""));
	}
	
	private void storeFields() {
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putString(PILOT_ID, mPilotID.getText().toString());
		editor.putString(REPORT_BACK_NUMBER, mReportBackNumber.getText().toString());
		editor.putString(MESSAGE, mMessage.getText().toString());
		editor.putString(ADDITIONAL_PILOTS, mAdditionalPilots.getText().toString());
		editor.putString(DATE, new Date().toGMTString());

		// Commit the edits!
		editor.commit();				
	}

	protected void reportProgress(String pilotID, String position,
			String additionalPilots, String message, String goal, String retrieve, String phoneNumber) {
		mStatus.setText(getText(R.string.sending) + " '" + pilotID + ":" + position + ":" + additionalPilots + ":" + message + goal + retrieve + "' " + getText(R.string.to) + " "
				+ phoneNumber + " ...");
	}

	protected void reportResult(String pilotID, String position,
			String additionalPilots, String message, String goal, String retrieve, String phoneNumber) {
		Date now = new Date();
		mStatus.setText(getText(R.string.sent) + " '" + pilotID + ":" + position + ":" + additionalPilots + ":" + message + goal + retrieve + "' " + getText(R.string.to) + " "
				+ phoneNumber + " " + getText(R.string.at) + " "
				+ now.toGMTString());
	}
	
	protected void reportFailure(String reason)
	{
		mStatus.setText(String.format(getText(R.string.failure).toString(), reason));
	}

	protected String getPilotID() {
		return mPilotID.getText().toString();
	}

	protected String getAdditionalPilots() {
		return mAdditionalPilots.getText().toString();
	}

	protected String getMessage() {
		return mMessage.getText().toString();
	}

	protected String getPhoneNumber() {
		return mReportBackNumber.getText().toString();
	}
	
	protected String getPosition() {
		return Double.toString(mLastLocation.getLatitude()) + " " + Double.toString(mLastLocation.getLongitude()); 
	}

	protected String getGoal() {
		return mGoal.isChecked() ? ":@goal" : "";
	}
	
	protected String getRetrieve() {
		return mRetrieve.isChecked() ? ":@noRetrieve" : "";
	}

	protected boolean isGPS_Fix() {
		return mIsGPS_Fix;
	}
	
	private class MyGPS_Listener implements GpsStatus.Listener {
		public void onGpsStatusChanged(int event) {
			switch (event) {
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				if (mLastLocation != null)
				{
					mIsGPS_Fix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < (GPS_UPDATE_TIME_INTERVAL * 2);
					if (mIsGPS_Fix)
					{
						mPosition.setText(getPosition());
					}
					else
					{
						mPosition.setText(R.string.waitingForGPS);
					}
				}
				break;

			case GpsStatus.GPS_EVENT_FIRST_FIX:
				mIsGPS_Fix = false;
				mPosition.setText(R.string.waitingForGPS);
				break;
			case GpsStatus.GPS_EVENT_STARTED:
				mIsGPS_Fix = false;
				mPosition.setText(R.string.waitingForGPS);
				break;
			case GpsStatus.GPS_EVENT_STOPPED:
				mPosition.setText(R.string.waitingForGPS);
				mIsGPS_Fix = false;
				break;
			}
		}
	}

	private class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
			if (location != null) {
				mLastLocationMillis = SystemClock.elapsedRealtime();
				// do some things here
				mLastLocation = location;
			}
		}

		public void onProviderDisabled(String provider) {
			// imgGpsState.setImageDrawable(ctx.getResources().getDrawable(R.drawable.gps_on_red));
		}

		public void onProviderEnabled(String provider) {
			// imgGpsState.setImageDrawable(ctx.getResources().getDrawable(R.drawable.gps_on_orange));
		}

		// this doesn't trigger on Android 2.x users say
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	protected void reportNoGPS_Fix() {
		mStatus.setText(getText(R.string.waitingForGPS));
	}

	protected void reportUnableGetGPS_Fix() {
		mStatus.setText(getText(R.string.noGPS));
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) 
	{
		switch (requestCode)
		{
			case (REQUEST_CODE_ENABLE_GPS):
				if (resultCode == Activity.RESULT_OK) 
				{
					String provider = Settings.Secure.getString(getContentResolver(),
							Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
					if (provider != null) {
						startFetchingLocation();
					} else {
						// User did not switch on the GPS
					}
				}
				break;
			case (REQUEST_CODE_SETTINGS):
				if (resultCode == Activity.RESULT_OK)
				{
					Bundle data = intent.getExtras();
					String pilotID = data.getString(PILOT_ID);
					if (pilotID.length() > 0)
						mPilotID.setText(pilotID);
					
					String reportBackNumber = data.getString(REPORT_BACK_NUMBER);
					if (reportBackNumber.length() > 0)
						mReportBackNumber.setText(reportBackNumber);
							
					storeFields();
				}
				break;
		}
	}

	protected void disableAll() {
		mReportBackButton.setEnabled(false);
		mGoal.setEnabled(false);
		mRetrieve.setEnabled(false);
		mAdditionalPilots.setEnabled(false);
		mMessage.setEnabled(false);
	}
	
	protected void enableAll() {
		mReportBackButton.setEnabled(true);
		mGoal.setEnabled(true);
		mRetrieve.setEnabled(true);
		mAdditionalPilots.setEnabled(true);
		mMessage.setEnabled(true);
	}

	protected void reportTooLong(int tooLong) {
		mStatus.setText(String.format(getText(R.string.tooLong).toString(), new Integer(tooLong)));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		return mReportBackButton.isEnabled();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.settings:
	        displaySettings();
	        return true;
	    case R.id.about:
	        displayAbout();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	private void displayAbout()
	{
		try
		{
			AlertDialog about = AboutDialogBuilder.create(this);
			about.show();
		}
		catch(Exception e)
		{
			Toast.makeText(getBaseContext(), getText(R.string.generalError),
					Toast.LENGTH_SHORT).show();
		}
	}
	
	private void displaySettings()
	{
		Intent intent = new Intent(this, ReportBackPreferences.class);
		
		String pilotID = mPilotID.getText().toString();
		if (pilotID.equals(getText(R.string.not_set).toString()))
			pilotID = "";
		
		String reportBackNumber = mReportBackNumber.getText().toString();
		if (reportBackNumber.equals(getText(R.string.not_set).toString()))
			reportBackNumber = "";
		
		Bundle data = new Bundle();
		data.putString(PILOT_ID, pilotID);
		data.putString(REPORT_BACK_NUMBER, reportBackNumber);
		
		intent.putExtras(data);
		
		startActivityForResult(intent, REQUEST_CODE_SETTINGS);
	}
}