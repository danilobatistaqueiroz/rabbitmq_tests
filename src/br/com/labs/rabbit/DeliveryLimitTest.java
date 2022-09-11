package br.com.labs.rabbit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ReturnCallback;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.AMQP.BasicProperties;

class DeliveryLimitTest {

	static Connection connection;
	
	/*****************************
	 * 
	 * Será necessário criar uma Quorum Queue para esse cenário
	 * 
	 * @throws IOException
	 * @throws TimeoutException
	 */

	@BeforeAll
	static void createConnection() throws IOException, TimeoutException {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("localhost");
		cf.setUsername("guest");
		cf.setPassword("guest");
		connection = cf.newConnection();
	}

	/*** ***************************
	 * Uma mensagem mandatory, se não for roteada para alguma queue, ela volta pelo ReturnListener.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	void test_mandatory() throws IOException, InterruptedException {

		Channel ch = connection.createChannel();

		String queue = "qquorum1";
		String exchange = "exchange3";

		// ** queue será dropada após 5 segundos **//
		Map<String, Object> args = new HashMap<String, Object>();
		//args.put("x-expires", 15000);

		ch.queueDeclare(queue, true, false, false, args);
		ch.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT);
		ch.queueBind(queue, exchange, "tests");

		ch.confirmSelect();
		var ack = new Object() {
			String value = "";
		};
		ConfirmCallback okConfirms = (sequenceNumber, multiple) -> {
			ack.value = "ack";
		};
		ConfirmCallback errConfirms = (sequenceNumber, multiple) -> {
			ack.value = "nack";
		};
		ch.addConfirmListener(okConfirms, errConfirms);

		String body = String.valueOf("msg test_mandatory_message");
		Boolean mandatory = true;
		ch.addReturnListener(new ReturnListener() {
			@Override
			public void handleReturn(int replyCode, String replyText, String exchange, String routingKey,
					AMQP.BasicProperties basicProperties, byte[] body) throws IOException {
				System.out.println(replyCode);
				System.out.println(replyText);
				System.out.println(exchange);
				System.out.println(routingKey);
				System.out.println(new String(body, StandardCharsets.UTF_8));
			}
		});
		ch.basicPublish(exchange, "tests", mandatory, null, body.getBytes());

		consume(ch, queue);

		Thread.sleep(2000);

		Assert.assertEquals("ack", ack.value);
	}

	//rabbitmqctl set_policy delivery-limit "^q_max_requeue$" '{"delivery-limit":3}' --apply-to queues
	void consume(Channel ch, String queue) throws IOException {
		boolean autoAck = false;
		ch.basicConsume(queue, autoAck, "a-consumer-tag", new DefaultConsumer(ch) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
					throws IOException {
				String s = new String(body, StandardCharsets.UTF_8);
				System.out.println("recebido: " + s);
				long deliveryTag = envelope.getDeliveryTag();
				boolean multiple = true, requeue = true;
				ch.basicNack(deliveryTag, multiple, requeue);
				//ch.basicAck(deliveryTag, autoAck);
				try {
					Thread.sleep(10);
				} catch (InterruptedException ex) {
					System.out.println("erro no sleep");
				}
			}
		});
	}

}
