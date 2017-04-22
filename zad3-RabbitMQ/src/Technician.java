import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class Technician {
    private List<Utils.ExaminTypes> supportedTypes = new ArrayList<>();
    private Connection connection;
    private Channel response;

    public Technician(String spec1, String spec2) throws IOException, TimeoutException {

        supportedTypes.add(Utils.ExaminTypes.valueOf(spec1));
        supportedTypes.add(Utils.ExaminTypes.valueOf(spec2));
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        response = connection.createChannel();

        response.queueDeclare();
        response.exchangeDeclare(Utils.EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        Channel channel = connection.createChannel();

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("received: "+message);
                response.basicPublish(Utils.EXCHANGE_NAME, changeKey(envelope.getRoutingKey()), null, (message+": badanie wykonane").getBytes("UTF-8"));
            }
        };
        supportedTypes.forEach((type) -> {
            try {
                createQueue(type, channel, consumer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private void createQueue(Utils.ExaminTypes type, Channel channel, Consumer consumer) throws IOException {

        System.out.println("request."+type);
        String name = channel.queueDeclare(Utils.REQUEST+"."+type+".*", false, false, false, null).getQueue();
        channel.exchangeDeclare(Utils.EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        channel.queueBind(name, Utils.EXCHANGE_NAME, Utils.REQUEST+"."+type+".*");

        System.out.println("Waiting for messages...");
        channel.basicConsume(name, true, consumer);



    }

    private String changeKey(String key) {
        String[] keys = key.split("\\.");
        return Utils.RESPONSE+"."+keys[2];
    }

    public static void main(String[] args) throws Exception {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Podaj specjalizacje");
        String spec1 = console.readLine();
        String spec2 = console.readLine();
        Technician technician = new Technician(spec1, spec2);
    }

}
