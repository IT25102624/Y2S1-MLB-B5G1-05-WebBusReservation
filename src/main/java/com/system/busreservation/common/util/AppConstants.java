package com.system.busreservation.common.util;

/**
 * Application-wide constants and default configuration values.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // Pagination and Sorting Defaults
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "ASC";

    // Date and Time Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    // Security & JWT Defaults
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // API Base Paths
    public static final String API_BASE_PATH = "/api/v1";
}
