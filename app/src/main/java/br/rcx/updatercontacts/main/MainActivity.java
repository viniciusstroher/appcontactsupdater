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
    private static final int maxLogListItems = 200;

    //variaveis de estado
    public static String hostValue="";
    public static String userValue="";
    public static String pwdValue="";
    public static String authValue="";
    public static String msValue = "";
    public static String httpValue = "";
    public static String zabbixUrlValue = "http://zabbix?senha=123&user=1";
    public static boolean startstop = false;
    public static boolean zabbixService = false;

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
        hostValue = preferenceService.getPreference("host");
        userValue = preferenceService.getPreference("user");
        pwdValue = preferenceService.getPreference("pwd");
        httpValue = preferenceService.getPreference("http");
        msValue = preferenceService.getPreference("ms");
        startstop = preferenceService.getPreferenceBoolean("startstop");

        //gera tocken de auth
        authValue = ApiService.generateAuth(userValue,pwdValue);

        //preenche campos
        if(hostValue != null) {
            host.setText(hostValue);
        }

        if(user != null) {
            user.setText(userValue);
        }

        if(pwd != null) {
            pwd.setText(pwdValue);
        }

        if(msValue != null) {
            ms.setText(msValue);
        }else{
            msValue = MainActivity.DEFAULT_MS;
            ms.setText(msValue);
        }

        if(httpValue != null){
            httpButton.setText(httpValue);
        }else{
            httpButton.setText(MainActivity.DEFAULT_PROTOCOL_HTTP);
        }

        if(startstop){
            startStopButton.setText("Parar!");
        }else{
            startStopButton.setText("Iniciar!");
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
                    httpButton.setText("https");
                }else{
                    httpButton.setText("http");
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
                    authValue = ApiService.generateAuth(userValue,pwdValue);

                    //salva estado nos preferences
                    preferenceService.setPreference("host",hostValue);
                    preferenceService.setPreference("user",userValue);
                    preferenceService.setPreference("pwd",pwdValue);
                    preferenceService.setPreference("ms",msValue);
                    preferenceService.setPreference("http",httpValue);

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
                preferenceService.setPreference("startstop",startstop);

                if(startstop) {
                    showSaveMessage("Iniciando rest!");
                    addMessageToList("Iniciando rest!");
                }else{
                    showSaveMessage("Parando rest!");
                    addMessageToList("Parando rest!");
                }

                if(startstop){
                    startStopButton.setText("Parar!");
                }else{
                    startStopButton.setText("Iniciar!");
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
