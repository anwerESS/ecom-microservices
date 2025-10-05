package com.ecommerce.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

//@Component     // disable it temporarily
public class LoggingFilter implements GlobalFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("Incoming request to: {}", exchange.getRequest().getPath());
        return chain.filter(exchange);
    }
}










/*

  `GlobalFilter` is an interface for reactive, cross-cutting filters that run for every request passing through the gateway—regardless of the route.

  Package: org.springframework.cloud.gateway.filter.GlobalFilter
  Method: Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    ServerWebExchange = the current HTTP request/response context (WebFlux).
    GatewayFilterChain = lets you continue the pipeline (call chain.filter(exchange)), or short-circuit it.


  How it differs from GatewayFilter?
    `GatewayFilter`: applied per route (configured in application.yml or via RouteLocator).
    `GlobalFilter`: applied to all routes.


 */