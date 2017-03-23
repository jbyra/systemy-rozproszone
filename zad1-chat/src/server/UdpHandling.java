package server;


import sun.applet.Main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class UdpHandling extends Thread {
    private int port;
    private byte[] recvBuffer = new byte[1024];
    private DatagramSocket socket = null;

    public UdpHandling(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            while(!socket.isClosed()) {
                DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
                socket.receive(recvPacket);
                String msg = new String(recvPacket.getData());
                //System.out.println("UDP > " + msg);
                String[] t = msg.split(">");
                String nick = t[0];
                //System.out.println(nick);
                synchronized (MainServer.sockets) {
                    for (String s: MainServer.sockets.keySet()) {
                        if (!s.equals(nick)) {
                            //System.out.println(s);
                            int clientPort = MainServer.sockets.get(s).getPort();
                            InetAddress addr = MainServer.sockets.get(s).getInetAddress();
                            byte[] sendBuffer = msg.getBytes();
                            try {
                                DatagramSocket sendSocket = new DatagramSocket();
                                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, addr, clientPort);
                                sendSocket.send(sendPacket);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) socket.close();
        }
    }
}
