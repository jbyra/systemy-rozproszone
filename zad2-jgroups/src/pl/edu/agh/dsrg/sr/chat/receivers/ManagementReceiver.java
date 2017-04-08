package pl.edu.agh.dsrg.sr.chat.receivers;


import com.google.protobuf.InvalidProtocolBufferException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import pl.edu.agh.dsrg.sr.chat.ChatClient;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;


public class ManagementReceiver extends ReceiverAdapter{

    private JChannel mainChannel;

    public ManagementReceiver(JChannel channel) {
        this.mainChannel = channel;
    }

    @Override
    public void viewAccepted(View view) {
        super.viewAccepted(view);
        System.out.println("** chat info >"+view.toString()+ " **");


    }

    @Override
    public void receive(Message msg) {
        try {
            ChatOperationProtos.ChatAction action = ChatOperationProtos.ChatAction.parseFrom(msg.getBuffer());
            ChatOperationProtos.ChatAction.ActionType type = action.getAction();
            String channelName = action.getChannel();
            String nickname = action.getNickname();

            synchronized (ChatClient.users) {
                switch (type) {
                    case JOIN:
                        if (!ChatClient.users.containsKey(channelName)) {
                            ChatClient.users.put(channelName, new LinkedList<>());
                        }
                        ChatClient.users.get(channelName).add(nickname);
                        break;
                    case LEAVE:
                        ChatClient.users.get(channelName).remove(nickname);
                        if (ChatClient.users.get(channelName).isEmpty()) {
                            ChatClient.users.remove(channelName);
                        }
                        break;
                    default:
                        break;
                }
            }

        } catch (InvalidProtocolBufferException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        ChatOperationProtos.ChatState state = ChatOperationProtos.ChatState.parseFrom(input);
        synchronized (ChatClient.users) {
            ChatClient.users.clear();

            for (ChatOperationProtos.ChatAction action: state.getStateList()) {
                String chName = action.getChannel();
                String nick = action.getNickname();

                if (!ChatClient.users.containsKey(chName)) {
                    ChatClient.users.put(chName, new LinkedList<>());
                }
                ChatClient.users.get(chName).add(nick);
            }
        }
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        ChatOperationProtos.ChatState.Builder builder = ChatOperationProtos.ChatState.newBuilder();
        synchronized (ChatClient.users) {
            for (String chName: ChatClient.users.keySet()) {
                List<String> chUsers = ChatClient.users.get(chName);

                for (String u : chUsers) {
                    builder.addStateBuilder()
                            .setAction(ChatOperationProtos.ChatAction.ActionType.JOIN)
                            .setChannel(chName)
                            .setNickname(u);
                }
            }
            ChatOperationProtos.ChatState state = builder.build();
            state.writeTo(output);
        }

    }


}
