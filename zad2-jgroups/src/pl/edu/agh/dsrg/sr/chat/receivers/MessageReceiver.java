package pl.edu.agh.dsrg.sr.chat.receivers;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos;


public class MessageReceiver extends ReceiverAdapter {

    @Override
    public void viewAccepted(View view) {
        super.viewAccepted(view);
        System.out.println(view.toString());
    }

    @Override
    public void receive(Message msg) {

        try {
            ChatOperationProtos.ChatMessage chatMsg = ChatOperationProtos.ChatMessage.parseFrom(msg.getBuffer());
            System.out.println(msg.getSrc() + " : "+chatMsg.getMessage());
        } catch (InvalidProtocolBufferException e ) {
            e.printStackTrace();
        }

    }
}
