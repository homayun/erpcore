package com.erp.core.security.internal.authorization;

import com.erp.core.security.api.actor.ActorType;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.authorization.FieldAccessDecision;
import com.erp.core.security.api.authorization.FieldAccessRequest;
import com.erp.core.security.api.authorization.ResourceDescriptor;
import com.erp.core.security.internal.authorization.application.FieldAuthorizationService;
import com.erp.core.security.internal.authorization.domain.FieldRule;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.FieldRuleRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FieldAuthorizationServiceTest {

    private final UUID tenant = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private CurrentActor actor() {
        return new CurrentActor(actorId, ActorType.USER, tenant, "alice", Set.of());
    }

    private FieldAccessRequest readRequest(Set<String> fields) {
        return new FieldAccessRequest(
                actor(),
                new ResourceDescriptor("FINANCE_INVOICE", "ref-1", tenant),
                fields,
                FieldAccessRequest.FieldAccessMode.READ);
    }

    private FieldAccessRequest writeRequest(Set<String> fields) {
        return new FieldAccessRequest(
                actor(),
                new ResourceDescriptor("FINANCE_INVOICE", "ref-1", tenant),
                fields,
                FieldAccessRequest.FieldAccessMode.WRITE);
    }

    private FieldRule rule(String field, String permission, FieldRule.MaskingWhenDenied masking) {
        return new FieldRule("FINANCE_INVOICE", field, PermissionKey.of(permission), masking);
    }

    private FieldAuthorizationService service(FieldRule... rules) {
        List<FieldRule> fixedRules = List.of(rules);
        FieldRuleRepositoryPort repo = new FieldRuleRepositoryPort() {
            @Override public List<FieldRule> findByResourceType(String resourceType) { return fixedRules; }
            @Override public List<FieldRule> findAll() { return fixedRules; }
            @Override public FieldRule upsert(FieldRule r) { return r; }
            @Override public boolean delete(String resourceType, String fieldName) { return false; }
            @Override public boolean exists(String resourceType, String fieldName) { return false; }
        };
        return new FieldAuthorizationService(repo, (a, t) -> Set.of());
    }

    // --- Test 1: no rule = full read access ------------------------------

    @Test
    void readWithoutRuleGrantsFullAccess() {
        var svc = service();
        var decision = svc.decide(readRequest(Set.of("invoiceNo")), Set.of());

        assertThat(decision.grantedFields()).contains("invoiceNo");
        assertThat(decision.maskings()).doesNotContainKey("invoiceNo");
    }

    // --- Test 2: rule + permission = full access -------------------------

    @Test
    void readWithRuleAndPermissionGrantsFull() {
        var svc = service(rule("amount", "VIEW_AMOUNT", FieldRule.MaskingWhenDenied.PARTIAL));
        var decision = svc.decide(
                readRequest(Set.of("amount")),
                Set.of(PermissionKey.of("VIEW_AMOUNT")));

        assertThat(decision.grantedFields()).contains("amount");
        assertThat(decision.maskings().get("amount"))
                .isEqualTo(FieldAccessDecision.MaskingStrategy.FULL);
    }

    // --- Test 3: rule + NO permission = masked (BOLA read prevention) ----

    @Test
    void readWithRuleAndNoPermissionMasks() {
        var svc = service(rule("amount", "VIEW_AMOUNT", FieldRule.MaskingWhenDenied.PARTIAL));
        var decision = svc.decide(readRequest(Set.of("amount")), Set.of());

        assertThat(decision.grantedFields()).contains("amount");
        assertThat(decision.maskings().get("amount"))
                .isEqualTo(FieldAccessDecision.MaskingStrategy.PARTIAL);
    }

    // --- Test 4: write without _UPDATE = denied (mass assignment) --------

    @Test
    void writeWithoutUpdatePermissionDenied() {
        var svc = service();
        var decision = svc.decide(writeRequest(Set.of("invoiceNo")), Set.of());

        assertThat(decision.grantedFields()).doesNotContain("invoiceNo");
    }

    // --- Test 5: write with _UPDATE = granted ----------------------------

    @Test
    void writeWithUpdatePermissionGrants() {
        var svc = service();
        var decision = svc.decide(
                writeRequest(Set.of("invoiceNo")),
                Set.of(PermissionKey.of("FINANCE_INVOICE_UPDATE")));

        assertThat(decision.grantedFields()).contains("invoiceNo");
    }

    // --- Test 6: mixed fields get per-field masking ----------------------

    @Test
    void mixedFieldsApplyPerFieldMasking() {
        var svc = service(
                rule("amount",     "VIEW_AMOUNT", FieldRule.MaskingWhenDenied.PARTIAL),
                rule("salaryCost", "VIEW_SALARY", FieldRule.MaskingWhenDenied.HASH));

        var decision = svc.decide(
                readRequest(Set.of("invoiceNo", "amount", "salaryCost")),
                Set.of(PermissionKey.of("VIEW_AMOUNT")));

        assertThat(decision.grantedFields()).contains("invoiceNo");
        assertThat(decision.maskings()).doesNotContainKey("invoiceNo");

        assertThat(decision.maskings().get("amount"))
                .isEqualTo(FieldAccessDecision.MaskingStrategy.FULL);

        assertThat(decision.maskings().get("salaryCost"))
                .isEqualTo(FieldAccessDecision.MaskingStrategy.HASH);
    }

    // --- Test 7: null request yields deny-all ----------------------------

    @Test
    void nullRequestYieldsDenyAll() {
        var svc = service();
        var decision = svc.decide(null, Set.of());

        assertThat(decision.grantedFields()).isEmpty();
        assertThat(decision.maskings()).isEmpty();
    }

    // --- Test 8: HIDDEN masking rule maps to REDACT in API ---------------

    @Test
    void hiddenMaskingMapsToRedact() {
        var svc = service(rule("ssn", "VIEW_SSN", FieldRule.MaskingWhenDenied.HIDDEN));
        var decision = svc.decide(readRequest(Set.of("ssn")), Set.of());

        assertThat(decision.grantedFields()).contains("ssn");
        assertThat(decision.maskings().get("ssn"))
                .isEqualTo(FieldAccessDecision.MaskingStrategy.REDACT);
    }
}