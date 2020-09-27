package in.neuw.oauth2;

import in.neuw.oauth2.config.OAuth2ClientSSLPropertiesConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import javax.net.ssl.HttpsURLConnection;
import java.util.Properties;

@SpringBootApplication(exclude = ReactiveUserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(OAuth2ClientSSLPropertiesConfigurer.class)
public class Client2Application {

    public static void main(String[] args) {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
        final Properties props = System.getProperties(); props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        SpringApplication.run(Client2Application.class, args);
    }

}
