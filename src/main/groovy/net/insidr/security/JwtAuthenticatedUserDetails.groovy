package net.insidr.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class JwtAuthenticatedUserDetails implements UserDetails {

    private String userId
    private String token
    private Collection<GrantedAuthority> authorities
    private Collection<String> adminFor
    private Collection<String> operationsFor
    private Collection<String> expertFor

    JwtAuthenticatedUserDetails(
            String userId,
            String token,
            Collection<GrantedAuthority> authorities,
            Collection<String> adminFor,
            Collection<String> operationsFor,
            Collection<String> expertFor) {
        this.userId = userId
        this.token = token
        this.authorities = authorities
        this.adminFor = adminFor ? adminFor : []
        this.operationsFor = operationsFor ? operationsFor : []
        this.expertFor = expertFor ? expertFor : []
    }

    @Override
    Collection<GrantedAuthority> getAuthorities() {
        return authorities
    }

    Collection<String> getAdminFor() {
        return adminFor
    }

    Collection<String> getOperationsFor() {
        return operationsFor
    }

    Collection<String> getExportFor() {
        return expertFor
    }

    @Override
    String getPassword() {
        return null
    }

    @Override
    String getUsername() {
        return userId
    }

    @Override
    boolean isAccountNonExpired() {
        return true
    }

    @Override
    boolean isAccountNonLocked() {
        return true
    }

    @Override
    boolean isCredentialsNonExpired() {
        return true
    }

    @Override
    boolean isEnabled() {
        return true
    }

    Long getId() {
        return userId as Long
    }

    String getToken() {
        return token
    }

    def hasPrivilegedAccessForCompany(companyTag) {
        def isSuperadmin = authorities.any {it.getAuthority() == 'ROLE_ADMIN'}
        return isSuperadmin ||  operationsFor.contains(companyTag)
    }

    def canManageCompany(companyTag) {
        hasPrivilegedAccessForCompany(companyTag) || adminFor.contains(companyTag)
    }

    def hasExpertAccessForCompany(companyTag) {
        canManageCompany(companyTag) || expertFor.contains(companyTag)
    }

}
