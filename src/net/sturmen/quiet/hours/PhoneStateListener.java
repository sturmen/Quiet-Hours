package net.sturmen.quiet.hours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;

public class PhoneStateListener extends BroadcastReceiver{

	private Cursor mCursor = null;
	private static final String[] mProjection =
			new String[] {
		CalendarContract.Events._ID,
		CalendarContract.Events.TITLE,
		CalendarContract.Events.DESCRIPTION,
		CalendarContract.Events.DTSTART,
		CalendarContract.Events.DTEND
	};
	private CharSequence vibrate = "_vibrate".subSequence(0,7);
	private CharSequence silent = "_silent".subSequence(0,6);
	private static final String tag = "QUIETHOURS";

	@Override
	public void onReceive(Context context, Intent intent) {
		AudioManager ringer = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		Bundle extras = intent.getExtras();
		if (extras != null) {
			String state = extras.getString(TelephonyManager.EXTRA_STATE);
			Log.d(tag, state);
			if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
				ringer.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				Log.d(tag, "Phone returned to normal.");
			} else 
				getCurrent(context, ringer);
		}
	}



	public void getCurrent(Context context, AudioManager ringer){
		Time time = new Time();
		time.setToNow();
		long now = time.toMillis(true);
		Log.d(tag, "" + now);
		String mSelectionClause = CalendarContract.Events.DTSTART + " <= " + now + " AND " + now + " <= " + CalendarContract.Events.DTEND;
		Log.d(tag, "mSelectionClause =" +  mSelectionClause);
		String[] mSelectionArgs = {""};
		mSelectionArgs[0] = "Vibrate";
		Log.d(tag, mSelectionArgs[0]);
		String mSortOrder = CalendarContract.Events.DTSTART + " ASC";
		Log.d(tag, "mSortOrder =" + mSortOrder);
		mCursor = context.getContentResolver().query(CalendarContract.Events.CONTENT_URI, mProjection, mSelectionClause, null, mSortOrder);
		mCursor.moveToFirst();
		if (null == mCursor) {
			Log.d(tag, "ERROR!");
			// If the Cursor is empty, the provider found no matches
		} else if (mCursor.getCount() < 1) {
			Log.d(tag, "No events. Putting on back.");
		} else {
			evaluate(ringer, mCursor);
			while(mCursor.moveToNext()){
				evaluate(ringer, mCursor);
			}
		}
	}
	public void evaluate(AudioManager ringer, Cursor cursor){
		if (cursor.getString(cursor.getColumnIndex("DESCRIPTION")).contains(silent))
		{
			ringer.setRingerMode(AudioManager.RINGER_MODE_SILENT);
			Log.d(tag, "Current event: " + cursor.getString(cursor.getColumnIndex("TITLE")) + ". Set to silent!");
		}
		else if (cursor.getString(cursor.getColumnIndex("DESCRIPTION")).contains(vibrate)) {
			ringer.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
			Log.d(tag, "Current event: " + cursor.getString(cursor.getColumnIndex("TITLE")) + ". Set to vibrate!");
		}
	}

}
