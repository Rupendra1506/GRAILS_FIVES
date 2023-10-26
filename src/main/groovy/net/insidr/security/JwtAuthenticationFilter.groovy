package net.insidr.security

import grails.converters.JSON
import grails.core.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    def grailsApplication
    def jwtService

    JwtAuthenticationFilter() {
        super("/") // the defaultFilterProcessesUrl is set when the bean is initialized via setGrailsApplication
    }

    @Override
    Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String header = request.getHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) {
            throw new JwtAuthenticationException("No JWT token found in request headers")
        }
        def authToken = header.substring(7)
        def decodedToken = jwtService.decodeToken(authToken)
        if (!decodedToken) {
            throw new JwtAuthenticationException("Invalid token")
        }

        def roles = decodedToken.getClaim("roles").asList(String)
        def authorityList = AuthorityUtils.commaSeparatedStringToAuthorityList(roles.join(","))
        def adminForList = decodedToken.getClaim("adminFor").asList(String)
        def operationsForList = decodedToken.getClaim("operationsFor").asList(String)
        def expertForList = decodedToken.getClaim("expertFor").asList(String)

        def principal = new JwtAuthenticatedUserDetails(decodedToken.subject, authToken, authorityList, adminForList, operationsForList, expertForList)
        def authRequest = new JwtAuthenticationToken(principal, authToken)
        return authenticationManager.authenticate(authRequest)
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        try {
            SecurityContextHolder.getContext().setAuthentication(authResult)
            chain.doFilter(request, response)
        } catch (AccessDeniedException ex) {
            response.status = 403
            response.contentType = "application/json"
            response.outputStream << ([
                    error: [ message: "You're not allowed to access this resource" ]
            ] as JSON)
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        SecurityContextHolder.clearContext()
        response.status = 401
        response.contentType = "application/json"
        response.outputStream << ([
                error: [ message: "Authentication failed" ]
        ] as JSON)
    }

    @Override
    @Autowired
    void setAuthenticationManager(AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager)
    }

    @Autowired
    void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        def pattern = this.grailsApplication.config.grails.plugin.springsecurity.filterChain.chainMap.find { it.filters =~ /(?<!-)jwtAuthenticationFilter/ }?.pattern
        if (pattern) {
            setFilterProcessesUrl(pattern)
        }
    }

}
