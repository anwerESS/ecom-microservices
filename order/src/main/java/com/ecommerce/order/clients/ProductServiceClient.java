package com.ecommerce.order.clients;

import com.ecommerce.order.dtos.ProductResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;


/*
    @HttpExchange (Spring 6) marks an interface/class as a declarative HTTP client and can define shared settings (base URL/path, headers, content type) for all methods.
 */
@HttpExchange
public interface ProductServiceClient {


/*
    @GetExchange maps a specific method to an HTTP GET call with its path (here /api/products/{id}), binding parameters (e.g., @PathVariable) and return type.
    Think of it like a lightweight, built-in Feign: the proxy created by HttpServiceProxyFactory turns these annotated methods into real HTTP requests.
 */
    @GetExchange("/api/products/{id}")
    ProductResponse getProductDetails(@PathVariable String id);
}
