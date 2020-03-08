package br.rcx.updatercontacts;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent){
            //recebe message que vier pelo brodcast do socket
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO,"[MainActivity][BroadcastReceiver][onReceive] Mensagem recebida ");

            String type = intent.getStringExtra("type");
            String message = intent.getStringExtra("message");
            Boolean returnMEssage = intent.getBooleanExtra("return",false);

            try{
                switch (type){
                    case "console":
                        addMessageToList(message);
                        break;
                    case "socket":
                        api(message);
                        break;
                    default:
                        api(message);
                        break;
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
            arrayListMessages.clear();
            ((ArrayAdapter<String>) listView.getAdapter()).notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //falta adicionar nos contatos
    public void addContact(String phoneNumber,String groupId,boolean replace) throws OperationApplicationException, RemoteException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        int rawContactID = ops.size();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, phoneNumber.replace(" ",""))
                .build());

        String phoneReplace = phoneNumber;
        if(replace){
            phoneReplace = phoneNumber.replace("-","");
        }
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneReplace)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        //String groupId = "6"; //my contacts group
        //grupo
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
                        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
                        groupId).build());

        getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

    }

    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number.replace(" ","")));
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
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number.replace(" ","")));
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

    public JSONArray getPhoneNumbers(String contactId) throws JSONException {
        JSONArray r = new JSONArray();
        ContentResolver cr = getContentResolver();
        ArrayList<String> phoneNumber = new ArrayList<String>();
        // IF HAS NUMBER THEN GET ALL
        Cursor pCur = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId}, null);

        String number = null;
        int x = 0;
        while (pCur.moveToNext()){
            number = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            r.put(x,number);
            x++;
        }
        pCur.close();
        return r;
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

    public long getGroupIdFor(Long contactId){
        Uri uri = ContactsContract.Data.CONTENT_URI;
        String where = String.format(
                "%s = ? AND %s = ?",
                ContactsContract.RawContacts.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID);

        String[] whereParams = new String[] {
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                Long.toString(contactId),
        };

        String[] selectColumns = new String[]{
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
        };

        Cursor groupIdCursor = getContentResolver().query(
                uri,
                selectColumns,
                where,
                whereParams,
                null);
        try{
            if (groupIdCursor.moveToFirst()) {
                return groupIdCursor.getLong(0);
            }
            return Long.MIN_VALUE; // Has no group ...
        }finally{
            groupIdCursor.close();
        }
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



    public void api(String message) throws JSONException, OperationApplicationException, RemoteException {
        //valida json
        JSONObject objMessage = null;
        try {
            objMessage = new JSONObject(message);
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[MainActivity][BroadcastReceiver][onReceive] message: " + objMessage.toString());
            addMessageToList(objMessage.toString());
        }catch(Exception e) {
            objMessage = null;
        }

        JSONObject returnObject = new JSONObject();
        String messageReturn = "esta acao nao existe";

        if(objMessage == null) {
            //json invalido retorna msg de error
            returnObject.put("message", messageReturn);

        }else {
            //json valido valida no switch
            String contactId = null;
            addMessageToList("ação: " + objMessage.getString("action"));

            //proura por ação
            switch (objMessage.getString("action")) {
                case "add_contact":
                    addMessageToList("Adicionando numero: " + objMessage.getString("phone"));
                    contactId = getContactIdByNumber(objMessage.getString("phone"));

                    if (contactId != null) {
                        messageReturn = "Numero: " + objMessage.getString("phone") + " já existe";
                        returnObject.put("message", messageReturn);

                    } else {
                        String groupId = "6";
                        if(objMessage.has("groupid")){
                            groupId = objMessage.getString("groupid");
                        }

                        boolean replace = false;
                        if(objMessage.has("replace")){
                            replace = objMessage.getString("replace").equals("0") ? false:true ;
                        }

                        addContact(objMessage.getString("phone"),groupId,replace);

                        messageReturn = "Adicionado numero: " + objMessage.getString("phone");
                        returnObject.put("message", messageReturn);

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
                        JSONArray phoneNumbers = getPhoneNumbers(contactId);
                        returnObject.put("message", "contato encontrado");
                        returnObject.put("id", contactId);
                        returnObject.put("contactId", contactId);
                        returnObject.put("contactName", contactName);
                        returnObject.put("phoneNumber", phoneNumbers);
                        returnObject.put("hasWhats", hasWhats == null ? "0":"1");
                        returnObject.put("group", getGroupIdFor(Long.parseLong(contactId)));

                    }
                    break;

                case "check_contact":
                    addMessageToList("Pesquisando numero: " + objMessage.getString("phone"));
                    contactId = getContactIdByNumber(objMessage.getString("phone"));

                    if (contactId == null) {
                        String groupId = "6";
                        if(objMessage.has("groupid")){
                            groupId = objMessage.getString("groupid");
                        }

                        boolean replace = false;
                        if(objMessage.has("replace")){
                            replace = objMessage.getString("replace").equals("0") ? false:true ;
                        }

                        addContact(objMessage.getString("phone"),groupId,replace);

                        messageReturn = "Adicionado numero: " + objMessage.getString("phone");
                    }

                    contactId = getContactIdByNumber(objMessage.getString("phone"));

                    String contactName = getContactDisplayNameByNumber(objMessage.getString("phone"));
                    String hasWhats =  hasWhatsapp(contactId);

                    JSONArray phoneNumbers = getPhoneNumbers(contactId);
                    returnObject.put("message", "contato encontrado");
                    returnObject.put("id", contactId);
                    returnObject.put("contactId", contactId);
                    returnObject.put("contactName", contactName);
                    returnObject.put("phoneNumber", phoneNumbers);
                    returnObject.put("hasWhats", hasWhats == null ? "0":"1");
                    returnObject.put("group", getGroupIdFor(Long.parseLong(contactId)));

                    break;
                default:
                    returnObject.put("message", messageReturn);
                    returnObject.put("group", getGroupIdFor(Long.parseLong(contactId)));
                    break;
            }
        }

        addMessageToList(messageReturn);
        addMessageToList("Retornando: "+returnObject.toString());
        sendMessage(returnObject.toString());
    }
}
