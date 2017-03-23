package client;


import java.util.Scanner;

public class ChatStream extends Thread {
    private Scanner in = null;

    public ChatStream(Scanner in) {
        this.in = in;
    }

    @Override
    public void run() {
        while(in.hasNextLine()) {
            System.out.println(in.nextLine());
            System.out.flush();
        }
        in.close();
    }
}
