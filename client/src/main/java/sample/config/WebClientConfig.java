package sample.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@Slf4j
public class WebClientConfig {
	@Bean
	WebClient webClient(ReactiveClientRegistrationRepository clientRegistrationRepository) {
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
			getOAuth2FilterFunction(clientRegistrationRepository);
		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().wiretap(true)
			))
			.filter(oauth)
			.build();
	}

	private ServerOAuth2AuthorizedClientExchangeFilterFunction getOAuth2FilterFunction(
			ReactiveClientRegistrationRepository clientRegistrationRepository) {

		InMemoryReactiveOAuth2AuthorizedClientService authorizedClientService =
			new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
		AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
			new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
		authorizedClientManager.setAuthorizedClientProvider(
			new ClientCredentialsReactiveOAuth2AuthorizedClientProvider());

		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2FilterFunction =
			new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
		//oauth2FilterFunction.setDefaultClientRegistrationId("messaging-client");
		return oauth2FilterFunction;
	}
}
