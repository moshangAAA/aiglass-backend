package com.almousleck.enums;

public enum UserRole {
    USER,
    ADMIN,
    DEVICE_OWNER;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
