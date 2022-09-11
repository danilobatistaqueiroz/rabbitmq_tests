package br.com.labs.rabbit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

class ProcessTest {

	@Test
	void test_process_builder() throws IOException {
        //ProcessBuilder builder = new ProcessBuilder("docker","exec","-i","rabbitmq3","bash");
        ProcessBuilder builder = new ProcessBuilder("docker","exec","rabbitmq2","rabbitmqctl","set_policy","DLX",".*","{\"dead-letter-exchange\":\"minha-dlx-queue\"}", "--apply-to","queues");
        Process process = builder.start();

        OutputStream stdin = process.getOutputStream(); // <- Eh?
        InputStream stdout = process.getInputStream();
        InputStream stderr = process.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
        BufferedReader error = new BufferedReader(new InputStreamReader(stderr));

        writer.write("rabbitmqctl list_queues \n");
        writer.flush();
        writer.close();

        Scanner err = new Scanner(stderr);
        while (err.hasNextLine()) {
            System.out.println(err.nextLine());
        }
        
        Scanner scanner = new Scanner(stdout);
        while (scanner.hasNextLine()) {
            System.out.println(scanner.nextLine());
        }
	}

}
