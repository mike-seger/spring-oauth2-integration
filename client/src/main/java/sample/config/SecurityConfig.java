package sample.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfig {
	@Bean
	public SecurityWebFilterChain securitygWebFilterChain(
			ServerHttpSecurity http) {
		return http
			.authorizeExchange(authorizeExchange -> authorizeExchange
				.pathMatchers("/favicon.ico", "/*/actuator/**", "/login", "/logout",
					"/explicit", "/authorize", "/auth/login", "/auth/logout", "/auth/logged",
					"/user/register", "/webjars/**").permitAll())
				.oauth2Client()
				.and().logout(logout -> {})
				.build();
	}
}
