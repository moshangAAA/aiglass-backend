package com.almousleck.util;

import jakarta.servlet.http.HttpServletRequest;

//Utility class for HTTP request operations
public class HttpRequestUtil {

    // Extracts client IP address from request, handling proxy headers
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        // Check X-Forwarded-For header (for proxies/load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }
}

