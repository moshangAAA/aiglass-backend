package com.almousleck.model;

import com.almousleck.common.BaseEntity;
import com.almousleck.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "devices")
public class Device extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Column(nullable = false)
    private String type; // ex: "AI-GLASS-V1"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    @Column(name = "location_lat")
    private Double locationLat; // Hardware Telemetry

    @Column(name = "location_lng")
    private Double locationLng;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "connect_time")
    private Instant connectTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
}
