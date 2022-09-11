package br.com.labs.rabbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

class ShellTest {

	@Test
	void test() throws IOException {
        //ProcessBuilder processBuilder = new ProcessBuilder();
        //processBuilder.command("bash", "-c", "docker exec -it rabbitmq bash");
        try {
            Process process = Runtime.getRuntime().exec("docker exec -i rabbitmq3 bash");
            //Process process = processBuilder.start();
            Writer w = new OutputStreamWriter(process.getOutputStream(), "UTF-8");
            w.write("rabbitmqctl list_queues\n");
            w.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            System.out.println("lines");
            int cnt = 0;
            while ((line = reader.readLine()) != null) {
            	cnt++;
            	System.out.println(cnt);
                System.out.println(line);
            }
            
            //BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            //String errorString = error.readLine();
            //System.out.println("\nError : " + errorString);

            //int exitCode = process.waitFor();
            //System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException e) {
            e.printStackTrace();
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        }
	}

}
