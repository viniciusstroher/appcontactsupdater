package br.rcx.updatercontacts;

import android.Manifest;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    public static String filterBroadCastMessage = "message";

    public LocalBroadcastManager lbm;
    public ArrayList<String> arrayListMessages=new ArrayList<String>();
    public ArrayAdapter<String> adapter;

    public ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mMessageReceiver, new IntentFilter(MainActivity.filterBroadCastMessage));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        listView = (ListView) findViewById(R.id.listLog);
        adapter = new ArrayAdapter<String>(this, R.layout.simple_row, arrayListMessages);
        listView.setAdapter(adapter);

        addMessageToList("Iniciando app.");

        try {
            startService(new Intent(MainActivity.this, UpdaterService.class));
        }catch(Exception e){
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO,"[MainActivity][onCreate] "+ e.getMessage());
        }

        try {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, 12345);

            addContact("Vinicius","+55 51 95412459");
        } catch (OperationApplicationException e) {
            addMessageToList("[MainActivity] "+e.getMessage());
        } catch (RemoteException e) {
            addMessageToList("[MainActivity] "+e.getMessage());
        }

    }


    public void addMessageToList(String message){
        String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
        arrayListMessages.add("["+currentDateTimeString+"] "+message);
        ((ArrayAdapter<String>) listView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent){
            //recebe message que vier pelo brodcast do socket
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO,"[MainActivity][BroadcastReceiver][onReceive] Mensagem recebida ");
            String message = intent.getStringExtra("message");

            try {
                JSONObject objMessage = new JSONObject(message);
                Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO,"[MainActivity][BroadcastReceiver][onReceive] message: "+objMessage.toString());
                addMessageToList(objMessage.toString());
//                addContact("teste","teste");
            } catch (Exception e) {
                addMessageToList(message);
                Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO,"[MainActivity][BroadcastReceiver][onReceive][Exception] "+ message);
            }
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addContact(String displayName, String phoneNumber) throws OperationApplicationException, RemoteException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        int rawContactID = ops.size();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

    }
}
