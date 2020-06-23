package br.rcx.updatercontacts.main;

import android.Manifest;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import br.rcx.updatercontacts.R;
import br.rcx.updatercontacts.services.ApiService;
import br.rcx.updatercontacts.services.PreferenceService;
import br.rcx.updatercontacts.services.UpdaterService;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    public LocalBroadcastManager lbm;
    public ArrayList<String> arrayListMessages=new ArrayList<String>();
    public ArrayAdapter<String> adapter;
    public static String filterBroadCastMessageIn = "message-in";
    public static String filterBroadCastMessageOut = "message-out";
    public static ContentResolver ctx;

    private SharedPreferences sharedPref;
    private PreferenceService preferenceService;

    private static final String DEFAULT_MS = "60000";
    private static final String DEFAULT_PROTOCOL_HTTP = "http";
    private static final String DEFAULT_PROTOCOL_HTTPS = "https";
    private static final String DEFAULT_URL_ZABBIX = "http://zabbix?senha=123&user=1";
    private static final String DEFAULT_ON = "ON";
    private static final String DEFAULT_OFF = "OFF";

    private static final int maxLogListItems = 200;

    private static final String DEFAULT_PREF_KEY_HOST = "host";
    private static final String DEFAULT_PREF_KEY_USER = "user";
    private static final String DEFAULT_PREF_KEY_PWD = "pwd";
    private static final String DEFAULT_PREF_KEY_HTTP = "http";
    private static final String DEFAULT_PREF_KEY_MS = "ms";
    private static final String DEFAULT_PREF_KEY_ZABBIX_URL = "zabbixserviceurlvalue";
    private static final String DEFAULT_PREF_KEY_ZABBIX_MS = "zabbixservicemsvalue";
    private static final String DEFAULT_PREF_KEY_ZABBIX = "zabbixservice";
    private static final String DEFAULT_PREF_KEY_STARTSTOP = "startstop";

    //variaveis de estado
    public static String hostValue="";
    public static String userValue="";
    public static String pwdValue="";
    public static String authValue="";
    public static String msValue = "";
    public static String httpValue = "";
    public static String zabbixServiceUrlValue = "";
    public static String zabbixServiceMsValue = "";
    public static boolean startStopValue = false;
    public static boolean zabbixServiceValue = false;

    //elementos da activity
    public ListView listView = null;
    private TextView host;
    private TextView user;
    private TextView pwd;
    private TextView ms;
    private Button httpButton;
    private Button saveButton;
    private Button startStopButton;
    private Button clearlogButton;
    private LinearLayout configPanel;
    private LinearLayout logPanel;

    private Button zabbixServiceButton;
    private TextView zabbixServiceUrl;
    private TextView zabbixServiceMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        preferenceService = new PreferenceService(sharedPref);

        //inicia broadcaster
        lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mMessageReceiver, new IntentFilter(MainActivity.filterBroadCastMessageIn));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.listLog);
        adapter = new ArrayAdapter<String>(this, R.layout.simple_row, arrayListMessages);
        listView.setAdapter(adapter);

        addMessageToList("Iniciando app.");

        //pede as permissoes
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, 12345);
        ctx = getContentResolver();

        //referencia objetos
        host = findViewById(R.id.serverIpInput);
        user = findViewById(R.id.serverUserInput);
        pwd = findViewById(R.id.serverPwdInput);
        ms = findViewById(R.id.serverMsInput);
        httpButton = findViewById(R.id.http);
        saveButton = findViewById(R.id.save);

        zabbixServiceButton = findViewById(R.id.zabbixServiceInput);
        zabbixServiceUrl = findViewById(R.id.zabbixServiceUrlInput);
        zabbixServiceMs = findViewById(R.id.zabbixServiceMsInput);

        startStopButton = findViewById(R.id.startstop);
        clearlogButton = findViewById(R.id.clearlog);
        configPanel = findViewById(R.id.configPanel);
        logPanel = findViewById(R.id.logPanel);

        //carrega valores nos campos
        loadFieldsValues();

        //inicia intent de servico
        startService();

        //bind de aoes de click
        registerFieldsAction();
    }

    public void startService(){
        try {
            startService(new Intent(MainActivity.this, UpdaterService.class));
        }catch(Exception e){
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO,"[MainActivity][onCreate] "+ e.getMessage());
        }
    }

    public void loadFieldsValues(){
        //carrega valores do preferences
        hostValue = preferenceService.getPreference(DEFAULT_PREF_KEY_HOST);
        userValue = preferenceService.getPreference(DEFAULT_PREF_KEY_USER);
        pwdValue = preferenceService.getPreference(DEFAULT_PREF_KEY_PWD);
        httpValue = preferenceService.getPreference(DEFAULT_PREF_KEY_HTTP);
        msValue = preferenceService.getPreference(DEFAULT_PREF_KEY_MS);
        //zabbix
        zabbixServiceUrlValue = preferenceService.getPreference(DEFAULT_PREF_KEY_ZABBIX_URL);
        zabbixServiceMsValue = preferenceService.getPreference(DEFAULT_PREF_KEY_ZABBIX_MS);
        zabbixServiceValue = preferenceService.getPreferenceBoolean(DEFAULT_PREF_KEY_ZABBIX);
        startStopValue = preferenceService.getPreferenceBoolean(DEFAULT_PREF_KEY_STARTSTOP);
        //gera tocken de auth
        authValue = ApiService.generateAuth(userValue,pwdValue);

        //preenche campos
        //host
        if(hostValue != null) {
            host.setText(hostValue);
        }

        //usuario
        if(user != null) {
            user.setText(userValue);
        }

        //senha
        if(pwd != null) {
            pwd.setText(pwdValue);
        }

        //ms http
        if(msValue != null) {
            ms.setText(msValue);
        }else{
            msValue = MainActivity.DEFAULT_MS;
            ms.setText(msValue);
        }

        //protocolo http/https
        if(httpValue != null){
            httpButton.setText(httpValue);
        }else{
            httpButton.setText(MainActivity.DEFAULT_PROTOCOL_HTTP);
        }

        //start stop button
        if(startStopValue){
            startStopButton.setText(DEFAULT_ON);
        }else{
            startStopButton.setText(DEFAULT_OFF);
        }

        //zabbix
        if(zabbixServiceUrlValue != null) {
            zabbixServiceUrl.setText(zabbixServiceUrlValue);
        }else{
            zabbixServiceUrlValue = MainActivity.DEFAULT_URL_ZABBIX;
            zabbixServiceUrl.setText(msValue);
        }

        if(zabbixServiceMsValue != null) {
            zabbixServiceMs.setText(zabbixServiceMsValue);
        }else{
            zabbixServiceMsValue = MainActivity.DEFAULT_MS;
            zabbixServiceMs.setText(msValue);
        }

        //start stop button
        if(zabbixServiceValue){
            zabbixServiceButton.setText(DEFAULT_ON);
        }else{
            zabbixServiceButton.setText(DEFAULT_OFF);
        }

        configPanel.setVisibility(View.INVISIBLE);
    }

    public void registerFieldsAction(){
        //acao botao de escolher protocolo
        httpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v){
            Button httpButton = (Button) v.findViewById(R.id.http);
            if(httpButton.getText().toString().equals(MainActivity.DEFAULT_PROTOCOL_HTTP)){
                httpButton.setText(DEFAULT_PROTOCOL_HTTPS);
            }else{
                httpButton.setText(DEFAULT_PROTOCOL_HTTP);
            }
            }
        });

        //acao botao de salvar
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v){
            if(host.getText().toString().equals("")){
                showSaveMessage("Campo host não pode ser vazio");
            }else if(user.getText().toString().equals("") || pwd.getText().toString().equals("")){
                showSaveMessage("Campo user e pwd não podem estar vazio");
            }else{
                //atualiza valores
                hostValue = host.getText().toString();
                userValue = user.getText().toString();
                pwdValue = pwd.getText().toString();
                httpValue = httpButton.getText().toString();
                msValue = ms.getText().toString();

                zabbixServiceValue = zabbixServiceButton.getText().toString().toLowerCase().equals(DEFAULT_OFF) ? false : true;
                zabbixServiceUrlValue = zabbixServiceUrl.getText().toString();
                zabbixServiceMsValue = zabbixServiceMs.getText().toString();

                authValue = ApiService.generateAuth(userValue,pwdValue);

                //salva estado nos preferences
                preferenceService.setPreference(DEFAULT_PREF_KEY_HOST,hostValue);
                preferenceService.setPreference(DEFAULT_PREF_KEY_USER,userValue);
                preferenceService.setPreference(DEFAULT_PREF_KEY_PWD,pwdValue);
                preferenceService.setPreference(DEFAULT_PREF_KEY_MS,msValue);
                preferenceService.setPreference(DEFAULT_PREF_KEY_HTTP,httpValue);
                preferenceService.setPreference(DEFAULT_PREF_KEY_ZABBIX_URL,zabbixServiceUrlValue);
                preferenceService.setPreference(DEFAULT_PREF_KEY_ZABBIX_MS,zabbixServiceMsValue);
                preferenceService.setPreference(DEFAULT_PREF_KEY_ZABBIX,zabbixServiceValue);
                preferenceService.setPreference(DEFAULT_PREF_KEY_HTTP,httpValue);

                showSaveMessage("Configurações salvas!");
                addMessageToList("Configurações salvas!");
            }
            }
        });

        //botão iniciar serviço
        startStopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v){
                startstop = !startstop;
                preferenceService.setPreference(DEFAULT_PREF_KEY_STARTSTOP,startstop);

                if(startstop) {
                    showSaveMessage("Iniciando rest!");
                    addMessageToList("Iniciando rest!");
                }else{
                    showSaveMessage("Parando rest!");
                    addMessageToList("Parando rest!");
                }

                if(startstop){
                    startStopButton.setText(DEFAULT_ON);
                }else{
                    startStopButton.setText(DEFAULT_OFF);
                }
            }
        });

        //botao de limpeza de log
        clearlogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                arrayListMessages.clear();
                ((ArrayAdapter<String>) listView.getAdapter()).notifyDataSetChanged();
            }
        });

    }



    public void showSaveMessage(String message) {
        Toast.makeText(MainActivity.this, message , Toast.LENGTH_SHORT).show();
    }

    public void addMessageToList(String message){
        String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
        arrayListMessages.add("["+currentDateTimeString+"] "+message);

        //limpa logs apos maxLogListItems
        if(arrayListMessages.size() > maxLogListItems) {
            arrayListMessages.clear();
        }

        ((ArrayAdapter<String>) listView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
        recreate();
    }

    //RECEBE DO UPDATER SERVICE MSG - COMO NAO PODE ALTERAR A THREAD DO SERVIÇO
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent){
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

        //limpa logs
        if (id == R.id.action_settings) {
            arrayListMessages.clear();
            ((ArrayAdapter<String>) listView.getAdapter()).notifyDataSetChanged();
            return true;
        }

        //abre tela de configs
        if (id == R.id.action_show_config){
            configPanel.setVisibility(View.VISIBLE);
            logPanel.setVisibility(View.INVISIBLE);
            return true;
        }

        //abre tela de logs
        if(id == R.id.action_show_logs){
            configPanel.setVisibility(View.INVISIBLE);
            logPanel.setVisibility(View.VISIBLE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
