package br.cin.ufpe.nlp.ssense.tagger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SuperSenseTaggerServer {
	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication(SuperSenseTaggerServer.class);
		app.run(args);
	}

}
