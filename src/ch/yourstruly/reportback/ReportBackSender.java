package ch.yourstruly.reportback;

import ch.yourstruly.reportback.R;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.telephony.SmsManager;

public class ReportBackSender extends AsyncTask<Void, Void, Void> {

	private static final int MAX_SMS_LENGTH = 160;
	private static final String SENT = "SMS_SENT";
	private static final String DELIVERED = "SMS_DELIVERED";

	private ReportBackActivity mView;
	private String mPilotID;
	private String mAdditionalPilots;
	private String mMessage;
	private String mPhoneNumber;
	private String mPosition;
	private String mGoal;
	private String mRetrieve;
	private int mTooLong = 0;
	private String mFailureReason;

	protected ReportBackSender(ReportBackActivity view) {
		super();
		mView = view;
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		for (int i = 0; i < 10; i++) {
			if (mView.isGPS_Fix()) {
				mPosition = mView.getPosition();
				sendSMS(mPilotID, mPosition, mAdditionalPilots, mMessage,
						mGoal, mRetrieve, mPhoneNumber);
				break;
			} else {
				try {
					Thread.sleep(ReportBackActivity.GPS_UPDATE_TIME_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}

	private void sendSMS(String pilotID, String position,
			String additionalPilots, String message, String goal,
			String retrieve, String phoneNumber) {
		String text = mView.getText(R.string.prefix).toString() + ":" + pilotID
				+ ":" + position + ":" + additionalPilots + ":" + message
				+ goal + retrieve;
		if (text.length() > MAX_SMS_LENGTH) {
			mTooLong = text.length() - MAX_SMS_LENGTH;
		} else {
			PendingIntent sentPI = PendingIntent.getBroadcast(mView, 0,
					new Intent(SENT), 0);

			PendingIntent deliveredPI = PendingIntent.getBroadcast(mView, 0,
					new Intent(DELIVERED), 0);

			// ---when the SMS has been sent---
			mView.registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context arg0, Intent arg1) {
					switch (getResultCode()) {
					case Activity.RESULT_OK:
						mFailureReason = "";
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
						mFailureReason = "Generic failure";
						break;
					case SmsManager.RESULT_ERROR_NO_SERVICE:
						mFailureReason = "No service";
						break;
					case SmsManager.RESULT_ERROR_NULL_PDU:
						mFailureReason = "Null PDU";
						break;
					case SmsManager.RESULT_ERROR_RADIO_OFF:
						mFailureReason = "Phone is in Airplane Mode";
						break;
					}
				}
			}, new IntentFilter(SENT));

			// ---when the SMS has been delivered---
			mView.registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context arg0, Intent arg1) {
					switch (getResultCode()) {
					case Activity.RESULT_OK:
						mFailureReason = "";
						break;
					case Activity.RESULT_CANCELED:
						mFailureReason = "SMS not delivered";
						break;
					}
				}
			}, new IntentFilter(DELIVERED));

			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(phoneNumber, null, text, sentPI, deliveredPI);
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		if (mPosition != null) {
			if (mTooLong > 0) {
				mView.reportTooLong(mTooLong);
			} else {
				if (mFailureReason != null && mFailureReason != "") {
					mView.reportFailure(mFailureReason);
				} else {
					mView.reportResult(mPilotID, mPosition, mAdditionalPilots,
							mMessage, mGoal, mRetrieve, mPhoneNumber);
				}
			}
		} else {
			mView.reportUnableGetGPS_Fix();
		}
		mView.enableAll();
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		mView.disableAll();

		mPilotID = mView.getPilotID();
		mAdditionalPilots = mView.getAdditionalPilots();
		mMessage = mView.getMessage();
		mPhoneNumber = mView.getPhoneNumber();
		mGoal = mView.getGoal();
		mRetrieve = mView.getRetrieve();

		if (mView.isGPS_Fix()) {
			mPosition = mView.getPosition();
			mView.reportProgress(mPilotID, mPosition, mAdditionalPilots,
					mMessage, mGoal, mRetrieve, mPhoneNumber);
		} else {
			mView.reportNoGPS_Fix();
		}
	}
}