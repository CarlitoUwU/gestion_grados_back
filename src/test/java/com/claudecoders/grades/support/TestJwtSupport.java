package com.claudecoders.grades.support;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

public final class TestJwtSupport {
    public static final String ISSUER = "https://accounts.google.com";
    public static final String AUDIENCE = "grades-test";
    private static final String KEY_ID = "test-key";
    private static final KeyPair KEY_PAIR = generateKeyPair();
    private static final JwtEncoder ENCODER = encoder(KEY_PAIR);
    private static final JwtEncoder OTHER_ENCODER = encoder(generateKeyPair());

    private TestJwtSupport() {}

    public static JwtDecoder decoder() {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY_PAIR.getPublic()).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(ISSUER);
        OAuth2TokenValidator<Jwt> audience =
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD, values -> values != null && values.contains(AUDIENCE));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }

    public static String token(String subject, Instant expiresAt) {
        return token(subject, ISSUER, List.of(AUDIENCE), expiresAt);
    }

    public static String token(
            String subject, String issuer, List<String> audience, Instant expiresAt) {
        return token(ENCODER, subject, issuer, audience, expiresAt);
    }

    public static String invalidSignatureToken(String subject) {
        return token(
                OTHER_ENCODER, subject, ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(3600));
    }

    private static String token(
            JwtEncoder encoder,
            String subject,
            String issuer,
            List<String> audience,
            Instant expiresAt) {
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(subject)
                        .audience(audience)
                        .issuedAt(expiresAt.minusSeconds(3600))
                        .expiresAt(expiresAt)
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY_ID).build(),
                                claims))
                .getTokenValue();
    }

    private static JwtEncoder encoder(KeyPair keyPair) {
        RSAKey jwk =
                new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                        .privateKey((RSAPrivateKey) keyPair.getPrivate())
                        .keyID(KEY_ID)
                        .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la clave JWT de prueba", exception);
        }
    }
}
