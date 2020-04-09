package br.rcx.updatercontacts;

import android.app.Service;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.view.WindowManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdaterService extends Service {
    private int serverPort = 3322;
    private ServerSocket server;
    private Thread socketThread = null;
    private Thread restThread = null;

    private String serviceType = "REST"; //OU SOCKET
    public UpdaterService() throws IOException {
        //inicia servidor
        server = new ServerSocket(serverPort);
        String startSocketLog = "[UpdaterService][UpdaterService] Servidor iniciado na porta "+serverPort;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, startSocketLog);

        //envia mensagem para ser mostrada na lista (view)
        sendMessageList("UpdaterService iniciando");

        if(serviceType.equals("SOCKET")) {
            socketThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sendMessageList("Iniciando servico de socket");
                    while (true) {
                        try {
                            handleClientRequest();
                        } catch (Exception e) {
                            sendMessageList("handleClientRequest error:" + e.getMessage());
                        }
                    }
                }

            });

            socketThread.start();
        }else{
            restThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sendMessageList("Iniciando servico de rest");
                    while (true) {
                        try {
                            int sleepTime = Integer.parseInt(MainActivity.msValue);
                            if(sleepTime > 0) {
                                Thread.sleep(sleepTime);
                                if (MainActivity.startstop) {
                                    handleRestRequest();
                                }
                            }
                        } catch (Exception e) {
                            sendMessageList("handleClientRequest error:" + e.getMessage());
                        }
                    }
                }

            });
            restThread.start();
        }
    }

    public void handleRestRequest() throws JSONException, InterruptedException, OperationApplicationException, RemoteException {
        if(MainActivity.hostValue != null && MainActivity.authValue != null){
            if(!MainActivity.hostValue.equals("") && !MainActivity.authValue.equals("")){

                //VERIFICA SE TEM CELULARES PARA VERIFICAR

                //ENVIA REST PARA API
                JSONObject message = new JSONObject();

                //ajustar este fluxo
                message.put("action","android_get_contacts_whats");
                String returnApi = post(MainActivity.hostValue,MainActivity.authValue,message);
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
                    if(restResponse.has("data")){
                        JSONArray phones = restResponse.getJSONArray("data");
                        if(phones.length() > 0){
                            for (int i = 0; i < phones.length(); i++) {
                                JSONObject phoneChecked;
                                JSONObject phone = phones.getJSONObject(i);
                                phoneChecked = Api.checkPhone(MainActivity.ctx, phone.getString("VALUE"));
                                Thread.sleep(1000);
                                phoneChecked = Api.checkPhone(MainActivity.ctx, phone.getString("VALUE"));

                                //SetContact
                                JSONObject message2 = new JSONObject();
                                message2.put("action","android_set_contacts_whats");
                                message2.put("contact_id",Integer.parseInt(phone.getString("ID")));
                                message2.put("whats",phoneChecked.getString("hasWhats"));

                                returnApi = post(MainActivity.hostValue,MainActivity.authValue,message2);
                                sendMessageList("[ENVIO REST android_set_contacts_whats] "+MainActivity.hostValue+" - "+message2.toString());
                                sendMessageList("[RETORNO REST android_set_contacts_whats] "+returnApi.toString());
                            }

                        }else{
                            sendMessageList("[RETORNO REST] Não há phones");
                        }
                    }else{
                        sendMessageList("[RETORNO REST] Não tem data");
                    }
                }

                //VERIFICA SE TEM NUMEROS ANTIGOS PARA SEREM ENVIADOS

            }else{
                sendMessageList("[Configure] Configure o host e o auth");
            }
        }else{
            sendMessageList("[Configure] Configure o host e o auth");
        }
    }

    public String post(String uri,String auth,final JSONObject data) {
        try {
            final URL url = new URL("http://"+uri+"/rcx/ContactCenter/messages_api.php");
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            if(auth != null) {
                connection.setRequestProperty("Authorization", "Basic "+auth);
            }
//
//            connection.setRequestProperty("Accept", "application/json");
//            connection.setRequestProperty("Content-type", "application/json");
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
                    // do something with jsonObject here

            }
            //writer.write(data.toString());
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



    //cuida das msgs enviadas do cliente pelo socket
    public void handleClientRequest() throws OperationApplicationException, RemoteException, JSONException {
        Socket client = null;
        try {
            sendMessageList("Esperando por cliente");
            //espera por socket client
            client = server.accept();
            sendMessageList("Cliente conectado: "+client.getInetAddress().getHostAddress());
        } catch (Exception e) {
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][handleSocketProcess][Exception] " + e.getMessage());
            sendMessageList("Nenhum cliente conectado");
            client = null;
        }

        //se tiver algum cliente conectado no momento
        if (client != null) {
            //envia log do ip do cliente conectado
            String messageInLog = "[UpdaterService][handleSocketProcess] Cliente conectado do IP " + client.getInetAddress().
                    getHostAddress();
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, messageInLog);

            //transforma o input em un scanner para pdoer ser lido as mensagens do socket
            Scanner entry = null;
            try {
                entry = new Scanner(client.getInputStream());
            } catch (IOException e) {
                entry = null;
                e.printStackTrace();

                //se der problema no entry mata o client
                sendMessageList("Fechando socket ["+client.getInetAddress().getHostAddress()+"] ");
                closeSocket(client);
            }

            if(entry != null) {
                String entryData;
                try {
                    //pega mensagem do cliente
                    entryData = entry.nextLine();
                }catch (Exception e){
                    entryData = "";

                    //se der problema mata o socket
                    sendMessageList("Fechando socket ["+client.getInetAddress().getHostAddress()+"] ");
                    closeSocket(client);
                }

                //se veio alguma mensagem do socket do client manda para ser tratado
                if(!entryData.equals("")) {
                    JSONObject message;
                    try {
                        message = new JSONObject(entryData);
                    } catch (JSONException e) {
                        message = null;

                        JSONObject returnJsonBreaked = new JSONObject();
                        returnJsonBreaked.put("message","Json enviado invalido.");
                        sendMessageSocket(client, returnJsonBreaked.toString());

                        closeSocket(client);
                    }

                    if(message != null){
                        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][handleSocketProcess] " + entryData);
                        try {
                            JSONObject returnMessageSocket = Api.handleCommand(MainActivity.ctx, message);
                            sendMessageSocket(client, returnMessageSocket.toString());
                            sendMessageList("Message enviada ["+client.getInetAddress().getHostAddress()+"] "+returnMessageSocket.toString());
                        }catch (Exception e){
                            sendMessageList("Exception envio mensagem: "+e.getMessage());
                        }

                        sendMessageList("Fechando socket ["+client.getInetAddress().getHostAddress()+"] ");
                        closeSocket(client);
                    }
                }
            }
        }
    }

    //disconecta socket
    public static boolean closeSocket(Socket client){
        try {
            client.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    //envia msg para o socket
    public void sendMessageSocket(Socket client,String messageBack){
        DataOutputStream outputStream = null;
        try {
            PrintWriter out = new PrintWriter(client.getOutputStream()); //Gets output to connection
            out.println(messageBack);
            out.flush();
            sendMessageList("Message enviada ["+client.getInetAddress().getHostAddress()+"] "+messageBack);
        } catch (IOException e) {
            sendMessageList("Erro ao enviar message ao socket");
            e.printStackTrace();
        }
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
