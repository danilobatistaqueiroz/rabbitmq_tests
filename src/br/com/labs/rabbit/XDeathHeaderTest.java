package br.com.labs.rabbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

import br.com.labs.rabbit.PublishConsumeTest.Message;

class XDeathHeaderTest {

	static Connection connection;
	static String queue = "minha-queue";
	static String queueDLX = "minha-dlx-queue";
	static String exchange = "minha-exchange";
	static String exchangeDLX = "minha-dlx-exchange";
	
	@BeforeAll
	static void setUp() throws IOException, TimeoutException, InterruptedException {
		connection = Builder.createGuestConnection();
		Builder.getOutputRabbitmqctl("""
		set_policy DLX .* {"dead-letter-exchange":"%s"} --apply-to queues
		""".formatted(exchangeDLX));
    }
	@AfterAll
	static void setDown() throws IOException, InterruptedException {
		Thread.sleep(1000);
		Builder.getOutputRabbitmqctl("""
		clear_policy DLX
		""");
	}
	
	@Test
	void dead_letter_message_ttl() throws IOException, InterruptedException {
		
        Channel ch = connection.createChannel();
        
        Map<String, Object> args = queueExpires();
        
        ch.queueDeclare(queue, true, false, false, args);
        boolean durable = false, autoDelete = true, internal = false; //durable exchange survive broker restart //autoDelete after all queues and exchanges binded are unbinded is deleted // internal only accept bind to another exchange
        ch.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT,durable,autoDelete,internal,null);
        ch.exchangeDeclare(exchangeDLX, BuiltinExchangeType.DIRECT,durable,autoDelete,internal,null);
        ch.queueDeclare(queueDLX, true, false, false, args);
        ch.queueBind(queueDLX, exchangeDLX, "tests");
        ch.queueBind(queue, exchange, "tests");

        ch.confirmSelect();
        var ack = new Object(){ String value = ""; };
        ConfirmCallback okConfirms = (sequenceNumber, multiple) -> {
        	ack.value = "ack";
        };
        ConfirmCallback errConfirms = (sequenceNumber, multiple) -> {
        	ack.value = "nack";
        };
        ch.addConfirmListener(okConfirms, errConfirms);

        AMQP.BasicProperties messageProperties = setTTLPerMessage();
       
        String body = String.valueOf("test_dead_letter_message_ttl");
        ch.basicPublish(exchange,"tests", messageProperties, body.getBytes());
        
        Thread.sleep(1500);
        
        consume_auto_ack(ch,queueDLX);
        
        Assert.assertTrue(hasMessage(queueDLX));
        Assert.assertFalse(hasMessage(queue));
        Assert.assertEquals("ack",ack.value);
	}
	
	Map<String, Object> queueExpires() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-expires", 4000); //é possível expirar uma queue após um tempo sem uso, sem um client conectar, após a expiração, a queue é dropada
        return args;
	}
	
	AMQP.BasicProperties setTTLPerMessage() {
        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
        return propsBuilder.expiration("1000").build();
	}
	
	void consume_auto_ack(Channel ch, String queue) throws IOException, InterruptedException {
		boolean autoAck = true;
		String consumerTag = "a-consumer-tag";
		ch.basicConsume(queue, autoAck, consumerTag, new DefaultConsumer(ch) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
				System.out.println("redeliver: "+envelope.isRedeliver());
				String xDeath = properties.getHeaders().getOrDefault("x-death","").toString();
				System.out.println(xDeath);
				JsonObject jsonObject = JsonParser.parseString(xDeath).getAsJsonObject();
				System.out.println(jsonObject.get("queue"));
				System.out.println(jsonObject.get("reason"));
				System.out.println(jsonObject.get("time"));
				System.out.println(jsonObject.get("exchange"));
				System.out.println(jsonObject.get("routing-keys"));
				System.out.println(jsonObject.get("count"));
				System.out.println(jsonObject.get("original-expiration"));
			}
		});
	}
	
	boolean hasMessage(String queuename) throws IOException {
        Process process = Runtime.getRuntime().exec("docker exec rabbitmq2 rabbitmqadmin get queue=%s".formatted(queuename));
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        String str = "";
        while ((line = reader.readLine()) != null) {
            str += line;
        }
        if (str.indexOf("No items")>=0 || str.indexOf("Not found")>=0)
        	return false;
        else
        	return true;
	}

}
