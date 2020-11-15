package sample.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//import org.springframework.http.client.reactive.ReactorClientHttpConnector;
//import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.*;
//import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
//import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
//import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
//import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
//import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
//import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
//import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
//import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
//import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.util.StringUtils;
//import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
//import reactor.netty.http.client.HttpClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@Slf4j
public class WebClientConfig {

//	@Bean
//	WebClient webClient(
//			OAuth2AuthorizedClientManager authorizedClientManager,
//			HttpClient httpClient) {
//		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
//			new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
//		httpClient = httpClient.wiretap(true);
//		return WebClient.builder()
//			.clientConnector(new ReactorClientHttpConnector(httpClient))
//			.apply(oauth2Client.oauth2Configuration())
//			.filter(oauth2Client)
//			.filter(logRequest())
//			.filter(logResponse())
//			.build();
//	}

	@Bean
	public WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
			new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
		oauth.setDefaultOAuth2AuthorizedClient(true);
		oauth.setDefaultClientRegistrationId("messaging-client-password");
		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(
					HttpClient.create().wiretap(true)
			))
			.filter(oauth)
			.filter(logRequest())
			.filter(logResponse())
			.build();
	}

	@Bean
	public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
		ReactiveClientRegistrationRepository clientRegistrationRepository,
		ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {

		ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
			ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
				.refreshToken()
				.password()
				.build();
		DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager =
			new DefaultReactiveOAuth2AuthorizedClientManager(
				clientRegistrationRepository, authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		// For the `password` grant, the `username` and `password` are supplied via request parameters,
		// so map it to `OAuth2AuthorizationContext.getAttributes()`.
		authorizedClientManager.setContextAttributesMapper(contextAttributesMapper());
		return authorizedClientManager;
	}

	private Function<OAuth2AuthorizeRequest, Mono<Map<String, Object>>> contextAttributesMapper() {
		return authorizeRequest -> {
			Map<String, Object> contextAttributes = Collections.emptyMap();
			ServerWebExchange serverWebExchange = authorizeRequest.getAttribute(ServerWebExchange.class.getName());
			String username = serverWebExchange.getRequest().getQueryParams().getFirst(OAuth2ParameterNames.USERNAME);
			String password = serverWebExchange.getRequest().getQueryParams().getFirst(OAuth2ParameterNames.PASSWORD);
			if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
				contextAttributes = new HashMap<>();
				// `PasswordOAuth2AuthorizedClientProvider` requires both attributes
				contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username);
				contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password);
			}
			return Mono.just(contextAttributes);
		};
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

//	@Bean
//	OAuth2AuthorizedClientManager authorizedClientManager(
//		org.apache.http.client.HttpClient httpClient,
//		ClientRegistrationRepository clientRegistrationRepository,
//		OAuth2AuthorizedClientRepository authorizedClientRepository) {
//
//		OAuth2AuthorizedClientProvider authorizedClientProvider =
//			OAuth2AuthorizedClientProviderBuilder.builder()
//				.clientCredentials(configurer ->
//					configurer.accessTokenResponseClient(clientCredentialsTokenResponseClient(httpClient)))
//				.build();
//
//		DefaultOAuth2AuthorizedClientManager authorizedClientManager =
//			new DefaultOAuth2AuthorizedClientManager(
//				clientRegistrationRepository, authorizedClientRepository);
//		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
//
//		return authorizedClientManager;
//	}
//
//	private OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient(
//			org.apache.http.client.HttpClient httpClient) {
//		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
//		requestFactory.setHttpClient(httpClient);
//		RestTemplate restTemplate = new RestTemplate(requestFactory);
//		restTemplate.setMessageConverters(Arrays.asList(
//			new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
//		restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
//
//		DefaultClientCredentialsTokenResponseClient clientCredentialsTokenResponseClient = new DefaultClientCredentialsTokenResponseClient();
//		clientCredentialsTokenResponseClient.setRestOperations(restTemplate);
//		return clientCredentialsTokenResponseClient;
//	}
}
