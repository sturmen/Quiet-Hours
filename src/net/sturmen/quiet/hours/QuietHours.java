package net.sturmen.quiet.hours;

import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.View;




public class QuietHours extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_quiet_hours);
		FragmentTransaction transaction = getFragmentManager().beginTransaction().replace(android.R.id.content, new IntroFragment());
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_quiet_hours, menu);
		return true;
	}
	public void openSettings(View view){
		// Display the fragment as the main content.
		FragmentTransaction transaction = getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment());
		transaction.addToBackStack(null);
		transaction.commit();
	}
}
