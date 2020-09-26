package sample;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@Slf4j
public class ClientCliApplication implements CommandLineRunner {
 
    public static void main(String[] args) {
        SpringApplication.run(ClientCliApplication.class, args).close();
    }

    public ClientCliApplication(WebClient webClient, @Value("${app.test-url}") String testUrl) {
        this.webClient = webClient;
        this.testUrl=testUrl;
    }

    final private String testUrl;
    final private WebClient webClient;
 
    @Override
    public void run(String... args) {
        String result = webClient.get()
            .uri(testUrl)
            .exchange()
            .flatMap(response -> response.bodyToMono(String.class))
            .block();
        log.info(result);
    }
}
