package net.sturmen.quiet.hours;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class QuietHours extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiet_hours);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_quiet_hours, menu);
        return true;
    }
}
