package com.example.gcpcloudstreambug;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Slf4j
@SpringBootApplication
public class GcpCloudStreamBugApplication {

    public static void main(String[] args) {
        SpringApplication.run(GcpCloudStreamBugApplication.class, args);
    }

    @Bean
    public Consumer<Message<String>> test() {
        return (Message<String> message) -> log.info(message.getPayload());
    }
}
