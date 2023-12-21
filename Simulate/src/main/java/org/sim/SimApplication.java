package org.sim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

@SpringBootApplication
public class SimApplication {

	public static void main(String[] args) {
		try {
			System.setOut(new PrintStream(new FileOutputStream("../systemOut.txt")));
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			return;
		}
		SpringApplication.run(SimApplication.class, args);
	}

}
