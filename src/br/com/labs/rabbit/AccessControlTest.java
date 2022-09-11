package br.com.labs.rabbit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

class AccessControlTest {

	static Connection connection;
	
	@BeforeAll
	static void createConnection() throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost("localhost");
        cf.setUsername("financial");
        cf.setPassword("@1ab5JM%0981aklpq%3#");
        connection = cf.newConnection();
    }
	
	@Test
	void test_cannot_create_queue() throws IOException {
        Channel ch = connection.createChannel();
        ch.queueDeclare("minha_queue_teste", true, false, false, null);
	}
	@Test
	void test_cannot_create_exchange() throws IOException {
        Channel ch = connection.createChannel();
        ch.exchangeDeclare("minha_exchange_teste", BuiltinExchangeType.DIRECT);
	}
	@Test
	void test_cannot_publish_message() throws IOException {
        Channel ch = connection.createChannel();
        String body = String.valueOf("msg test_acl");
        ch.basicPublish("exchange_acl", "abc", null, body.getBytes());
	}
	@Test
	void test_cannot_consume_message() throws IOException {
        Channel ch = connection.createChannel();
        String body = String.valueOf("msg test_acl");
        ch.basicConsume("queue_acl", false, "a-consumer-tag",
                new DefaultConsumer(ch) {
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
                        ch.basicNack(deliveryTag, true, true);
                        try {
                       	 Thread.sleep(10);
                        } catch (InterruptedException ex) {
                       	 System.out.println("erro no sleep");
                        }
                    }
                });
	}

}
