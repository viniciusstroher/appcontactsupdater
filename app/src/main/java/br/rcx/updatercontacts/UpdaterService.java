package br.rcx.updatercontacts;

import android.app.Service;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdaterService extends Service {
    private int serverPort = 3322;
    private ServerSocket server;
    private Thread socketThread = null;

    public UpdaterService() throws IOException {
        //inicia servidor
        server = new ServerSocket(serverPort);
        String startSocketLog = "[UpdaterService][UpdaterService] Servidor iniciado na porta "+serverPort;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, startSocketLog);

        //envia mensagem para ser mostrada na lista (view)
        sendMessageList("UpdaterService iniciando");

        socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessageList("Iniciando servico de socket");
                while(true) {
                    try {
                        handleClientRequest();
                    } catch (Exception e) {
                        sendMessageList("handleClientRequest error:"+e.getMessage());
                    }
                }
            }

        });

        socketThread.start();
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
                    closeSocket(client);
                }

                //se veio alguma mensagem do socket do client manda para ser tratado
                if(!entryData.equals("")) {
                    JSONObject message;
                    try {
                        message = new JSONObject(entryData);
                    } catch (JSONException e) {
                        message = null;
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
            sendMessageList("Fechando socket ["+client.getInetAddress().getHostAddress()+"] ");
            return true;
        } catch (IOException e) {
            sendMessageList("Não foi possivel fechar socket ["+client.getInetAddress().getHostAddress()+"] ");
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
