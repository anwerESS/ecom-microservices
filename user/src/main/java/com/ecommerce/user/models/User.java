package com.ecommerce.user.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "users") // 'collection' similar to table in SQL
public class User {
    @Id
    private String id; // must be a string because mongo does not support auto increment
    private String firstName;
    private String lastName;

    /**
     * The @Indexed annotation creates a MongoDB index on the specified field.
     * The 'unique' attribute ensures that no two documents can have the same email.
     */
    @Indexed(unique = true)
    private String email;
    private String phone;
    private UserRole role = UserRole.CUSTOMER;
    private Address address;

    @CreatedDate // similar to @CreationTimestamp in JPA
    private LocalDateTime createdAt;

    @LastModifiedDate // similar to @UpdateTimestamp in JPA
    private LocalDateTime updatedAt;
}
