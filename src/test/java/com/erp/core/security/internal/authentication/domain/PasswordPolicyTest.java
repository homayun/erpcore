package com.erp.core.security.internal.authentication.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = PasswordPolicy.defaults();

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> policy.validate("Str0ng!Password"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTooShort() {
        assertThatThrownBy(() -> policy.validate("Sh0rt!"))
                .isInstanceOf(PasswordPolicyViolation.class)
                .hasMessageContaining("length");
    }

    @Test
    void rejectsMissingUppercase() {
        assertThatThrownBy(() -> policy.validate("weakpassword1!"))
                .isInstanceOf(PasswordPolicyViolation.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    void rejectsMissingLowercase() {
        assertThatThrownBy(() -> policy.validate("WEAKPASSWORD1!"))
                .isInstanceOf(PasswordPolicyViolation.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    void rejectsMissingDigit() {
        assertThatThrownBy(() -> policy.validate("WeakPassword!"))
                .isInstanceOf(PasswordPolicyViolation.class)
                .hasMessageContaining("digit");
    }

    @Test
    void rejectsMissingSpecial() {
        assertThatThrownBy(() -> policy.validate("WeakPassword1"))
                .isInstanceOf(PasswordPolicyViolation.class)
                .hasMessageContaining("special");
    }
}