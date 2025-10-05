## Key Differences

**`WebFilter`** and **`GlobalFilter`** serve different purposes in Spring's reactive ecosystem:

### WebFilter (Spring WebFlux)
- **Scope**: Applies to **all Spring WebFlux applications**
- **Part of**: Spring WebFlux framework (not gateway-specific)
- **Chain type**: Uses `WebFilterChain`
- **Exchange type**: Uses `ServerWebExchange`
- **Use cases**: 
  - Authentication/authorization
  - CORS handling
  - Request/response modification
  - Security concerns
  - Works in any WebFlux app, even without Spring Cloud Gateway

### GlobalFilter (Spring Cloud Gateway)
- **Scope**: Applies **only to Spring Cloud Gateway routes**
- **Part of**: Spring Cloud Gateway module
- **Chain type**: Uses `GatewayFilterChain`
- **Exchange type**: Uses `ServerWebExchange`
- **Use cases**:
  - Gateway-specific logic (routing decisions)
  - Request/response transformation for proxied requests
  - Load balancing
  - Circuit breaking
  - Rate limiting
  - Only works when Spring Cloud Gateway is present

## Execution Order

When both are present in a Gateway application:

1. **WebFilter** executes first (outermost layer)
2. **GlobalFilter** executes after (gateway routing layer)
3. Proxied request goes to downstream service



