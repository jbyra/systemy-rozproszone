package server;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;


public class MainServer {

    private ServerSocket tcpServer = null;

    static final HashMap<String, PrintWriter> writers = new HashMap<>();
    static final HashMap<String, Socket> sockets = new HashMap<>();


    public void startServer(int port) throws IOException {
        tcpServer = new ServerSocket(port);
        UdpHandling udpHandling = new UdpHandling(port);
        udpHandling.start();
        try {
            while(true) {
                new ClientHandling(tcpServer.accept()).start();
                System.out.println("new client connected");
            }
        } finally {
            if (tcpServer != null) tcpServer.close();
        }
    }



    public static void main(String[] args) {
        MainServer server = new MainServer();
        try {
            server.startServer(12345);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
