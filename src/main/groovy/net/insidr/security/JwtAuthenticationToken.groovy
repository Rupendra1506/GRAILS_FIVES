package net.insidr.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {

    JwtAuthenticationToken(JwtAuthenticatedUserDetails principal, String token) {
        super(principal, token)
    }

    String getToken() {
        return credentials
    }

}
