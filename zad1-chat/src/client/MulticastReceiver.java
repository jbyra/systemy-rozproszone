package client;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastReceiver extends Thread{

    private MulticastSocket socket = null;
    private byte[] buff = new byte[1024];
    private String nick = "";
    private InetAddress group = null;

    public MulticastReceiver(String nick) {
        this.nick = nick;

    }

    @Override
    public void run() {

        try {
            group = InetAddress.getByName(Client.multicastIP);
            socket = new MulticastSocket(Client.multicastPort);
            socket.joinGroup(group);
            while(!socket.isClosed()) {
                DatagramPacket recv = new DatagramPacket(buff, buff.length);
                socket.receive(recv);
                String msg = new String(recv.getData());
                if (!msg.startsWith(this.nick + ">")) {
                    System.out.println(msg);
                    System.out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.leaveGroup(group);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket.close();
            }
        }
    }
}

