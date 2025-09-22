Awesome—here’s a compact, end-to-end tutorial that puts everything together: create a keystore, wire it into a Spring Cloud Config Server, pull configs from GitHub, encrypt sensitive values, and have the server transparently decrypt them for clients.

# Securing Spring Cloud Config with a Keystore (Git backend + encryption)

## 0) What you’ll build

* A **Config Server** (Spring Boot + `spring-cloud-config-server`) that:

  * Reads application configs from **GitHub** (private or public).
  * Uses an **RSA keypair** from a **keystore** to encrypt/decrypt secrets.
  * Optionally **secures** `/encrypt` & `/decrypt` endpoints (or disables them).

## 1) Prerequisites

* Java 17+ (for Spring Boot 3.x)
* Spring Boot 3.x, Spring Cloud 2023.x (a.k.a. Leyton)
* Git repo for configs (e.g., `https://github.com/you/your-configs`)
* `keytool` available on your machine

## 2) Generate a keystore (PKCS12 recommended)

```bash
keytool -genkeypair \
  -alias config-server-key \
  -keyalg RSA \
  -keysize 2048 \
  -dname "CN=Config Server,OU=Spring Cloud,O=Company" \
  -keypass changeit \
  -keystore config-server.p12 \
  -storetype PKCS12 \
  -storepass changeit
```

* **Keystore file**: `config-server.p12`
* **Alias**: `config-server-key`
* **Passwords**: use strong values in real setups and keep them out of git.

> You can also use JKS (`.jks`)—PKCS12 is just more modern/interoperable.

## 3) Create the Config Server project

**Dependencies** (Maven):

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
  </dependency>

  <!-- Optional but recommended if you want to secure endpoints -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>

  <!-- Actuator helps you see health/info -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
</dependencies>
```

**Main class**:

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(ConfigServerApplication.class, args);
  }
}
```

## 4) Wire GitHub + Keystore in `application.yml`

Put the **keystore file** on the classpath (`src/main/resources/config-server.p12`) or mount it as a file and reference it via `file:` URL.

```yaml
server:
  port: 8888

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: https://github.com/you/your-configs
          # If private, use one of the following:
          # username: your-username
          # password: your-personal-access-token
          # or SSH:
          # uri: git@github.com:you/your-configs.git
          # privateKey: |
          #   -----BEGIN OPENSSH PRIVATE KEY-----
          #   ...
          #   -----END OPENSSH PRIVATE KEY-----
          searchPaths: .
        encrypt:
          key-store:
            location: classpath:config-server.p12
            alias: config-server-key
            password: changeit
            type: PKCS12
# To disable encryption endpoints entirely:
#        encrypt:
#          enabled: false

management:
  endpoints:
    web:
      exposure:
        include: health,info # add encrypt,decrypt temporarily if you want to call them
```

> **Why `/encrypt` & `/decrypt` appear without controllers?**
> They’re auto-registered by Spring Cloud Config’s encryption support (auto-configuration provides `EnvironmentEncryptorController`). You don’t write a controller yourself.

## 5) (Optional) Secure endpoints (recommended)

If you expose `/encrypt` and `/decrypt` at all, **lock them down**. A minimal basic-auth config:

**`application.yml` additions**

```yaml
spring:
  security:
    user:
      name: configadmin
      password: super-strong-password
```

**`SecurityConfig`** (Boot 3 / Spring Security 6 style):

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/encrypt", "/decrypt").hasRole("ADMIN")
        .anyRequest().authenticated()
      )
      .httpBasic(Customizer.withDefaults());
    return http.build();
  }

  // Map the in-memory user to ROLE_ADMIN if using properties-based user
  @Bean
  UserDetailsService users(@Value("${spring.security.user.name}") String user,
                           @Value("${spring.security.user.password}") String pass) {
    var uds = new InMemoryUserDetailsManager();
    uds.createUser(User.withUsername(user).password("{noop}"+pass).roles("ADMIN").build());
    return uds;
  }
}
```

> Adjust to your org standards (LDAP/OIDC, network ACLs, or disable endpoints in prod).

## 6) Structure your Git config repo

A typical repo:

```
your-configs/
  application.yml
  orderservice.yml
  orderservice-dev.yml
  inventoryservice.yml
