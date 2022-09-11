package br.com.labs.rabbit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
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

class BrokerNackTest {
	
	static Connection connection;
	
	@BeforeAll
	static void createConnection() throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost("localhost");
        cf.setUsername("guest");
        cf.setPassword("guest");
        connection = cf.newConnection();
    }
	
	/*** *****
	 * para o teste funcionar é preciso adicionar uma policy "max-length-bytes" na queue q_limit e overflow reject-publish
	 * rabbitmqctl set_policy pol-len-bytes "^q_limit$" '{"max-length-bytes":1048,"overflow":"reject-publish"}' --apply-to queues
	 */
	@Test
	void test_nack_length_limit() throws IOException, InterruptedException {
		
        Channel ch = connection.createChannel();

        String queue = "q_limit";
        ch.queueDeclare(queue, true, false, false, null);
        var ack = new Object(){ String value = ""; };
        
        /*** estoura o limite de 1048 bytes na queue ****/
        for (int i = 0; i < 400; i++) {
            String body = String.valueOf(i);
            ch.basicPublish("",queue, null, body.getBytes());
        }
        
	    ch.confirmSelect();
        ch.addConfirmListener(
		    (seq,mult) -> {
		    	ack.value = "ack";
		    }, 
		    (sequenceNumber, multiple) -> {
		    	ack.value = "nack";
		    }
        );
        String body = String.valueOf("msg test_nack_length_limit");
        ch.basicPublish("",queue, null, body.getBytes());
        
        Thread.sleep(2000);

        Assert.assertEquals("nack",ack.value);
	}


}
