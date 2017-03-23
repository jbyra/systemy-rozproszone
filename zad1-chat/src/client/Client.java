package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    static final int multicastPort = 42345;
    static final String multicastIP = "230.1.1.1";
    private Socket tcpSocket = null;
    private PrintWriter out = null;
    private Scanner in = null;
    private Scanner console = new Scanner(System.in);
    private String nick = "";


    private DatagramSocket udpSocket = null;
    private MulticastSocket multicastSocket = null;

    private String asciiArt = "\n" +
            "            /:.   ,:\\\n" +
            "      .~=-./::: u  ::\\,-~=.\n" +
            "   ___|::  \\    |    /  ::|___\n" +
            "  \\::  `.   \\   |   /   .' :::/\n" +
            "   \\:    `.  \\  |  /  .'    :/\n" +
            " .-: `-._  `.;;;;;;.'   _.-' :-.\n" +
            " \\::     `-;;;;;;;;;;;-'     ::/\n" +
            "  >~------~;;;;;;;;;;;~------~<\n" +
            " /::    _.-;;;;;;;;;;;-._    ::\\\n" +
            " `-:_.-'   .`;;;;;;;'.   `-._:-'\n" +
            "    /    .'  /  |  \\  `.   :\\\n" +
            "   /::_.'   /   |   \\   `._::\\\n" +
            "       |:: /    |    \\  ::|\n" +
            "       `=-'\\:::.n.:::/`-=-'      hjw\n" +
            "            \\:'   `:/";



    public Client(String servName, int port) throws IOException {

        try {
            tcpSocket = new Socket(servName, port);
            in = new Scanner(tcpSocket.getInputStream());
            out = new PrintWriter(tcpSocket.getOutputStream(), true);
            udpSocket = new DatagramSocket(tcpSocket.getLocalPort());




            System.out.println("Enter nickname:");
            nick = console.nextLine();
            out.println(nick);
            if (in.hasNextLine() && in.nextLine().equals("OK")) {
                System.out.println("Connected to chat.");
                ChatStream tcpReceiver = new ChatStream(in);
                UdpReceiver udpReceiver = new UdpReceiver(udpSocket);
                tcpReceiver.start();
                udpReceiver.start();


                InetAddress addr = InetAddress.getByName(servName);
                InetAddress group = InetAddress.getByName(multicastIP);
                multicastSocket = new MulticastSocket(multicastPort);
                MulticastReceiver multicastReceiver = new MulticastReceiver(nick);
                multicastReceiver.start();

                String line = console.nextLine();

                while (!line.equals("quit")) {
                    if (line.equals("M")) {
                        byte[] sendBuffer = (nick + ">" + asciiArt).getBytes();
                        DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, addr, port);
                        udpSocket.send(packet);
                    }
                    else if (line.equals("N")) {
                        byte[] sendBuffer = (nick + ">" + asciiArt).getBytes();
                        DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, group, multicastPort);
                        multicastSocket.send(packet);
                    }
                    else {
                        out.println(line);
                    }
                    line = console.nextLine();
                }
            } else {
                System.out.println("Nickname already taken");
            }

        } finally {
            if (tcpSocket != null) tcpSocket.close();
            if (out != null) out.close();
            if (console != null) console.close();
            if (udpSocket != null) udpSocket.close();

        }
    }

    public static void main(String[] args) {

        try {
            Client client = new Client("localhost", 12345);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
