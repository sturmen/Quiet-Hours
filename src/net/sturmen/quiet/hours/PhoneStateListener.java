/*
   Copyright 2012, Nick Tinsley

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package net.sturmen.quiet.hours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.telephony.TelephonyManager;
import android.text.format.Time;
//import android.util.Log;
import android.widget.Toast;

public class PhoneStateListener extends BroadcastReceiver{ 
	//initialize projection; required for content querying
	private static final String[] mProjection = 
			new String[] {
		CalendarContract.Events._ID,
		CalendarContract.Events.TITLE,
		CalendarContract.Events.DESCRIPTION,
		CalendarContract.Events.DTSTART,
		CalendarContract.Events.DTEND,
		CalendarContract.Events.AVAILABILITY,
	};
	//define the vibrate keyphrase as a CharSequence
	private CharSequence vibrate = "_vibrate".subSequence(0,7);
	//define the silent keyphrase as a CharSequence
	private CharSequence silent = "_silent".subSequence(0,6);
	//globally define the debug tag
//	private static final String tag = "QUIETHOURS";
	private static int original = 0;
	private static boolean toastEnabled;

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean("pref_key_enabled", true)) {
			toastEnabled = prefs.getBoolean("pref_key_toast", true);
			//Log.d(tag, "Toasts are " + toastEnabled);
			//initialize the audio manager
			AudioManager ringer = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			if (intent.getAction().equals("android.intent.action.PHONE_STATE"))
			{
				//Log.d(tag, "Phone state changed.");
				//gather up the intent's extras into a bundle
				Bundle extras = intent.getExtras();
				//sanity check: if they exist...
				if (extras != null) {
					//get the state of the telephone
					String state = extras.getString(TelephonyManager.EXTRA_STATE);
					//Log.d(tag, state);
					if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)){ 
						//store the previous phone state so we can return to it.
						if (original == 0) original = ringer.getRingerMode();
						//Log.d(tag, "Original stored as " + state);
						//and then this is where the magic happens...
						getCurrent(context, ringer);
					}
				}
			}
			else if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
			{
				//Log.d(tag, "SMS received.");
				if (original == 0) original = ringer.getRingerMode();
				getCurrent(context, ringer);
			}
		}
	}

	public void getCurrent(Context context, AudioManager ringer){
		//initialize an empty time
		Time time = new Time();
		//set the time to now
		time.setToNow();
		//convert the current time to milliseconds, ignoring Daylight Savings Time
		long now = time.toMillis(true);
		//Log.d(tag, "" + now);
		//define what we want in a calendar event. One that starts before now AND ends after now.
		String mSelectionClause = CalendarContract.Events.DTSTART + " <= " + now + " AND " + now + " <= " + CalendarContract.Events.DTEND;
		//Log.d(tag, "mSelectionClause =" +  mSelectionClause);
		//we want the results sorted by which started first, just because
		String mSortOrder = CalendarContract.Events.DTSTART + " ASC";
		//Log.d(tag, "mSortOrder =" + mSortOrder);
		//query the calendar provider and get the resulting cursor
		Cursor mCursor = context.getContentResolver().query(CalendarContract.Events.CONTENT_URI, mProjection, mSelectionClause, null, mSortOrder);
		//Move to first: must be done or everything dies
		mCursor.moveToFirst();
		if (mCursor.getCount() < 1) {
			original = 0;
			//Log.d(tag, "No events. Putting on back.");
		} else {
			//evaluate the first one
			evaluate(context, ringer, mCursor);
			//evaluate the rest of them
			while(mCursor.moveToNext()){
				evaluate(context, ringer, mCursor);
			}
		}
		mCursor.close();
	}
	public void evaluate(Context context, AudioManager ringer, Cursor cursor){
		//store the event's title as a variable for easy access later
		String title = cursor.getString(cursor.getColumnIndex("TITLE"));
		//check the description and title for the silent keyphrase
		if (cursor.getString(cursor.getColumnIndex("DESCRIPTION")).contains(silent) || cursor.getString(cursor.getColumnIndex("TITLE")).contains(silent))
		{
			//if it's there, change to silent
			ringer.setRingerMode(AudioManager.RINGER_MODE_SILENT);
			//Log.d(tag, "Current event: " + title + ". Set to silent! (" + ringer.getRingerMode() + ")");
			//send notification to user
			if (toastEnabled) Toast.makeText(context, "Current event: " + title + "\nSet to silent.", Toast.LENGTH_SHORT).show();
		}
		//check the description and title for the vibrate keyphrase
		else if (cursor.getString(cursor.getColumnIndex("DESCRIPTION")).contains(vibrate) || cursor.getString(cursor.getColumnIndex("TITLE")).contains(vibrate)) {
			//same, but do not override silent with vibrate!
			if (ringer.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
				ringer.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				//Log.d(tag, "Current event: " + title + ". Set to vibrate!(" + ringer.getRingerMode() + ")");
				//send notification to user
				if (toastEnabled) Toast.makeText(context, "Current event: " + title + "\nSet to vibrate.", Toast.LENGTH_SHORT).show();
			}
		}
		else if (cursor.getColumnIndex("AVAILIBILITY") == (CalendarContract.Events.AVAILABILITY_BUSY)) {
			ringer.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
			if (toastEnabled) Toast.makeText(context, "Current event: " + title + "\nSet to vibrate.", Toast.LENGTH_SHORT).show();
		}
		else
		{
			if (original != 0) 
			{
				ringer.setRingerMode(original);
				//Log.d(tag, "Phone returned to " + original);
				original = 0;
			}
		}
	}

}
