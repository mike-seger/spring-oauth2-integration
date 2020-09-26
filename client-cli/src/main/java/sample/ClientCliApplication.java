package sample;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@SpringBootApplication
@Slf4j
public class ClientCliApplication implements CommandLineRunner {
 
    public static void main(String[] args) {
        log.info("STARTING THE APPLICATION");
        SpringApplication.run(ClientCliApplication.class, args).close();
        log.info("APPLICATION FINISHED");
    }

    public ClientCliApplication(WebClient webClient) {
        this.webClient=webClient;
    }

    final private WebClient webClient;
 
    @Override
    public void run(String... args) {
        log.info("EXECUTING : command line runner");
        String result = webClient.get()
            .uri("https://localhost:39001/hello")
            .exchange()
            .flatMap(response -> response.toEntity(String.class))
            .map(ResponseEntity::toString).block();
        log.info(result);
    }
}
