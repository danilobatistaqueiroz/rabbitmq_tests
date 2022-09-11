package br.com.labs.rabbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

class PublishConsumeTest {
	
	static Connection connection;

	@BeforeAll
	static void createConnection() throws IOException, TimeoutException {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("localhost");
		cf.setUsername("guest");
		cf.setPassword("guest");
		connection = cf.newConnection();
	}

	class Message {
		String body;
		long deliveryTag;
	}
	
	@Test
	void test_publish_consume_auto_ack() throws IOException, InterruptedException {
		Channel ch = connection.createChannel();
		String queue = "minhaqueue";
		String exchange = "minhaexchange";
		ch.queueDeclare(queue, true, false, false, null);
		ch.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT);
		ch.queueBind(queue, exchange, "tests");
		String msg = "test message";
		String body = String.valueOf(msg);
		ch.basicPublish(exchange, "tests", false, null, body.getBytes());
		Message msgConsumed = consume_auto_ack(ch, queue);
		ch.queueDelete(queue);
		ch.exchangeDelete(exchange);
		Assert.assertEquals(msg, msgConsumed.body);
	}
	
	@Test
	void test_publish_consume_manual_ack() throws IOException, InterruptedException {
		Channel ch = connection.createChannel();
		String queue = "minhaqueue";
		String exchange = "minhaexchange";
		ch.queueDeclare(queue, true, false, false, null);
		ch.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT);
		ch.queueBind(queue, exchange, "abcdef");
		String msg = "test message manual ack";
		ch.basicPublish(exchange, "abcdef", false, null, msg.getBytes());
		ch.basicPublish(exchange, "abcdef", false, null, "msg 2".getBytes());
		Message msgConsumed = consume_manual_ack(ch, queue);
		System.out.println("body: "+msgConsumed.body);
		//ch.queueDelete(queue);
		//ch.exchangeDelete(exchange);
		Assert.assertTrue(get_msg(ch,queue).indexOf(msg)>0);
		Assert.assertEquals(msg, msgConsumed.body);
	}
	
	String get_msg(Channel ch, String queue) throws IOException {
        Process process = Runtime.getRuntime().exec("docker exec rabbitmq4 rabbitmqadmin -u guest -p guest get queue="+queue);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        String str = "";
        while ((line = reader.readLine()) != null) {
            str += line;
        }
        System.out.println(str);
      	return str;
	}
	
	Message consume_auto_ack(Channel ch, String queue) throws IOException, InterruptedException {
		boolean autoAck = true;
		Message msg = new Message();
		String consumerTag = "a-consumer-tag";//consumerTag é um identificador para visualizar no monitoramento do broker
		ch.basicConsume(queue, autoAck, consumerTag, new DefaultConsumer(ch) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
				msg.body = new String(body, StandardCharsets.UTF_8);
				msg.deliveryTag = envelope.getDeliveryTag();
			}
		});
		Thread.sleep(2000);
		return msg;
	}
	
	Message consume_manual_ack(Channel ch, String queue) throws IOException, InterruptedException {
		boolean autoAck = false;
		Message msg = new Message();
		ch.basicConsume(queue, autoAck, "a-consumer-tag", new DefaultConsumer(ch) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
				msg.body = new String(body, StandardCharsets.UTF_8);
				msg.deliveryTag = envelope.getDeliveryTag();
				System.out.println(msg.deliveryTag);
				ch.basicAck(msg.deliveryTag, autoAck); //envia o delivery tag para o broker remover a mensagem, se enviar um deliveryTag = 1 e deveria ser 2, vai apenas apagar a mensagem 1 e a 2 ficará aguardando
			}
		});
		Thread.sleep(2000);
		return msg;
	}

}
