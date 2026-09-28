package com.system.busreservation.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration of supported bus types.
 * Extended to support standard and emerging Sri Lankan and intercity transit vehicle classes.
 */
public enum BusType {
    AC,
    NON_AC,
    LUXURY,
    SLEEPER,
    SEMI_LUXURY,
    SUPER_LUXURY,
    DOUBLE_DECKER,
    MINI_BUS,
    ELECTRIC,
    EXPRESS,
    OTHER;

    @JsonCreator
    public static BusType fromString(String value) {
        if (value == null || value.isBlank()) {
            return NON_AC;
        }
        String clean = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (BusType type : BusType.values()) {
            if (type.name().equalsIgnoreCase(clean)) {
                return type;
            }
        }
        return OTHER;
    }
}
