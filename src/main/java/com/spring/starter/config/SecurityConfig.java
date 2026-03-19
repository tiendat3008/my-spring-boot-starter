package com.spring.starter.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtProperties jwtProperties;
	private final ResourceLoader resourceLoader;

	private static final String[] PUBLIC_ENDPOINTS = {
			"/api/v1/auth/**",
			"/actuator/health"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
			CustomAccessDeniedHandler customAccessDeniedHandler
	) throws Exception {
		
		http
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						.requestMatchers("/admin/filter").hasRole("ADMIN")
						.anyRequest().authenticated()
				)
				.csrf(AbstractHttpConfigurer::disable)
				.oauth2ResourceServer((oauth2) -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(customAuthenticationEntryPoint)
				)
				.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling((exceptions) -> exceptions
						.authenticationEntryPoint(customAuthenticationEntryPoint)
						.accessDeniedHandler(customAccessDeniedHandler)
				);

		return http.build();
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter =
				new JwtGrantedAuthoritiesConverter();

		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

		return converter;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
		return NimbusJwtDecoder.withPublicKey(publicKey).build();
	}

	@Bean
	JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
		JWK jwk = new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.build();

		JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));

		return new NimbusJwtEncoder(jwks);
	}

	@Bean
	RSAPublicKey rsaPublicKey() {
		return loadKey(jwtProperties.publicKey(), RsaKeyConverters.x509());
	}

	@Bean
	RSAPrivateKey rsaPrivateKey() {
		return loadKey(jwtProperties.privateKey(), RsaKeyConverters.pkcs8());
	}

	private <T> T loadKey(String location, Converter<InputStream, T> converter) {
		Resource resource = resourceLoader.getResource(location);

		try (InputStream inputStream = resource.getInputStream()) {
			return converter.convert(inputStream);
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to load key from: " + location, ex);
		}
	}
}
