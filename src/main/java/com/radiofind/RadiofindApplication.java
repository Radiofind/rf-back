package com.radiofind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class RadiofindApplication {

  public static void main(String[] args) {
    SpringApplication.run(RadiofindApplication.class, args);
  }

}
