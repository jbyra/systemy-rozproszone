import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

public class Doctor {

    private Channel sending;
    private Channel receiving;
    private String doctorName;

    public Doctor(String doctorName) throws IOException, TimeoutException {
        this.doctorName = doctorName;
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        sending = connection.createChannel();
        sending.queueDeclare();
        sending.exchangeDeclare(Utils.EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        receiving = connection.createChannel();

        String name = receiving.queueDeclare(Utils.RESPONSE+"."+doctorName, false, false, false, null).getQueue();
        receiving.exchangeDeclare(Utils.EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        receiving.queueBind(name, Utils.EXCHANGE_NAME, Utils.RESPONSE+"."+doctorName);
        System.out.println("created queue: "+ name);

        Consumer consumer = new DefaultConsumer(receiving){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received: " + message);
            }

            };
        receiving.basicConsume(name, true, consumer);
    }

    public void send() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Patient name:");
        String name = console.readLine();

        while (!name.equals("quit")) {
            sending.basicPublish(Utils.EXCHANGE_NAME, String.format("%s.%s.%s", Utils.REQUEST, Utils.ExaminTypes.ANKLE, doctorName), null, ("ankle : "+name).getBytes("UTF-8"));
            sending.basicPublish(Utils.EXCHANGE_NAME, String.format("%s.%s.%s", Utils.REQUEST, Utils.ExaminTypes.KNEE, doctorName), null, ("knee : "+name).getBytes("UTF-8"));
            sending.basicPublish(Utils.EXCHANGE_NAME, String.format("%s.%s.%s", Utils.REQUEST, Utils.ExaminTypes.ELBOW, doctorName), null, ("elbow : "+name).getBytes("UTF-8"));
            System.out.println("sent: "+name);
            System.out.println("Patient name:");
            name = console.readLine();
        }
    }

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Doctor name:");
        Doctor doctor = new Doctor(br.readLine());
        doctor.send();





    }
}
