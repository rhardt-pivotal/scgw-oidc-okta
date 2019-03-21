package io.pivotal.quickhitgateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.SetStatusGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import reactor.core.publisher.Mono;

/**
 * Pretty boring SCGW -> CF Route Service - just sends everything on its way.
 * But it gets a little more interesting once you wire oauth2 :)
 */
@SpringBootApplication
public class QuickHitGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuickHitGatewayApplication.class, args);
	}

	private static final Logger log = LoggerFactory.getLogger(QuickHitGatewayApplication.class.getName());

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder, DedupeResponseHeaderGatewayFilterFactory dedupe) {

		//@formatter:off
		return builder.routes()
				.route("carry_on_my_wayward_son", r -> r.alwaysTrue()
						.filters(f -> f.requestHeaderToRequestUri("X-CF-Forwarded-Url")
								.filter(dedupe.apply(dedupeConfig1()))
								.filter(dedupe.apply(dedupeConfig2()))
						)
						.uri("no://op"))
				.build();
		//@formatter:on
	}

	@Bean
	public GatewayFilter logFilter() {
		return ((exchange, chain) -> {
			log.info("*****SCGW PRE LOGGING");
			return chain.filter(exchange).then(Mono.fromRunnable(() -> {
				log.info("******SCGW POST LOGGING");
			}));
		});
	}

	@Bean
	public DedupeResponseHeaderGatewayFilterFactory.Config dedupeConfig1() {
		DedupeResponseHeaderGatewayFilterFactory.Config ret = new DedupeResponseHeaderGatewayFilterFactory.Config();
		ret.setStrategy(DedupeResponseHeaderGatewayFilterFactory.Strategy.RETAIN_FIRST);
		ret.setName("Access-Control-Allow-Credentials");
		return ret;
	}

	@Bean
	public DedupeResponseHeaderGatewayFilterFactory.Config dedupeConfig2() {
		DedupeResponseHeaderGatewayFilterFactory.Config ret = new DedupeResponseHeaderGatewayFilterFactory.Config();
		ret.setStrategy(DedupeResponseHeaderGatewayFilterFactory.Strategy.RETAIN_FIRST);
		ret.setName("Access-Control-Allow-Origin");
		return ret;
	}

	@Bean
	public DedupeResponseHeaderGatewayFilterFactory dedupeFactory() {
		return new DedupeResponseHeaderGatewayFilterFactory();
	}

}

