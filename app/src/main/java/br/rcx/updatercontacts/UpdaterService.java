package br.rcx.updatercontacts;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;
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
    private Thread socketThread = null;
    public boolean isRunning = true;
    public UpdaterService() throws IOException {

        server = new ServerSocket(serverPort);
        String startSocketLog = "[UpdaterService][UpdaterService] Servidor iniciado na porta "+serverPort;
        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, startSocketLog);

        sendMessage("UpdaterService iniciando");

        socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessage("Iniciando servico de socket: "+isRunning);
                while(isRunning) {
                    Socket client = null;
                    try {
                        sendMessage("Esperando por cliente");
                        client = server.accept();
                        sendMessage("Cliente conectado: "+client.getInetAddress().getHostAddress());
                    } catch (Exception e) {
                        Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][handleSocketProcess][Exception] " + e.getMessage());
                        sendMessage("Nenhum cliente conectado");
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
                            while (entry.hasNextLine()) {
                                String entryData = entry.nextLine();
                                sendMessage(entryData);
                                Logger.getLogger(UpdaterService.class.getName()).log(Level.INFO, "[UpdaterService][handleSocketProcess] " + entryData);
                            }
                        }

                        try {
                            client.close();
                        } catch (IOException e) {
                            e.printStackTrace();
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

    public void sendMessage(String message){
        Intent intent = new Intent(MainActivity.filterBroadCastMessage);
        intent.putExtra("message", message);
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


}
