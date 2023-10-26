package net.insidr.security

import org.springframework.security.core.AuthenticationException

class JwtAuthenticationException extends AuthenticationException {

    JwtAuthenticationException(message) {
        super(message)
    }
}
