package sample;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.sslcontext.SSLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * @author Karanbir Singh on 07/18/2020
 */
//@Configuration
@Slf4j
public class WebClientConfig {

//    @Value("${test.client.base.url}")
//    private String testClientBaseUrl;

    @Autowired
    private SslContext sslContext;
//    @Autowired
//    private OAuth2ClientSSLPropertiesConfigurer oAuth2ClientSSLPropertiesConfigurer;

    /**
     * The authorizedClientManager for required by the webClient
     */
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(final ReactiveClientRegistrationRepository clientRegistrationRepository,
                                                                         final ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    private ClientHttpConnector getHttpCClientHttpConnector(String registrationId) {
        // create httpClient based on customized SSLContext
        // where SSLContext is constructed based on the properties in the properties file
        // refer OAuth2ClientSSLPropertiesConfigurer for that

        HttpClient httpClient = HttpClient.create()
                .wiretap(true)
                .tcpConfiguration(client -> client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000))
                .secure(sslContextSpec -> {
                            // retrieve the sslContext from the oAuth2ClientSSLPropertiesConfigurer
                            // based on the registrationId
                            sslContextSpec.sslContext(sslContext);
                        }
                );
        ClientHttpConnector httpConnector = new ReactorClientHttpConnector(httpClient);
        return httpConnector;
    }

    /**
     * The Oauth2 based WebClient bean for the web service
     */
    @Bean("testWebClient")
    public WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
                               ReactiveClientRegistrationRepository clientRegistrationRepository,
                               ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {

        String registrationId = "local";

        ReactiveOAuth2AuthorizedClientManager authorizedClientManagerForTestClient = authorizedClientManager;

        // if the oAuth2ClientSSLPropertiesConfigurer has the ssl-enabled property for the registrationId
        // example local in the case
 /*       if (oAuth2ClientSSLPropertiesConfigurer.getRegistration().containsKey(registrationId)
                && oAuth2ClientSSLPropertiesConfigurer.getRegistration().get(registrationId).getSslEnabled())*/ {

            WebClientReactiveClientCredentialsTokenResponseClient accessTokenResponseClient = new WebClientReactiveClientCredentialsTokenResponseClient();

            WebClient webClient = WebClient.builder().clientConnector(getHttpCClientHttpConnector(registrationId)).build();

            accessTokenResponseClient.setWebClient(webClient);

            // create custom authorizedClientProvider based on custom accessTokenResponseClient
            ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder
                    .builder()
                    .clientCredentials(c -> {
                        c.accessTokenResponseClient(accessTokenResponseClient);
                    }).build();

            // override default authorizedClientManager based on custom authorizedClientProvider
            authorizedClientManagerForTestClient = new DefaultReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);

            ((DefaultReactiveOAuth2AuthorizedClientManager) authorizedClientManagerForTestClient).setAuthorizedClientProvider(authorizedClientProvider);
        }

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManagerForTestClient);
        // for telling which registration to use for the webclient
        oauth.setDefaultClientRegistrationId(registrationId);

        /*KeyManagerFactory keyManagerFactory = SSLContextService.getKeyStore(encodedKeystoreString, keystorePassword);

        TrustManagerFactory trustManagerFactory = SSLContextService.getTrustStore(encodedTruststoreString, truststorePassword);

        SslContext sslContext = SSLContextService.sslContext(keyManagerFactory, trustManagerFactory);

        HttpClient resourceServerHttpClient = HttpClient.create()
                .tcpConfiguration(client -> client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000))
                .secure(sslContextSpec -> {
                    sslContextSpec.sslContext(oAuth2ClientSSLPropertiesConfigurer.getConstructedSslContexts().get(registrationId));
                });
        ClientHttpConnector customHttpConnector = new ReactorClientHttpConnector(resourceServerHttpClient);*/

        return WebClient.builder()
                // base path of the client, this way we need to set the complete url again
              //  .baseUrl(testClientBaseUrl)
                .clientConnector(getHttpCClientHttpConnector(registrationId))
                //.clientConnector(customHttpConnector)
                .filter(oauth)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(c -> {
            log.info("Request: {} {}. Headers:{}", c.method(), c.url(), c.headers());
            return Mono.just(c);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(c -> {
            log.info("Response: {} {}", c.statusCode(), getHeaders(c.headers()));
            return Mono.just(c);
        });
    }

    private String getHeaders(ClientResponse.Headers headers) {
        return headers.asHttpHeaders().toString();
    }

//    @Bean
//    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//        return http.oauth2Client().and().build();
//    }

}
