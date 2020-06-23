package br.rcx.updatercontacts.services;

import android.app.Service;
import android.content.*;
import android.os.*;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import br.rcx.updatercontacts.main.MainActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdaterService extends Service {
    private Thread thread = null;
    private Date lastExecuteRest = null;
    private Date lastExecuteZabbix = null;
    private int intervalMs;
    private int intervalMsZabbix;

    public UpdaterService() throws IOException {
        intervalMs = Integer.parseInt(MainActivity.msValue);
        intervalMsZabbix = Integer.parseInt(MainActivity.zabbixServiceMsValue);
        //inicia servidor
        thread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {
                        Date now = new Date();
                        //executa se tiver dado o tempo configurado - app
                        if(canExecute(lastExecuteRest,intervalMs)) {
                            if (MainActivity.startStopValue) {
                                handleRestRequest();
                                lastExecuteRest = now;
                                sendMessageList("[UpdaterService] Iniciando Servico de Checagem de numeros.");
                            }
                        }

                        //executa se tiver dado o tempo configurado - zabbix
                        if(canExecute(lastExecuteZabbix,intervalMsZabbix)) {
                            if (MainActivity.zabbixServiceValue) {
                                handleZabbixRequest();
                                lastExecuteZabbix = now;
                                sendMessageList("[UpdaterService] Iniciando Servico de Zabbix.");
                            }
                        }
                    } catch (Exception e) {
                        sendMessageList("[UpdaterService] handleClientRequest error:" + e.getMessage());
                    }
                }
            }

        });
        thread.start();
    }

    public static boolean canExecute(Date lastDate,int ms) {
        if(lastDate == null){
            return true;
        }

        long diffInMillies = Math.abs(new Date().getTime() - lastDate.getTime());
        long diffSeconds   = TimeUnit.MILLISECONDS.toSeconds(diffInMillies);
        if(diffSeconds*1000 >= ms){
            return true;
        }

        return false;
    }

    public void handleZabbixRequest() throws JSONException, InterruptedException, OperationApplicationException, RemoteException {
        if (MainActivity.zabbixServiceUrlValue == null) {
            sendMessageList("[Configure] Configure a url");
        }

        if (!MainActivity.zabbixServiceUrlValue.equals("")) {
            sendMessageList("[Configure] Configure a url");
        }

        try {
            String messageZabbixRawResponse = get(MainActivity.zabbixServiceUrlValue);
            JSONObject messageZabbixResponse = new JSONObject(messageZabbixRawResponse);
            sendMessageList("[RETORNO Zabbix " + MainActivity.zabbixServiceUrlValue + "] " + messageZabbixResponse.toString());
        }catch(Exception e){
            sendMessageList("[RETORNO Zabbix " + MainActivity.zabbixServiceUrlValue + "] " + e.getMessage());
        }
    }

    public void handleRestRequest() throws JSONException, InterruptedException, OperationApplicationException, RemoteException {
        if(MainActivity.hostValue == null || MainActivity.authValue == null) {
            sendMessageList("[Configure] Configure o host e o auth");
        }

        if(!MainActivity.hostValue.equals("") && !MainActivity.authValue.equals("")){
            sendMessageList("[Configure] Configure o host e o auth");
        }

        JSONObject messagePhoneNumbersRequest = new JSONObject();
        messagePhoneNumbersRequest.put("action","android_get_contacts_whats");
        String returnApi = post(MainActivity.httpValue, MainActivity.hostValue, MainActivity.authValue, messagePhoneNumbersRequest);
        sendMessageList("[ENVIO REST android_get_contacts_whats] "+MainActivity.hostValue+" - "+messagePhoneNumbersRequest.toString());

        JSONObject messagePhoneNumbersResponse;
        try{
            messagePhoneNumbersResponse = new JSONObject(returnApi);
            sendMessageList("[RETORNO REST android_get_contacts_whats] "+messagePhoneNumbersResponse.toString());
        }catch (Exception e){
            messagePhoneNumbersResponse = null;
            sendMessageList("[ENVIO REST android_get_contacts_whats][Exception] Erro parse json api.");
        }

        //trata retorno api
        if(messagePhoneNumbersResponse != null){
            //regra de negocio
            if(!messagePhoneNumbersResponse.has("data")) {
                sendMessageList("[RETORNO REST] Não tem data");
            }else{
                JSONArray phones = messagePhoneNumbersResponse.getJSONArray("data");
                if(phones.length() == 0) {
                    sendMessageList("[RETORNO REST] Não há phones");
                }else{
                    for (int i = 0; i < phones.length(); i++) {
                        JSONObject phoneChecked;
                        JSONObject phone = phones.getJSONObject(i);
                        phoneChecked = ApiService.checkPhone(MainActivity.ctx, phone.getString("VALUE"));
                        Thread.sleep(1000);
                        phoneChecked = ApiService.checkPhone(MainActivity.ctx, phone.getString("VALUE"));

                        //SetContact
                        JSONObject messagePhoneNumberUpdateRequest = new JSONObject();
                        messagePhoneNumberUpdateRequest.put("action","android_set_contacts_whats");
                        messagePhoneNumberUpdateRequest.put("contact_id",Integer.parseInt(phone.getString("ID")));
                        messagePhoneNumberUpdateRequest.put("whats",phoneChecked.getString("hasWhats"));

                        returnApi = post(MainActivity.httpValue, MainActivity.hostValue, MainActivity.authValue, messagePhoneNumberUpdateRequest);
                        sendMessageList("[ENVIO REST android_set_contacts_whats] "+MainActivity.hostValue+" - "+messagePhoneNumberUpdateRequest.toString());
                        sendMessageList("[RETORNO REST android_set_contacts_whats] "+returnApi.toString());
                    }
                }
            }
        }
    }

    public String post(String protocol, String uri,String auth,final JSONObject data) {
        try {
            final URL url = new URL(protocol+"://"+uri+"/rcx/ContactCenter/messages_api.php");
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            if(auth != null) {
                connection.setRequestProperty("Authorization", "Basic "+auth);
            }

            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            final OutputStream outputStream = connection.getOutputStream();
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

            StringBuilder sb = new StringBuilder();
            Iterator<String> keys = data.keys();

            while(keys.hasNext()) {
                String key = keys.next();
                sb.append("&"+key+"="+data.get(key));
            }

            writer.write(sb.toString());
            writer.flush();
            writer.close();
            outputStream.close();
            connection.connect();

            final InputStream stream = connection.getInputStream();
            return new Scanner(stream, "UTF-8").next();
        } catch (Exception e) {
            sendMessageList("[POST EXCEPTION]: "+e.getMessage());
        }

        return null;
    }

    public String get(String uri) {
        try {
            final URL url = new URL(uri);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            final OutputStream outputStream = connection.getOutputStream();
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

            writer.flush();
            writer.close();

            outputStream.close();
            connection.connect();

            final InputStream stream = connection.getInputStream();
            return new Scanner(stream, "UTF-8").next();
        } catch (Exception e) {
            sendMessageList("[POST EXCEPTION]: "+e.getMessage());
        }

        return null;
    }

    //envia msg para MainActivity->class
    public void sendMessageList(String message){
        Intent intent = new Intent(MainActivity.filterBroadCastMessageIn);
        intent.putExtra("message", message);
        String startSocketLog = "[UpdaterService][sendMessage] Enviando mesage para MainActivity: "+message;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, startSocketLog);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onCreate(){

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }
}
