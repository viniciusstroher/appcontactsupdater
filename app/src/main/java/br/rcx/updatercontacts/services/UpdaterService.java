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
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdaterService extends Service {
    private Thread restThread = null;
    public UpdaterService() throws IOException {
        //inicia servidor
        restThread = new Thread(new Runnable() {
            @Override
            public void run() {
            sendMessageList("Iniciando servico de rest");
            while (true) {
                try {
                    int sleepTime = Integer.parseInt(MainActivity.msValue);
                    if(sleepTime > 0) {

                        if (MainActivity.startstop) {
                            handleRestRequest();
                        }
                        Thread.sleep(sleepTime);
                    }
                } catch (Exception e) {
                    sendMessageList("handleClientRequest error:" + e.getMessage());
                }
            }
            }

        });
        restThread.start();

    }

    public void handleRestRequest() throws JSONException, InterruptedException, OperationApplicationException, RemoteException {
        if(MainActivity.hostValue == null || MainActivity.authValue == null) {
            sendMessageList("[Configure] Configure o host e o auth");
        }

        if(!MainActivity.hostValue.equals("") && !MainActivity.authValue.equals("")){
            sendMessageList("[Configure] Configure o host e o auth");
        }
        JSONObject message = new JSONObject();

        //ajustar este fluxo
        message.put("action","android_get_contacts_whats");
        String returnApi = post(MainActivity.httpValue, MainActivity.hostValue, MainActivity.authValue, message);
        sendMessageList("[ENVIO REST android_get_contacts_whats] "+MainActivity.hostValue+" - "+message.toString());

        JSONObject restResponse;
        try{
            restResponse = new JSONObject(returnApi);
            sendMessageList("[RETORNO REST android_get_contacts_whats] "+restResponse.toString());
        }catch (Exception e){
            restResponse = null;
            sendMessageList("[ENVIO REST android_get_contacts_whats][Exception] Erro parse json api.");
        }

        //trata retorno api
        if(restResponse != null){
            //regra de negocio
            if(!restResponse.has("data")) {
                sendMessageList("[RETORNO REST] Não tem data");
            }else{
                JSONArray phones = restResponse.getJSONArray("data");
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
                        JSONObject message2 = new JSONObject();
                        message2.put("action","android_set_contacts_whats");
                        message2.put("contact_id",Integer.parseInt(phone.getString("ID")));
                        message2.put("whats",phoneChecked.getString("hasWhats"));

                        returnApi = post(MainActivity.httpValue, MainActivity.hostValue, MainActivity.authValue, message2);
                        sendMessageList("[ENVIO REST android_set_contacts_whats] "+MainActivity.hostValue+" - "+message2.toString());
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