```

Clients request configs like:

```
/{application}/{profile}        -> /orderservice/dev
/{application}-{profile}.yml    -> file name style
```

## 7) Encrypt a secret and store it in Git

1. **Start the Config Server**.
2. **Encrypt** a value using the keystore:

```bash
# Basic auth if secured
curl -u configadmin:super-strong-password \
     -X POST http://localhost:8888/encrypt \
     -d 'my-very-secret-password'
```

This returns something like:

```
AQB2LrK...<long-cipher>...
```

3. **Store the cipher text** in your Git config as:

```yaml
# in orderservice-dev.yml (for example)
spring:
  datasource:
    password: "{cipher}AQB2LrK...the-long-cipher..."
```

Commit and push to GitHub.

## 8) How clients receive decrypted values

When a client (e.g., `orderservice` with profile `dev`) calls the Config Server:

```
GET http://localhost:8888/orderservice/dev
```

the Config Server:

* Clones/pulls the Git repo
* Reads `orderservice-dev.yml`
* Detects `{cipher}...`
* **Decrypts** it using the keystore
* Returns **plain** values to the client (over HTTP response)

**Client bootstrap (Spring Boot 3.x):**
Add the client dependency and point it to the server:

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

`application.yml` (client):

```yaml
spring:
  application:
    name: orderservice

  config:
    import: optional:configserver:http://localhost:8888

# If your Config Server is secured:
  cloud:
    config:
      username: configadmin
      password: super-strong-password
```

> With this, the client will fetch decrypted config on startup. If you use Actuator on the client, you can trigger refresh with `/actuator/refresh` if you’re also using Spring Cloud Bus or `ContextRefresher`—but that’s outside this core tutorial.

## 9) Decrypt (for verification or debugging)

If you ever need to verify what’s inside a `{cipher}`, you can POST it to `/decrypt`:

```bash
curl -u configadmin:super-strong-password \
     -X POST http://localhost:8888/decrypt \
     -d '{cipher}AQB2LrK...'
```

It responds with the **plain text** (so treat this endpoint very carefully).

## 10) Hardening & best practices

* **Prefer disabling** `/encrypt` and `/decrypt` in production:

  ```yaml
  spring.cloud.config.server.encrypt.enabled=false
  ```

  Encrypt values in a locked-down staging admin instance, then commit the `{cipher}` values to Git.
* If you keep them enabled, **restrict access** (network + authz) and **log minimal info**.
* Keep the **keystore** out of public repos. Mount via container secret/volume or use a secret store (Vault, AWS KMS, Azure Key Vault, GCP KMS) with Spring Cloud Config’s native integrations if needed.
* Use **different strong passwords** for keystore and key alias.
* Rotate keys periodically and have a re-encryption process.

## 11) Quick smoke tests

**Health:**

```bash
curl http://localhost:8888/actuator/health
```

**Fetch configs (server view):**

```bash
curl http://localhost:8888/orderservice/dev
```

**Client logs:** should show it contacted the Config Server and loaded externalized properties.

## 12) Common troubleshooting

* **404 on `/encrypt`**: encryption disabled or endpoints not exposed; ensure `spring-cloud-config-server` is on the classpath and `encrypt.enabled` isn’t false.
* **“Cannot find key alias”**: alias mismatch; check `alias` name and keystore contents:

  ```bash
  keytool -list -keystore config-server.p12 -storetype PKCS12 -storepass changeit
  ```
* **Git auth failures**: verify PAT scopes (for HTTPS) or SSH private key format and permissions.
* **Client not loading configs**: ensure `spring.config.import=optional:configserver:...`, correct `spring.application.name`, and that the `{application}-{profile}.yml` exists in the Git repo.

---

That’s it! You now have a secure Config Server backed by GitHub, with keystore-powered encryption and transparent decryption for clients. If you want, I can add a minimal **Dockerfile + docker-compose** for the Config Server with the keystore mounted as a secret.
