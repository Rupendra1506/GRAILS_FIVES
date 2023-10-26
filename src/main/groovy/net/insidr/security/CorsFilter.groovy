package net.insidr.security

import grails.core.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CorsFilter extends OncePerRequestFilter {

    def grailsApplication

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        def path = request.requestURI
        def origin = request.getHeader("Origin")
        if (originAndPathAreAllowed(origin, path)) {
            response.addHeader("Access-Control-Allow-Methods", "GET, PUT, POST, OPTIONS")
            // Yes, insidr does send the numbers below as headers
            response.addHeader("Access-Control-Allow-Headers", "0, 1, 10, 11, 12, 2, 3, 4, 5, 6, 7 ,8, 9, authorization, content-type")
            response.addHeader("Access-Control-Max-Age", "1800")
            response.addHeader("Access-Control-Allow-Origin", origin)
            response.addHeader("Access-Control-Allow-Credentials", "true")
        }
        if (HttpMethod.OPTIONS.matches(request.method)) {
            response.status = HttpStatus.OK.value()
        } else {
            chain.doFilter(request, response)
        }
    }

    private originAndPathAreAllowed(origin, path) {
        boolean originAllowed = origin?.matches(this.grailsApplication.config.cors.allowedOriginRegex) ?: true
        boolean pathAllowed = path?.matches(this.grailsApplication.config.cors.allowedPathRegex) ?: true
        return originAllowed && pathAllowed
    }

    @Autowired
    void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

}
