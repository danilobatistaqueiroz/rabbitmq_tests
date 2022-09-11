package br.com.labs.rabbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import com.rabbitmq.client.ReturnListener;

class MandatoryMessageTest {

	static Connection connection;

	@BeforeAll
	static void createConnection() throws IOException, TimeoutException {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("localhost");
		cf.setUsername("guest");
		cf.setPassword("guest");
		connection = cf.newConnection();
	}

	@Test
	void test_mandatory() throws IOException, InterruptedException {

		Channel ch = connection.createChannel();

		String queue = "q_manda";
		String exchange = "exchange3";

		// ** queue será dropada após 15 segundos **//
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("x-expires", 15000);

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

		String out = listqueues(ch, queue);
		Assert.assertTrue(out.indexOf(queue+'\t'+"1")>0);

		Thread.sleep(2000);

		Assert.assertEquals("ack", ack.value);
	}
	
	String listqueues(Channel ch, String queue) throws IOException {
        Process process = Runtime.getRuntime().exec("docker exec rabbitmq rabbitmqctl list_queues");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        String str = "";
        while ((line = reader.readLine()) != null) {
            str += line;
            System.out.println(line);
        }
        if(str!=null)
        	return str;
        else
        	return "";
	}

}
