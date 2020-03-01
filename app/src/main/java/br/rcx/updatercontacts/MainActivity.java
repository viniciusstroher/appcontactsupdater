package br.rcx.updatercontacts;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mMessageReceiver, new IntentFilter(MainActivity.filterBroadCastMessageIn));

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

       ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, 12345);

//        try {
//            addContact("+55 51 95412459");
//        } catch (OperationApplicationException e) {
//            e.printStackTrace();
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//        getContactIdByNumber("+55 51 95412459");
//            hasWhatsapp("5");
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
            JSONObject objMessage = null;

            try {
                objMessage = new JSONObject(message);
                Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[MainActivity][BroadcastReceiver][onReceive] message: " + objMessage.toString());
                addMessageToList(objMessage.toString());
            }catch(Exception e) {
                objMessage = null;
            }

            try{

                if(objMessage != null) {
                    JSONObject returnObject = new JSONObject();
                    String messageReturn = "esta acao nao existe";
                    String contactId = null;
                    addMessageToList("ação: " + objMessage.getString("action"));
                    switch (objMessage.getString("action")) {
                        case "add_contact":
                            contactId = getContactIdByNumber(objMessage.getString("phone"));

                            if (contactId != null) {
                                messageReturn = "Numero: " + objMessage.getString("phone") + " já existe";
                                addMessageToList(messageReturn);

                                returnObject.put("message", messageReturn);
                                sendMessage(returnObject.toString());
                            } else {
                                addContact(objMessage.getString("phone"));
                                messageReturn = "Adicionado numero: " + objMessage.getString("phone");
                                addMessageToList(messageReturn);

                                returnObject.put("message", messageReturn);
                                sendMessage(returnObject.toString());
                            }

                            break;
                        case "get_contact":
                            addMessageToList("Pesquisando numero: " + objMessage.getString("phone"));

                            contactId = getContactIdByNumber(objMessage.getString("phone"));

                            if (contactId == null) {
                                returnObject.put("message", "contato não encontrado");
                            }else{
                                String contactName = getContactDisplayNameByNumber(objMessage.getString("phone"));
                                String hasWhats =  hasWhatsapp(contactId);
                                returnObject.put("message", "contato encontrado");
                                returnObject.put("id", contactId);
                                returnObject.put("contactId", contactId);
                                returnObject.put("contactName", contactName);
                                returnObject.put("hasWhats", hasWhats);
                            }

                            addMessageToList(returnObject.toString());
                            sendMessage(returnObject.toString());
                            break;
                        default:
                            addMessageToList(messageReturn);
                            returnObject.put("message", messageReturn);
                            sendMessage(returnObject.toString());
                            break;
                    }
                }else{
                    addMessageToList(message);
                }
            } catch (Exception e) {
                addMessageToList(e.getMessage());
                Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO,"[MainActivity][BroadcastReceiver][onReceive][Exception] "+ e.getMessage());
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
    //falta adicionar nos contatos
    public void addContact(String phoneNumber) throws OperationApplicationException, RemoteException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        int rawContactID = ops.size();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, phoneNumber)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

    }

    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = null;

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    public String getContactIdByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = null;

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.CONTACT_ID }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }


    public String hasWhatsapp(String contactID) {
        String rowContactId = null;
        boolean hasWhatsApp;

        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.Data.CONTACT_ID + " = ? AND account_type IN (?,?)";
        String[] selectionArgs = new String[]{contactID, "com.whatsapp","com.whatsapp.w4b"};
        Cursor cursor = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null);
        if (cursor != null) {
            hasWhatsApp = cursor.moveToNext();
            if (hasWhatsApp) {
                rowContactId = cursor.getString(0);
            }
            cursor.close();
        }
        return rowContactId;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super
                .onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);


    }

    public void sendMessage(String message){
        Intent intent = new Intent(MainActivity.filterBroadCastMessageOut);
        intent.putExtra("message", message);
        String returnSocketSend = "[MainActivity][sendMessage] Enviando mesage para UpdaterService: "+message;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, returnSocketSend);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
