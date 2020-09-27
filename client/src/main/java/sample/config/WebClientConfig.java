/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.Arrays;

/**
 * @author Joe Grandja
 * @since 0.0.1
 */
@Configuration
@Slf4j
public class WebClientConfig {

	@Value("${messages.base-uri}") String uri;

//	@Bean
//	WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager, HttpClient httpClient) {
//		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
//			new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
//		oauth.setDefaultClientRegistrationId("messaging-client-client-credentials");
//		//oauth.setDefaultOAuth2AuthorizedClient(true);
//		return WebClient.builder()
//				.clientConnector(new ReactorClientHttpConnector(httpClient))
////			.clientConnector(new HttpComponentsClientHttpRequestFactory(httpClient))
//				.filter(oauth)
//				.filter(logRequest())
//				.filter(logResponse())
//				.build();
//	}

	@Bean
	WebClient webClient(
			OAuth2AuthorizedClientManager authorizedClientManager,
			//ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
			/*org.apache.http.client.HttpClient*/HttpClient httpClient) {
		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
			new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(httpClient))
//			.clientConnector(new HttpComponentsClientHttpRequestFactory(httpClient))
			.apply(oauth2Client.oauth2Configuration())
			.filter(oauth2Client)
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

//	@Bean
//	OAuth2AuthorizedClientManager authorizedClientManager(
//		ClientRegistrationRepository clientRegistrationRepository,
//		OAuth2AuthorizedClientRepository authorizedClientRepository) {
//
//
//		OAuth2AuthorizedClientProvider authorizedClientProvider =
//			OAuth2AuthorizedClientProviderBuilder.builder()
//				.authorizationCode()
//				.clientCredentials()
//				.build();
//		DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
//			clientRegistrationRepository, authorizedClientRepository);
//		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
//		return authorizedClientManager;
//	}

/*
	@Bean
	public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
			ReactiveClientRegistrationRepository clientRegistrationRepository,
			ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {

		ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
				ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
						.authorizationCode()
						.refreshToken()
						.clientCredentials()
						.password()
						.build();

//		AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
//				new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
//						clientRegistrationRepository, authorizedClientRepository);

		DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager =
			new DefaultReactiveOAuth2AuthorizedClientManager(
				clientRegistrationRepository, authorizedClientRepository);

		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		return authorizedClientManager;
	}*/


	@Bean
	OAuth2AuthorizedClientManager authorizedClientManager(
			org.apache.http.client.HttpClient httpClient,
			ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientRepository authorizedClientRepository) {

		OAuth2AuthorizedClientProvider authorizedClientProvider =
				OAuth2AuthorizedClientProviderBuilder.builder()
						.clientCredentials(configurer ->
								configurer.accessTokenResponseClient(clientCredentialsTokenResponseClient(httpClient)))
						.build();

		DefaultOAuth2AuthorizedClientManager authorizedClientManager =
				new DefaultOAuth2AuthorizedClientManager(
						clientRegistrationRepository, authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		return authorizedClientManager;
	}

	private OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient(
			org.apache.http.client.HttpClient httpClient) {
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		restTemplate.setMessageConverters(Arrays.asList(
			new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
		restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

		DefaultClientCredentialsTokenResponseClient clientCredentialsTokenResponseClient = new DefaultClientCredentialsTokenResponseClient();
		clientCredentialsTokenResponseClient.setRestOperations(restTemplate);
		return clientCredentialsTokenResponseClient;
	}
}
