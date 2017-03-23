package client;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpReceiver extends Thread {
    private DatagramSocket socket = null;
    private byte[] recvBuffer = new byte[1024];

    public UdpReceiver(DatagramSocket datagramSocket) {
        this.socket = datagramSocket;
    }

    @Override
    public void run() {

        try {
            while(!socket.isClosed()) {
                DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
                socket.receive(recvPacket);
                String msg = new String(recvPacket.getData());
                System.out.println(msg);
                System.out.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) socket.close();
        }
    }
}
