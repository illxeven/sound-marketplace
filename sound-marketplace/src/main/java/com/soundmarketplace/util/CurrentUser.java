package com.soundmarketplace.util;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {}

    public static Integer getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated");
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return Integer.valueOf(jwt.getSubject());
        }
        try {
            return Integer.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Invalid authentication principal: " + auth.getName());
        }
    }

    public static Integer getUserIdOrNull() {
        try {
            return getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAdminOrSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    public static void requireOwnerOrAdmin(Integer resourceOwnerId) {
        if (isAdminOrSuperAdmin()) {
            return;
        }
        Integer currentUserId = getUserId();
        if (resourceOwnerId == null || !resourceOwnerId.equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to modify this resource");
        }
    }
}
