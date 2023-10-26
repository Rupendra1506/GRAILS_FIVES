import net.insidr.security.CorsFilter
import net.insidr.security.JwtAuthenticationFilter
import net.insidr.security.JwtAuthenticationProvider
import org.springframework.boot.web.servlet.FilterRegistrationBean

// Place your Spring DSL code here
beans = {
    jwtAuthenticationProvider(JwtAuthenticationProvider) {
        jwtService = ref("jwtService")
    }

    jwtAuthenticationFilter(JwtAuthenticationFilter) {
        jwtService = ref("jwtService")
    }

    myFilterDeregistrationBean(FilterRegistrationBean) {
        filter = ref('jwtAuthenticationFilter')
        enabled = false
    }

    corsFilter(CorsFilter)
}
