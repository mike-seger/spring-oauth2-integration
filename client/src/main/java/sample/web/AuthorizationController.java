package sample.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

//import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
//import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Controller
public class AuthorizationController {
	private final WebClient webClient;
	private final String messagesBaseUri;

	public AuthorizationController(WebClient webClient,
			@Value("${messages.base-uri}") String messagesBaseUri) {
		this.webClient = webClient;
		this.messagesBaseUri = messagesBaseUri;
	}

	@GetMapping(value = "/authorize", params = "grant_type=authorization_code")
	public String authorizationCodeGrant(Model model,
			@RegisteredOAuth2AuthorizedClient("messaging-client-authorization-code")
			OAuth2AuthorizedClient authorizedClient) {

		String[] messages = webClient
			.get()
			.uri(messagesBaseUri)
			.attributes(oauth2AuthorizedClient(authorizedClient))
			.retrieve()
			.bodyToMono(String[].class)
			.block();
		model.addAttribute("messages", messages);

		return "index";
	}

	@GetMapping(value = "/authorize", params = "grant_type=password")
	public String passwordGrant(Model model,
			@RegisteredOAuth2AuthorizedClient("messaging-client-password")
			OAuth2AuthorizedClient authorizedClient) {

		String[] messages = webClient
			.get()
			.uri(messagesBaseUri)
			.attributes(oauth2AuthorizedClient(authorizedClient))
			.retrieve()
			.bodyToMono(String[].class)
			.block();
		model.addAttribute("messages", messages);
		return "index";
	}

	@GetMapping(value = "/authorize", params = "grant_type=client_credentials")
	public String clientCredentialsGrant(Model model) {

		String[] messages = webClient
			.get()
			.uri(this.messagesBaseUri)
			.attributes(clientRegistrationId("messaging-client-client-credentials"))
			.retrieve()
			.bodyToMono(String[].class)
			.block();
		model.addAttribute("messages", messages);

		return "index";
	}

	@GetMapping("/explicit")
	public Mono<String[]> explicit() {
		return this.webClient
			.get()
			.uri("http://localhost:8092/messages")
			.attributes(clientRegistrationId("messaging-client-password"))
			.retrieve()
			.bodyToMono(String[].class);
	}

	private OAuth2AuthorizedClient authorizedClient;
	@GetMapping("/token")
	public String token(@RegisteredOAuth2AuthorizedClient("messaging-client-password") OAuth2AuthorizedClient authorizedClient) {
		this.authorizedClient = authorizedClient;
		return authorizedClient.getAccessToken().getTokenValue();
	}
}
