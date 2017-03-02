package ch.epfl.ndubois.whoisit;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    ToggleButton btnStartStopService;
    EditText inputApiUrl;
    Button btnSaveApiUrl;

    SharedPreferences sharedPref;
    Intent intentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find UI elements
        btnStartStopService = (ToggleButton)findViewById(R.id.btnStartStopService);
        inputApiUrl = (EditText)findViewById(R.id.inputApiUrl);
        btnSaveApiUrl = (Button)findViewById(R.id.btnSaveApiUrl);

        intentService = new Intent(this, WhoIsItService.class);
        btnStartStopService.setChecked(isMyServiceRunning(WhoIsItService.class));
        btnStartStopService.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (btnStartStopService.isChecked()) {
                    startService(intentService);
                } else {
                    stopService(intentService);
                }
            }
        });

        sharedPref = this.getSharedPreferences(this.getPackageName() + "_prefs", Context.MODE_PRIVATE);
        //sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        btnSaveApiUrl.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.pref_apiUrl), inputApiUrl.getText().toString());
                editor.commit();
            }
        });

        inputApiUrl.setText(sharedPref.getString(getString(R.string.pref_apiUrl), "http://"));

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
