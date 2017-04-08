package pl.edu.agh.dsrg.sr.chat;


import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import pl.edu.agh.dsrg.sr.chat.receivers.ManagementReceiver;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos;
import pl.edu.agh.dsrg.sr.chat.receivers.MessageReceiver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;


public class ChatClient {
    private JChannel mainChannel;
    private String nickname;
    private static final String MANAGEMENT_CHANNEL = "ChatManagement321321";
    private HashMap<String, JChannel> channels = new HashMap<>();
    public static final HashMap<String, LinkedList<String>> users = new HashMap<>();
    private JChannel current = null;

    public ChatClient(String nickname) {
        this.nickname = nickname;
    }

    private void start() throws  Exception {

        try {
            mainChannel = new JChannel(false);

            ProtocolStack stack = new ProtocolStack();
            mainChannel.setProtocolStack(stack);
            stack.addProtocol( new UDP())
                    .addProtocol(new PING())
                    .addProtocol(new MERGE3())
                    .addProtocol(new FD_SOCK())
                    .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                    .addProtocol(new VERIFY_SUSPECT())
                    .addProtocol(new BARRIER())
                    .addProtocol(new NAKACK2())
                    .addProtocol(new UNICAST3())
                    .addProtocol(new STABLE())
                    .addProtocol(new GMS())
                    .addProtocol(new UFC())
                    .addProtocol(new MFC())
                    .addProtocol(new FRAG2())
                    .addProtocol(new STATE_TRANSFER())
                    .addProtocol(new FLUSH());
            stack.init();

            mainChannel.setReceiver(new ManagementReceiver(mainChannel));
            mainChannel.connect(MANAGEMENT_CHANNEL);
            mainChannel.getState(null, 10000);

            establishConnection();
            userInput();
        } finally {
            ChatOperationProtos.ChatAction action = ChatOperationProtos.ChatAction.newBuilder()
                    .setAction(ChatOperationProtos.ChatAction.ActionType.LEAVE)
                    .setNickname(nickname)
                    .setChannel(MANAGEMENT_CHANNEL)
                    .build();
            mainChannel.send(new Message(null, null, action.toByteArray()));
        }


        mainChannel.disconnect();

    }
    private void establishConnection() throws Exception{
        ChatOperationProtos.ChatAction action = ChatOperationProtos.ChatAction.newBuilder()
                .setAction(ChatOperationProtos.ChatAction.ActionType.JOIN)
                .setNickname(nickname)
                .setChannel(MANAGEMENT_CHANNEL)
                .build();
        mainChannel.send(new Message(null, null,action.toByteArray()));
    }

    private void userInput() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while(true) {
                String input = in.readLine();
                if (input.startsWith("-")) {
                    String[] cmd = input.split(" ");
                    switch(cmd[0]) {
                        case "-join":
                            if (cmd.length > 1 && cmd[1].length() >0) {
                                joinChannel(cmd[1]);
                            }
                            break;
                        case "-leave":
                            if (cmd.length > 1 && cmd[1].length() >0) {
                                leaveChannel(cmd[1]);
                            }
                            break;
                        case "-switch" :
                            if (cmd.length > 1 && cmd[1].length() >0) {
                                if (channels.containsKey(cmd[1])) {
                                    current = channels.get(cmd[1]);
                                    System.out.println("Switched to: "+cmd[1]);
                                }
                            }
                            break;
                        case "-list":
                            listChannels();
                            break;
                        default:
                            sendMessage(input);
                            break;

                    }
                } else {
                    sendMessage(input);
                }


            }
    }

    private void defaultProtocolStack(ProtocolStack stack, InetAddress addr) throws Exception {
        stack.addProtocol( new UDP().setValue("mcast_group_addr", addr))
                .addProtocol(new PING())
                .addProtocol(new MERGE3())
                .addProtocol(new FD_SOCK())
                .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(new BARRIER())
                .addProtocol(new NAKACK2())
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new UFC())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2())
                .addProtocol(new STATE_TRANSFER())
                .addProtocol(new FLUSH());
        stack.init();

    }

    private void listChannels() {
        synchronized (users) {
            for (String chName: users.keySet()) {
                System.out.println(chName+" - users:");
                for (String uName: users.get(chName)){
                    System.out.println("\t - "+uName);
                }
            }
        }
    }

    private void joinChannel(String name) throws Exception {
        synchronized (channels) {
            if (!channels.containsKey(name)) {
                JChannel channel = new JChannel(false);
                channel.setName(name);
                ProtocolStack p = new ProtocolStack();
                channel.setProtocolStack(p);
                defaultProtocolStack(p, InetAddress.getByName(name));
                channel.setReceiver(new MessageReceiver());
                channel.connect(name);

                channels.put(name, channel);

                ChatOperationProtos.ChatAction action = ChatOperationProtos.ChatAction.newBuilder()
                        .setAction(ChatOperationProtos.ChatAction.ActionType.JOIN)
                        .setNickname(nickname)
                        .setChannel(name)
                        .build();
                synchronized (users) {
                    if(!users.containsKey(name)) {
                        users.put(name, new LinkedList<>());
                    }
                }
                mainChannel.send(new Message(null, null, action.toByteArray()));
                current = channel;

            }
        }
    }

    private void leaveChannel(String name) throws Exception {
        synchronized (channels) {
            if (channels.containsKey(name)) {
                JChannel ch = channels.get(name);
                channels.remove(name);

                ChatOperationProtos.ChatAction action = ChatOperationProtos.ChatAction.newBuilder()
                        .setAction(ChatOperationProtos.ChatAction.ActionType.LEAVE)
                        .setNickname(nickname)
                        .setChannel(name)
                        .build();
                mainChannel.send(new Message(null, null, action.toByteArray()));

                if (ch == current) {
                    current = null;
                    System.out.println("Left: "+name+". Switch to other channel");
                }
                ch.disconnect();

            }
        }
    }

    private void sendMessage(String msg) throws Exception {
        ChatOperationProtos.ChatMessage chatMsg = ChatOperationProtos.ChatMessage.newBuilder()
                .setMessage(nickname+" > "+msg)
                .build();
        current.send(new Message(null, null, chatMsg.toByteArray()));

    }

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String nickname;
        try {
            System.out.println("Enter nickname:");
            nickname = in.readLine();
            ChatClient c = new ChatClient(nickname);
            c.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
