package ch.yourstruly.reportback;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ReportBackPreferences extends Activity {

	private EditText mPilotID;
	private EditText mReportBackNumber;
	private Button mSaveButton;
	private Button mCancelButton;
	private Button mBrowseButton;

	private static final int REQUEST_CODE_BROWSE_FOR_NUMBER = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);
		setTitle(getText(R.string.app_name).toString() + " "
				+ getText(R.string.settings).toString());

		mPilotID = (EditText) findViewById(R.id.e_pilot_id);
		mReportBackNumber = (EditText) findViewById(R.id.e_reportback_number);
		mBrowseButton = (Button) findViewById(R.id.b_browse);

		Bundle data = getIntent().getExtras();
		mPilotID.setText(data.getString(ReportBackActivity.PILOT_ID));
		mReportBackNumber.setText(data
				.getString(ReportBackActivity.REPORT_BACK_NUMBER));

		mSaveButton = (Button) findViewById(R.id.b_save);
		mCancelButton = (Button) findViewById(R.id.b_cancel);

		mBrowseButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_PICK,
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
				startActivityForResult(intent, REQUEST_CODE_BROWSE_FOR_NUMBER);
			}
		});

		mSaveButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Intent intent = new Intent();
				Bundle data = new Bundle();
				data.putString(ReportBackActivity.PILOT_ID, mPilotID.getText()
						.toString());
				data.putString(ReportBackActivity.REPORT_BACK_NUMBER,
						mReportBackNumber.getText().toString());

				intent.putExtras(data);
				setResult(RESULT_OK, intent);
				finish();
			}
		});

		mCancelButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

	}

	@Override
	protected void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			switch (reqCode) {
			case REQUEST_CODE_BROWSE_FOR_NUMBER:
				Cursor cursor = null;
				String number = "";
				try {
					Uri result = data.getData();

					// get the id from the uri
					String id = result.getLastPathSegment();

					// query
					cursor = getContentResolver().query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							null, BaseColumns._ID + " = ? ",
							new String[] { id }, null);

					int numberIdx = cursor.getColumnIndex(Phone.DATA);

					if (cursor.moveToFirst()) {
						number = cursor.getString(numberIdx);
					} else {
						// WE FAILED
					}
				} catch (Exception e) {
					Toast.makeText(getBaseContext(),
							getText(R.string.generalError), Toast.LENGTH_SHORT)
							.show();
				} finally {
					if (cursor != null) {
						cursor.close();
					}
					mReportBackNumber.setText(number);
				}
				break;
			default:
				// do nothing
			}
		}
	}
}