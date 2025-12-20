package com.almousleck.model;

import com.almousleck.common.BaseEntity;
import com.almousleck.enums.UserRole;
import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_phone", columnList = "phone_number")
        }
)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "phone_number", nullable = false, unique = true, length = 255)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    private UserRole role;

    @Column(nullable = false)
    private Integer usageCount = 0; // Number of navigations

    @Column(nullable = false)
    private Integer totalDistance = 0; // total distance from zero

    @Column(columnDefinition = "JSON")
    private String preference; // JSON to store preferences Ex: "voice": "female" "lg": "ch"

    @Column(nullable = false, length = 255)
    private String passwordHash;
}


