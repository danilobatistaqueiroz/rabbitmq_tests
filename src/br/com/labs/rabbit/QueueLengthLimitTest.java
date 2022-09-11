package br.com.labs.rabbit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

class QueueLengthLimitTest {
	
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
	 * para o teste funcionar é preciso adicionar uma policy "max-length-bytes" na queue q_max_limit
	 * com essa policy as mensagens mais antigas serão descartadas ao chegar uma nova na queue
	 * rabbitmqctl set_policy pol-max-len-bytes "^q_max_limit_bytes$" '{"max-length-bytes":32}' --apply-to queues
	 */
	@Test
	void test_max_length_bytes() throws IOException, InterruptedException {
		
        Channel ch = connection.createChannel();

        //** para que a queue seja dropada após 10 segundos **//
        //** ha duas opções, via policy, ou na criação da queue usando args **//
        //rabbitmqctl set_policy queue_expiry "^q_max_limit_bytes$" '{"expires":10000}' --apply-to queues
        
        //** queue será dropada após 5 segundos **//
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-expires", 5000);
        
        String queue = "q_max_limit_bytes";
        Boolean exclusive = false; //exclusive só aceita uma conexão, e assim que é fechada, é deletada a queue
        Boolean autodelete = false; //autodelete assim que o último consumer ou publisher fechar a conexão, é deletada a queue
        ch.queueDeclare(queue, true, exclusive, autodelete, args);
        var ack = new Object(){ String value = ""; };
        
        /*** estoura o limite de 32 bytes na queue ****/
        for (int i = 0; i < 50; i++) {
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
        
        List<String> messages = new ArrayList<String>();
        var cnt = new Object(){ int value = 0; };
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
            cnt.value++;
            messages.add(message);
        };
        ch.basicConsume(queue, true, deliverCallback, consumerTag -> { });
        
        Thread.sleep(2000);
        Assert.assertEquals(4,cnt.value);
        Assert.assertEquals("ack",ack.value);
        Assert.assertEquals("[47, 48, 49, msg test_nack_length_limit]",messages.toString());
	}

	/*** *****
	 * para o teste funcionar é preciso adicionar uma policy "max-length" na queue q_max_limit
	 * com essa policy as mensagens mais novas serão descartadas ao chegar uma nova na queue
	 * rabbitmqctl set_policy pol-max-len-new "^q_max_limit_new$" '{"max-length":3,"overflow":"reject-publish"}' --apply-to queues
	 */
	@Test
	void test_new_msg_droped() throws IOException, InterruptedException {
		
        Channel ch = connection.createChannel();

        //** para que a queue seja dropada após 10 segundos **//
        //** ha duas opções, via policy, ou na criação da queue usando args **//
        //rabbitmqctl set_policy queue_expiry "^q_max_limit_new$" '{"expires":10000}' --apply-to queues
        
        //** queue será dropada após 5 segundos **//
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-expires", 5000);
        
        String queue = "q_max_limit_new";
        Boolean exclusive = false; //exclusive só aceita uma conexão, e assim que é fechada, é deletada a queue
        Boolean autodelete = false; //autodelete assim que o último consumer ou publisher fechar a conexão, é deletada a queue
        ch.queueDeclare(queue, true, exclusive, autodelete, args);
        var ack = new Object(){ String value = ""; };
        
        /*** estoura o limite de 3 msg na queue ****/
        for (int i = 0; i < 10; i++) {
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
        
        List<String> messages = new ArrayList<String>();
        var cnt = new Object(){ int value = 0; };
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
            cnt.value++;
            messages.add(message);
        };
        ch.basicConsume(queue, true, deliverCallback, consumerTag -> { });
        
        Thread.sleep(2000);
        Assert.assertEquals(3,cnt.value);
        Assert.assertEquals("nack",ack.value);
        Assert.assertEquals("[0, 1, 2]",messages.toString());
	}
	
	/*** *****
	 * para o teste funcionar é preciso adicionar uma policy "max-length" na queue q_max_limit
	 * com essa policy as mensagens mais antigas serão descartadas ao chegar uma nova na queue
	 * rabbitmqctl set_policy pol-max-len-old "^q_max_limit_old$" '{"max-length":6}' --apply-to queues
	 */
	@Test
	void test_old_msg_droped() throws IOException, InterruptedException {
		
        Channel ch = connection.createChannel();

        //** para que a queue seja dropada após 10 segundos **//
        //** ha duas opções, via policy, ou na criação da queue usando args **//
        //rabbitmqctl set_policy queue_expiry "^q_max_limit_old$" '{"expires":10000}' --apply-to queues
        
        //** queue será dropada após 5 segundos **//
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-expires", 5000);
        
        String queue = "q_max_limit_old";
        Boolean exclusive = false; //exclusive só aceita uma conexão, e assim que é fechada, é deletada a queue
        Boolean autodelete = false; //autodelete assim que o último consumer ou publisher fechar a conexão, é deletada a queue
        ch.queueDeclare(queue, true, exclusive, autodelete, args);
        var ack = new Object(){ String value = ""; };
        
        /*** estoura o limite de 6 msg na queue ****/
        for (int i = 0; i < 10; i++) {
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
        
        List<String> messages = new ArrayList<String>();
        var cnt = new Object(){ int value = 0; };
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
            cnt.value++;
            messages.add(message);
        };
        ch.basicConsume(queue, true, deliverCallback, consumerTag -> { });
        
        Thread.sleep(2000);
        Assert.assertEquals(6,cnt.value);
        Assert.assertEquals("ack",ack.value);
        Assert.assertEquals("[5, 6, 7, 8, 9, msg test_nack_length_limit]",messages.toString());
	}


}
