package br.com.labs.rabbit;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
//import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.DefaultConsumer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Receiver {
    //private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        channel.basicQos(110);
        //channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = "qquorum1";
        //channel.queueBind(queueName, EXCHANGE_NAME, "");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

//        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//            String message = new String(delivery.getBody(), "UTF-8");
//            System.out.println(" [x] Received '" + message + "'");
//        };
        //channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });

        boolean autoAck = false;
        channel.basicConsume(queueName, autoAck, "a-consumer-tag",
             new DefaultConsumer(channel) {
                 @Override
                 public void handleDelivery(String consumerTag,
                                            Envelope envelope,
                                            BasicProperties properties,
                                            byte[] body)
                     throws IOException
                 {
                    String s = new String(body, StandardCharsets.UTF_8);
                    System.out.println("recebido" + s);
                     long deliveryTag = envelope.getDeliveryTag();
                     // requeue the delivery
                     channel.basicNack(deliveryTag, true, true);
                     try {
                    	 Thread.sleep(10);
                     } catch (InterruptedException ex) {
                    	 System.out.println("erro no sleep");
                     }
                 }
             });
    }
}

