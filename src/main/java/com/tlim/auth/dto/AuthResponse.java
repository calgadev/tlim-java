package com.tlim.auth.dto;

public record AuthResponse(String token, String username, String role) {}
