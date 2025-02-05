package com.eazybytes.gatewayserver;

import java.time.*;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class GatewayserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayserverApplication.class, args);
	}

	@Bean
	public RouteLocator eazyBankRouteConfig(RouteLocatorBuilder routeLocatorBuilder){
		return routeLocatorBuilder
			.routes()
			.route((path) -> path
				.path("/eazybank/accounts/**")
				.filters((filter) -> filter
					.rewritePath("/eazybank/accounts/(?<segment>.*)", "/${segment}")
					.addResponseHeader("X-Response-Time", LocalDate.now().toString())
					.circuitBreaker((config) -> config
						.setName("accountsCircuitBreaker")
						.setFallbackUri("forward:/contactSupport")
					)
				)
				.uri("lb://ACCOUNTS")
			)
			.route((path) -> path
				.path("/eazybank/loans/**")
				.filters((filter) -> filter
					.rewritePath("/eazybank/loans/(?<segment>.*)", "/${segment}")
					.addResponseHeader("X-Response-Time", LocalDate.now().toString())
					.retry((retryConfig) -> retryConfig
						.setRetries(3) //1 + 3.
						.setMethods(HttpMethod.GET)
						.setBackoff(
							Duration.ofMillis(100),  //FirstBackoff: This is the initial waiting time before the first retry attempt.
							Duration.ofMillis(1000),  //MaxBackoff: This is the maximum waiting time between retries.
							2,                       //Factor: This is the multiplier used to calculate the next wait time.
							true                     //BasedOnPreviousValue: If true: Each new interval is calculated by multiplying the previous interval by the factor. If false: Each new interval is calculated by multiplying the first interval by the factor raised to the retry number
						)
						// .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY)
						// .setExceptions(IOException.class, TimeoutException.class)
					)
				)
				.uri("lb://LOANS")
			)
			.route((path) -> path
				.path("/eazybank/cards/**")
				.filters((filter) -> filter
					.rewritePath("/eazybank/cards/(?<segment>.*)", "/${segment}")
					.addResponseHeader("X-Response-Time", LocalDate.now().toString())
					.requestRateLimiter((config) -> config
						.setRateLimiter(redisRateLimiter())
						.setKeyResolver(userKeyResolver())
					)
				)
				.uri("lb://CARDS")
			)
			.build();
	}

	//Time Out Circuit Breaker.
	@Bean
	public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
		return (factory) -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
			.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
			.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(10)).build()).build()); //Con 4 segundos se dispara el fallback del Gateway. Con 10 segundos se dispara el propio fallback del MS. Siempre se va a disparar el que tarde menos.
	}

	@Bean
	public RedisRateLimiter redisRateLimiter(){
		return new RedisRateLimiter(1, 1, 1);
		//Parameters.
		//1. DefaultReplenishRate. How many tokens per second in token-bucket algorithm.
		//2. DefaultBurstCapacity. How many tokens the bucket can hold in token-bucket algorithm.
		//3. DefaultRequestedTokens. How many tokens are requested per request.
	}

	@Bean
	KeyResolver userKeyResolver(){
		return (exchange) -> Mono
			.justOrEmpty(exchange.getRequest().getHeaders().getFirst("user"))
			.defaultIfEmpty(("anonymous"));
			//TODO: Acá debería estar el token o algo que identifique al usuario. Si se deja en "anonymous", el backend va a considerar que TODAS las requests vienen de la misma persona y no podrá crearle un budget de tokens a cada solicitante.
	}

}
