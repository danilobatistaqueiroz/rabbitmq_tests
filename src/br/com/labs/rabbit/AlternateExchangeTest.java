package br.com.labs.rabbit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

class AlternateExchangeTest {

	static Connection connection;
	
	@BeforeAll
	static void createConnection() throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost("localhost");
        cf.setUsername("guest");
        cf.setPassword("guest");
        connection = cf.newConnection();
    }
    
	/**** ********************************
	 * para que o teste funcione precisa aplicar a policy
	 * rabbitmqctl set_policy AE "^minhaexchange$" '{"alternate-exchange":"minha-ae"}' --apply-to exchanges
	 */
	@Test
	void test_send_alternate_exchange() throws IOException, InterruptedException {
        Channel ch = connection.createChannel();

        String queue = "minhaqueue";
        String exchange = "minhaexchange";
        ch.queueDeclare(queue, true, false, false, null);
        ch.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT);
        ch.exchangeDeclare("minha-ae", BuiltinExchangeType.FANOUT);
        ch.queueDeclare("minha-q-ae", true, false, false, null);
        ch.queueBind("minha-q-ae", "minha-ae", "");
        ch.queueBind("minhaqueue", "minhaexchange", "tests");

        ch.confirmSelect();
        var ack = new Object(){ String value = ""; };
        ConfirmCallback okConfirms = (sequenceNumber, multiple) -> {
        	ack.value = "ack";
        };
        ConfirmCallback errConfirms = (sequenceNumber, multiple) -> {
        	ack.value = "nack";
        };
        ch.addConfirmListener(okConfirms, errConfirms);

        String body = String.valueOf("msg teste 1");
        /** a rota não existe, e portanto, a exchange tendo uma policy de alternate-exchange, a mensagem é redirecionada **/ 
        ch.basicPublish(exchange,"xyz", null, body.getBytes());
        
        Thread.sleep(2000);

        Assert.assertEquals("ack",ack.value);
	}

}
