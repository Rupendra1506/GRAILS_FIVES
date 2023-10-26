package net.insidr.security

import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails

class JwtAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    def jwtService

    @Override
    boolean supports(Class<?> authentication) {
        return (JwtAuthenticationToken.class.isAssignableFrom(authentication))
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) {
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication
        String token = jwtAuthenticationToken.token
        if (!jwtService.verifyToken(token)) {
            throw new JwtAuthenticationException("Invalid token")
        }
        return jwtAuthenticationToken.principal
    }

}
