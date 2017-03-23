package server;


import sun.applet.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandling extends Thread{

    private Socket socket = null;
    private BufferedReader in = null;
    private PrintWriter out = null;
    private String nick;


    public ClientHandling(Socket socket) {
        this.socket = socket;

    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            nick = in.readLine();
            synchronized (MainServer.writers) {
                if (!MainServer.writers.containsKey(nick)) {
                    MainServer.writers.put(nick, out);
                    out.println("OK");
                }
                else {
                    out.println("ERR");
                    return;
                }

            }
            synchronized (MainServer.sockets) {
                MainServer.sockets.put(nick, socket);
            }


            while(!socket.isClosed()) {
                String input = in.readLine();
                if (input == null) return;
                synchronized (MainServer.writers) {
                    for (PrintWriter w : MainServer.writers.values()) {
                         if (w != out) w.println(nick + " > " + input);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            synchronized (MainServer.writers){
                if (!nick.equals("") && MainServer.writers.containsKey(nick)) MainServer.writers.remove(nick);
            }
            synchronized (MainServer.sockets) {
                if (!nick.equals("") && MainServer.sockets.containsKey(nick)) MainServer.sockets.remove(nick);
            }
            if (socket != null) try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
