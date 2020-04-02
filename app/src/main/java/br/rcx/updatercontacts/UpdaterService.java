package br.rcx.updatercontacts;

import android.app.Service;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
    public boolean isRunning = true;

    public String messageBack = null;
    public LocalBroadcastManager lbm;

    public UpdaterService() throws IOException {
        lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mMessageReceiver, new IntentFilter(MainActivity.filterBroadCastMessageOut));

        //inicia servidor
        server = new ServerSocket(serverPort);
        String startSocketLog = "[UpdaterService][UpdaterService] Servidor iniciado na porta "+serverPort;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, startSocketLog);

        //envia mensagem para ser mostrada na lista (view)
        sendMessage("console","UpdaterService iniciando",false);

        //inicia socket server para escutar requests
        socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessage("console","Iniciando servico de socket: "+isRunning,false);
                while(isRunning) {
                    handleClientRequest();
                }
            }

        });

        socketThread.start();
    }

    public void handleClientRequest(){
        Socket client = null;
        try {
            sendMessage("console","Esperando por cliente",false);
            //espera por socket client
            client = server.accept();
            sendMessage("console","Cliente conectado: "+client.getInetAddress().getHostAddress(),false);
        } catch (Exception e) {
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][handleSocketProcess][Exception] " + e.getMessage());
            sendMessage("console","Nenhum cliente conectado",false);
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
            }

            if(entry != null) {
                String entryData;
                try {
                    //pega mensagem do cliente
                    entryData = entry.nextLine();
                }catch (Exception e){
                    entryData = "";
                    sendMessageSocket(client,messageBack);
                    sendMessageSocket(client,messageBack);
                }

                sendMessage("socket",entryData,true);
                boolean waitReturnCommand = true;
                while(waitReturnCommand){
                    //espera receber a message do broadcast
                    //para processeguir
                    if(messageBack != null){
                        sendMessageSocket(client,messageBack);
                        messageBack = null;
                        waitReturnCommand = false;
                    }
                }

                Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][handleSocketProcess] " + entryData);

                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }



    //envia msg para o socket
    public void sendMessageSocket(Socket client,String messageBack){
        DataOutputStream outputStream = null;
        try {
            PrintWriter out = new PrintWriter(client.getOutputStream()); //Gets output to connection
            out.println(messageBack);
            out.flush();
            sendMessage("console","Message enviada ["+client.getInetAddress().getHostAddress()+"] "+messageBack,false);
        } catch (IOException e) {
            sendMessage("console","Erro ao enviar message ao socket",false);
            e.printStackTrace();
        }
    }



    //recebe mensagens que MainActivity enviar
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //recebe message que vier pelo brodcast do socket
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][BroadcastReceiver][onReceive] Mensagem recebida ");
            String message = intent.getStringExtra("message");
            messageBack = message;
        }
    };

    //envia msg para UpdaterService->class
    public void sendMessage(String type,String message,boolean waitForReturn){
        Intent intent = new Intent(MainActivity.filterBroadCastMessageIn);
        intent.putExtra("type", type);
        intent.putExtra("message", message);
        intent.putExtra("return", waitForReturn);
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
