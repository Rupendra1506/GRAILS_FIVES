package net.insidr.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException

import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

class JwtService {

    def grailsApplication

    def verifyToken(token) {
        try {
            def algorithm = Algorithm.RSA256(readPublicKey(), null)
            def verifier = JWT.require(algorithm)
                .withIssuer("directly")
                .build()
            verifier.verify(token)
            return true
        } catch (JWTVerificationException ex) {
            if (ex instanceof TokenExpiredException) {
                log.info("Token expired", ex)
            } else {
                log.error("Token verification failed", ex)
            }
            return false
        }
    }

    def decodeToken(token) {
        try {
            return JWT.decode(token)
        } catch (JWTDecodeException ex) {
            log.error("Token could not be decoded", ex)
            return null
        }
    }

    def readPublicKey() {
        def keyBytes
        def publicKey = grailsApplication.config.jwt.publicKey
        if (publicKey) {
            keyBytes = Base64.decoder.decode(publicKey)
        } else {
            def publicKeyPath = grailsApplication.config.jwt.publicKeyPath
            def publicKeyFile = new File(publicKeyPath)
            keyBytes = publicKeyFile.isAbsolute() ? publicKeyFile.bytes : this.class.getClassLoader().getResourceAsStream(publicKeyPath).bytes
        }
        def spec = new X509EncodedKeySpec(keyBytes)
        def keyFactory = KeyFactory.getInstance("RSA")

        return keyFactory.generatePublic(spec)
    }
}
