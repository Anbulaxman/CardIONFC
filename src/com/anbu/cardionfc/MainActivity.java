package com.anbu.cardionfc;

import java.nio.charset.Charset;

import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback
{
	// You MUST register with card.io to get an app token. Go to https://card.io/apps/new/
    private static final String MY_CARDIO_APP_TOKEN = "4345df03111746b2bb33e112a314c4b8";

	final String TAG = getClass().getName();

	private Button scanButton;
	private TextView resultTextView;

	private int MY_SCAN_REQUEST_CODE = 100; // arbitrary int
	
	NfcAdapter mNfcAdapter;
    TextView mInfoText;
    private static final int MESSAGE_SENT = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		resultTextView = (TextView)findViewById(R.id.resultTextView);
		scanButton = (Button)findViewById(R.id.scanButton);
		
	//	resultTextView.setText("card.io library version: " + CardIOActivity.sdkVersion() + "\nBuilt: " + CardIOActivity.sdkBuildDate());
		
		mInfoText = (TextView) findViewById(R.id.textView);
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            mInfoText = (TextView) findViewById(R.id.textView);
            mInfoText.setText("NFC is not available on this device.");
        }
        // Register callback to set NDEF message
        mNfcAdapter.setNdefPushMessageCallback(this, this);
        // Register callback to listen for message-sent success
        mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (CardIOActivity.canReadCardWithCamera(this)) {
			scanButton.setText("Scan a credit card with card.io");
		}
		else {
			scanButton.setText("Enter credit card information");
		}
		
		 if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
	            processIntent(getIntent());
	        }
	}

	public void onScanPress(View v) {
		// This method is set up as an onClick handler in the layout xml
		// e.g. android:onClick="onScanPress"

		Intent scanIntent = new Intent(this, CardIOActivity.class);

		// required for authentication with card.io
		scanIntent.putExtra(CardIOActivity.EXTRA_APP_TOKEN, MY_CARDIO_APP_TOKEN);

		// customize these values to suit your needs.
		scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, true); // default: true
		scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false); // default: false
		scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false); // default: false

		// hides the manual entry button
		// if set, developers should provide their own manual entry mechanism in the app
		scanIntent.putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, true); // default: false

		// MY_SCAN_REQUEST_CODE is arbitrary and is only used within this activity.
		startActivityForResult(scanIntent, MY_SCAN_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		String resultStr;
		if (data != null && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
			CreditCard scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);

			// Never log a raw card number. Avoid displaying it, but if necessary use getFormattedCardNumber()
			resultStr = "Card Number: " + scanResult.getRedactedCardNumber() + "\n";

			// Do something with the raw number, e.g.:
			// myService.setCardNumber( scanResult.cardNumber );

			if (scanResult.isExpiryValid()) {
				resultStr += "Expiration Date: " + scanResult.expiryMonth + "/" + scanResult.expiryYear + "\n"; 
			}

			if (scanResult.cvv != null) { 
				// Never log or display a CVV
				resultStr += "CVV has " + scanResult.cvv.length() + " digits.\n";
			}

			if (scanResult.postalCode != null) {
				resultStr += "Postal Code: " + scanResult.postalCode + "\n";
			}
		}
		else {
			resultStr = "Scan was canceled.";
		}
		resultTextView.setText(resultStr);

	}
	
	 @Override
	    public NdefMessage createNdefMessage(NfcEvent event) {
	        Time time = new Time();
	        time.setToNow();
//	        String text = ("Beam me up!\n\n" +
//	                "Beam Time: " + time.format("%H:%M:%S"));
	        String text = resultTextView.getText().toString();
	        NdefMessage msg = new NdefMessage(
	                new NdefRecord[] { createMimeRecord(
	                        "application/com.anbu.android.beam", text.getBytes())
	         /**
	          * The Android Application Record (AAR) is commented out. When a device
	          * receives a push with an AAR in it, the application specified in the AAR
	          * is guaranteed to run. The AAR overrides the tag dispatch system.
	          * You can add it back in to guarantee that this
	          * activity starts when receiving a beamed message. For now, this code
	          * uses the tag dispatch system.
	          */
	          //,NdefRecord.createApplicationRecord("com.example.android.beam")
	        });
	        return msg;
	    }

	    /**
	     * Implementation for the OnNdefPushCompleteCallback interface
	     */
	    @Override
	    public void onNdefPushComplete(NfcEvent arg0) {
	        // A handler is needed to send messages to the activity when this
	        // callback occurs, because it happens from a binder thread
	        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
	    }

	    /** This handler receives a message from onNdefPushComplete */
	    private final Handler mHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
	            case MESSAGE_SENT:
	                Toast.makeText(getApplicationContext(), "Message sent!", Toast.LENGTH_LONG).show();
	                break;
	            }
	        }
	    };
	    
	    @Override
	    public void onNewIntent(Intent intent) {
	        // onResume gets called after this to handle the intent
	        setIntent(intent);
	    }

	    /**
	     * Parses the NDEF Message from the intent and prints to the TextView
	     */
	    void processIntent(Intent intent) {
	        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
	                NfcAdapter.EXTRA_NDEF_MESSAGES);
	        // only one message sent during the beam
	        NdefMessage msg = (NdefMessage) rawMsgs[0];
	        // record 0 contains the MIME type, record 1 is the AAR, if present
	        mInfoText.setText(new String(msg.getRecords()[0].getPayload()));
	    }

	    /**
	     * Creates a custom MIME type encapsulated in an NDEF record
	     *
	     * @param mimeType
	     */
	    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
	        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
	        NdefRecord mimeRecord = new NdefRecord(
	                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
	        return mimeRecord;
	    }

	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        // If NFC is not available, we won't be needing this menu
	        if (mNfcAdapter == null) {
	            return super.onCreateOptionsMenu(menu);
	        }
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.options, menu);
	        return true;
	    }

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	            case R.id.menu_settings:
	                Intent intent = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
	                startActivity(intent);
	                return true;
	            default:
	                return super.onOptionsItemSelected(item);
	        }
	    }

}

