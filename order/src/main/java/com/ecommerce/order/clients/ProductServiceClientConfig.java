package com.ecommerce.order.clients;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Optional;

@Configuration
public class ProductServiceClientConfig {

    /*
        @LoadBalanced makes the RestClient.Builder use Spring Cloud LoadBalancer, so http://product-service is treated as a service ID instead of a DNS host.
        At runtime it resolves instances from Eureka (or another registry) and routes each call to a healthy instance (e.g., round-robin).
        Without it, the call would try plain DNS and you’d lose service discovery + client-side load balancing.
     */

    @Bean
    public ProductServiceClient productServiceInterface(RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder
                            .baseUrl("http://product-service")
                            .defaultStatusHandler(HttpStatusCode::is4xxClientError,
                                        ((request, response) -> Optional.empty()))
                            .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                                            .builderFor(adapter)
                                            .build();
        return factory.createClient(ProductServiceClient.class);
    }
}
