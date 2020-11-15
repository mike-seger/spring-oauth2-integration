package sample;

import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.sslcontext.SSLFactory;
import nl.altindag.sslcontext.util.NettySslContextUtils;
//import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

import static java.util.Objects.nonNull;

@Component
@Slf4j
public class ClientConfig {
    @Bean
    @Scope("prototype")
    public HttpClient nettyHttpClient(@Autowired(required = false) SSLFactory sslFactory) throws SSLException {
        var httpClient = HttpClient.create();
        if (nonNull(sslFactory)) {
            SslContext sslContext = NettySslContextUtils.forClient(sslFactory).build();
            httpClient = httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));
        }
        return httpClient;
    }

//    @Bean
//    @Scope("prototype")
//    public org.apache.http.client.HttpClient apacheHttpClient(@Autowired(required = false) SSLFactory sslFactory) {
//        if (nonNull(sslFactory)) {
//            return HttpClients.custom()
//                .setSSLContext(sslFactory.getSslContext())
//                .setSSLHostnameVerifier(sslFactory.getHostnameVerifier())
//                .build();
//        } else {
//            return HttpClients.createMinimal();
//        }
//    }

}
