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
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //recebe message que vier pelo brodcast do socket
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][BroadcastReceiver][onReceive] Mensagem recebida ");
            String message = intent.getStringExtra("message");
            messageBack = message;
        }
    };

    public UpdaterService() throws IOException {
        lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mMessageReceiver, new IntentFilter(MainActivity.filterBroadCastMessageOut));

        server = new ServerSocket(serverPort);
        String startSocketLog = "[UpdaterService][UpdaterService] Servidor iniciado na porta "+serverPort;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, startSocketLog);

        sendMessage("UpdaterService iniciando",false);

        socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessage("Iniciando servico de socket: "+isRunning,false);
                while(isRunning) {
                    Socket client = null;
                    try {
                        sendMessage("Esperando por cliente",false);
                        client = server.accept();
                        sendMessage("Cliente conectado: "+client.getInetAddress().getHostAddress(),false);
                    } catch (Exception e) {
                        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][handleSocketProcess][Exception] " + e.getMessage());
                        sendMessage("Nenhum cliente conectado",false);
                        client = null;
                    }

                    if (client != null) {
                        String messageInLog = "[UpdaterService][handleSocketProcess] Cliente conectado do IP " + client.getInetAddress().
                                getHostAddress();
                        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, messageInLog);

                        Scanner entry = null;
                        try {
                            entry = new Scanner(client.getInputStream());
                        } catch (IOException e) {
                            entry = null;
                            e.printStackTrace();
                        }

                        if(entry != null) {

                                String entryData = entry.nextLine();
                                sendMessage(entryData,true);
                                boolean waitReturnCommand = true;
                                while(waitReturnCommand){
                                    //espera receber a message do broadcast
                                    //para processeguir
                                    if(messageBack != null){
                                        DataOutputStream outputStream = null;
                                        try {
//                                            outputStream = new DataOutputStream(client.getOutputStream());
//                                            outputStream.pr(messageBack);
                                            PrintWriter out = new PrintWriter(client.getOutputStream()); //Gets output to connection

                                            out.println(messageBack);
                                            out.flush();
                                            sendMessage("Message enviada ["+client.getInetAddress().getHostAddress()+"] "+messageBack,false);
                                        } catch (IOException e) {
                                            sendMessage("Erro ao enviar message ao socket",false);
                                            e.printStackTrace();
                                        } finally {
                                            
                                        }

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

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

        });

        socketThread.start();
    }

    @Override
    public void onCreate(){

    }

    public void sendMessage(String message,boolean waitForReturn){
        Intent intent = new Intent(MainActivity.filterBroadCastMessageIn);
        intent.putExtra("message", message);
        intent.putExtra("return", waitForReturn);
        String startSocketLog = "[UpdaterService][sendMessage] Enviando mesage para MainActivity: "+message;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, startSocketLog);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    public void addContact(String displayName, String phoneNumber){
        ContentValues contentValues = new ContentValues();
        Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, contentValues);
        Uri addContactsUri = ContactsContract.Data.CONTENT_URI;

        long rawContactId = ContentUris.parseId(rawContactUri);
        contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);

        // Each contact must has an mime type to avoid java.lang.IllegalArgumentException: mimetype is required error.
        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        // Put contact display name value.
        contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, displayName);

        contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
        int phoneContactType = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
        contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, phoneContactType);

        getContentResolver().insert(addContactsUri, contentValues);
    }
}
