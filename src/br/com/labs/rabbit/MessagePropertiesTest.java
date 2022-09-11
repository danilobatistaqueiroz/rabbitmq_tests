package br.com.labs.rabbit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

class MessagePropertiesTest {

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
	void test_message_properties() throws IOException, InterruptedException {
        Channel ch = connection.createChannel();
        String queue = "minhaqueue";
        String exchange = "minhaexchange";
        ch.queueDeclare(queue, true, false, false, null);
        ch.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT);
        
        Map<String,Object>headerMap = new HashMap<String,   Object>();
        headerMap.put("mandatory", true);

        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
        AMQP.BasicProperties messageProperties = propsBuilder
        .timestamp(new Date())
        .contentType("text/plain")
        .userId("guest")
        .appId("app id: 20")
        .deliveryMode(1)
        .priority(1)
        .headers(headerMap)
        .clusterId("cluster id: 1")
        .build();
        
        String body = String.valueOf("msg teste 1");
        ch.basicPublish(exchange,"xyz", messageProperties, body.getBytes());
	}

}
