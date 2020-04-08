package br.rcx.updatercontacts;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    public static String filterBroadCastMessageIn = "message-in";
    public static String filterBroadCastMessageOut = "message-out";

    public LocalBroadcastManager lbm;
    public ArrayList<String> arrayListMessages=new ArrayList<String>();
    public ArrayAdapter<String> adapter;

    public ListView listView = null;
    public static ContentResolver ctx;

    public static String hostValue="";
    public static String authValue="";
    public static boolean startstop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mMessageReceiver, new IntentFilter(MainActivity.filterBroadCastMessageIn));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.listLog);
        adapter = new ArrayAdapter<String>(this, R.layout.simple_row, arrayListMessages);
        listView.setAdapter(adapter);

        addMessageToList("Iniciando app.");

        try {
            startService(new Intent(MainActivity.this, UpdaterService.class));
        }catch(Exception e){
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO,"[MainActivity][onCreate] "+ e.getMessage());
        }

       ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, 12345);
        ctx = getContentResolver();

        TextView host = findViewById(R.id.serverUrlInput);
        TextView auth = findViewById(R.id.serverAuthInput);

        hostValue = getPreference("host");
        authValue = getPreference("auth");

        if(hostValue != null) {
            host.setText(hostValue);
        }

        if(authValue != null) {
            auth.setText(authValue);
        }

        Button saveButton = findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v){
                TextView host = findViewById(R.id.serverUrlInput);
                TextView auth = findViewById(R.id.serverAuthInput);
                if(host.getText().toString().equals("")){
                    showSaveMessage("Campo host não pode ser vazio");
                }else if(auth.getText().toString().equals("")){
                    showSaveMessage("Campo auth não pode ser vazio");
                }else{
                    hostValue = host.getText().toString();
                    authValue = auth.getText().toString();

                    setPreference("host",hostValue);
                    setPreference("auth",authValue);

                    showSaveMessage("Configurações salvas!");
                    addMessageToList("Configurações salvas!");
                }

            }
        });

        startstop = getPreferenceBoolean("startstop");
        Button startStopButton = findViewById(R.id.startstop);
        if(startstop){
            startStopButton.setText("Parar!");
        }else{
            startStopButton.setText("Iniciar!");
        }
        startStopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v){

                startstop = !startstop;
                setPreference("startstop",startstop);
                if(startstop) {
                    showSaveMessage("Iniciando rest!");
                    addMessageToList("Iniciando rest!");
                }else{
                    showSaveMessage("Parando rest!");
                    addMessageToList("Parando rest!");
                }

                Button startStopButton = findViewById(R.id.startstop);
                if(startstop){
                    startStopButton.setText("Parar!");
                }else{
                    startStopButton.setText("Iniciar!");
                }
            }
        });

        Button clearlogButton = findViewById(R.id.clearlog);
        clearlogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                arrayListMessages.clear();
                ((ArrayAdapter<String>) listView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    public void setPreference(String key,String value){
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void setPreference(String key,Boolean value){
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public String getPreference(String key){
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getString(key,null);
    }

    public boolean getPreferenceBoolean(String key){
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key,false);
    }

    public void showSaveMessage(String message) {
        Toast.makeText(MainActivity.this, message , Toast.LENGTH_SHORT).show();
    }


    public void addMessageToList(String message){
        String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
        arrayListMessages.add("["+currentDateTimeString+"] "+message);

        if(arrayListMessages.size() > 20) {
            arrayListMessages.clear();
        }
        ((ArrayAdapter<String>) listView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
        recreate();
    }


    //RECEBE DO UPDATER SERVICE MSG - COMO NAO PODE ALTERAR A THREAD DO SERVIÇO
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent){
            //recebe message que vier pelo brodcast do socket
            String message = intent.getStringExtra("message");
            addMessageToList(message);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            arrayListMessages.clear();
            ((ArrayAdapter<String>) listView.getAdapter()).notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
