package com.erp.core.security.internal.jwt.config;

import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

/**
 * Fails application startup when the JWT signing secret is the publicly known
 * placeholder (anyone could forge tokens for any tenant).
 *
 * Dev/test escape hatch (NEVER in production):
 *   erp.security.jwt.allow-insecure-default-secret=true
 */
public class JwtSecretGuard implements InitializingBean {

    public static final String ALLOW_PROPERTY = "erp.security.jwt.allow-insecure-default-secret";

    private static final Logger log = LoggerFactory.getLogger(JwtSecretGuard.class);

    private final SecurityProperties props;
    private final Environment env;

    public JwtSecretGuard(SecurityProperties props, Environment env) {
        this.props = props;
        this.env = env;
    }

    @Override
    public void afterPropertiesSet() {
        String secret = props.jwt().secret();
        boolean placeholder = secret == null
                || secret.isBlank()
                || secret.toLowerCase().startsWith("change-me");
        if (!placeholder) {
            return;
        }
        if (env.getProperty(ALLOW_PROPERTY, Boolean.class, false)) {
            log.warn("SECURITY: running with the INSECURE default JWT secret ({} = true). "
                    + "Never do this outside local dev/test.", ALLOW_PROPERTY);
            return;
        }
        throw new IllegalStateException(
                "erp.security.jwt.secret is not set (or is the default placeholder). "
                        + "Set JWT_SECRET to a random value of at least 32 bytes.");
    }
}