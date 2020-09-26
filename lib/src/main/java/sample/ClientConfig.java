package sample;

import io.netty.handler.ssl.SslContext;
import nl.altindag.sslcontext.SSLFactory;
import nl.altindag.sslcontext.util.NettySslContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import static java.util.Objects.nonNull;

@Component
public class ClientConfig {
    @Bean
    @Scope("prototype")
    public HttpClient nettyHttpClient(@Autowired(required = false) SSLFactory sslFactory) throws SSLException {
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create();
        if (nonNull(sslFactory)) {
            SslContext sslContext = NettySslContextUtils.forClient(sslFactory).build();
            httpClient = httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));
        }
        return httpClient;
    }

    @Bean
    public WebClient webClientWithNetty(HttpClient httpClient) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

}
