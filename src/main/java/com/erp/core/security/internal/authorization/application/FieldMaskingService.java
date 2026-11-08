package com.erp.core.security.internal.authorization.application;

import com.erp.core.security.api.authorization.FieldAccessDecision;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class FieldMaskingService {

    public static final Object HIDDEN = new Object() {
        @Override public String toString() { return "<hidden>"; }
    };

    public Object mask(Object value, MaskingStrategy strategy) {
        if (value == null) return null;
        return switch (strategy) {
            case HIDDEN -> HIDDEN;
            case REDACT -> "***";
            case PARTIAL -> partialMask(String.valueOf(value));
            case HASH -> hashPrefix(String.valueOf(value));
        };
    }

    public Object maskForApi(Object value, FieldAccessDecision.MaskingStrategy strategy) {
        if (value == null || strategy == null || strategy == FieldAccessDecision.MaskingStrategy.FULL) {
            return value;
        }
        return switch (strategy) {
            case FULL -> value;
            case REDACT -> "***";
            case PARTIAL -> partialMask(String.valueOf(value));
            case HASH -> hashPrefix(String.valueOf(value));
        };
    }

    private String partialMask(String v) {
        int len = v.length();
        if (len <= 2) return "**";
        if (len <= 4) return v.charAt(0) + "**" + v.charAt(len - 1);
        int keep = Math.max(2, len / 4);
        return v.substring(0, keep)
                + "*".repeat(len - 2 * keep)
                + v.substring(len - keep);
    }

    private String hashPrefix(String v) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(v.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(h, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            return "sha256:unavailable";
        }
    }

    public enum MaskingStrategy { HIDDEN, REDACT, PARTIAL, HASH }
}