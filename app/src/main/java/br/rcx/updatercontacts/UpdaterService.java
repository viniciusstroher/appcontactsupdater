package br.rcx.updatercontacts;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdaterService extends Service {
    private int serverPort = 3322;
    private ServerSocket server;

    public UpdaterService() throws IOException {
        server = new ServerSocket(serverPort);
        String startSocketLog = "[UpdaterService][UpdaterService] Servidor iniciado na porta "+serverPort;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, startSocketLog);

        sendMessage("Iniciando servico de socket");

        handleSocketProcess();


    }

    public void sendMessage(String message){
        Intent intent = new Intent(MainActivity.filterBroadCastMessage);
        intent.putExtra("message", message);

        String startSocketLog = "[UpdaterService][sendMessage] Enviando mesage para MainActivity: "+message;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, startSocketLog);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void handleSocketProcess() throws IOException {
        try {
            while(true) {
                Socket client = server.accept();
                String messageInLog = "[UpdaterService][handleSocketProcess] Cliente conectado do IP " + client.getInetAddress().
                        getHostAddress();
                Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, messageInLog);

                Scanner entry = new Scanner(client.getInputStream());
                while (entry.hasNextLine()) {
                    String entryData = entry.nextLine();
                    Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][handleSocketProcess] "+entryData);
                }

                ((Socket) client).close();
            }
        }catch(Exception e){
            server.close();
            Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][handleSocketProcess][Exception] "+e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }


}
