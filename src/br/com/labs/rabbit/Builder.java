package br.com.labs.rabbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Builder {
	
	static Connection createGuestConnection() throws IOException, TimeoutException {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost("localhost");
        cf.setUsername("guest");
        cf.setPassword("guest");
        return cf.newConnection();
    }
	
	static String getOutputRabbitmqctl(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("docker exec rabbitmq2 rabbitmqctl "+command);
//        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line = "";
        String str = "";
//        while ((line = reader.readLine()) != null) {
//            str += line;
//        }
//        System.out.println(str);
//        line = "";
//        str = "";
//        while ((line = error.readLine()) != null) {
//            str += line;
//        }
//        System.err.println(str);
        int exitCode = process.waitFor();
        System.out.println("\nExited with error code : " + exitCode);
      	return str;
	}
	
}
