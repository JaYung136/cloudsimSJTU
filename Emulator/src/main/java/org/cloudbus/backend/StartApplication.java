package org.cloudbus.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

@SpringBootApplication(exclude= {SecurityAutoConfiguration.class, DataSourceAutoConfiguration.class})
public class StartApplication {
    public static void main(String[] args) {
//        try {
//            System.setOut(new PrintStream(new FileOutputStream("../Emulator-log.txt", false)));
//        } catch (FileNotFoundException ex) {
//            ex.printStackTrace();
//            return;
//        }

        SpringApplication.run(StartApplication.class, args);
    }
}
