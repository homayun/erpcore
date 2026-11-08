package com.erp.core.security.internal.authentication.adapter.out.jdbc;

import com.erp.core.security.internal.authentication.port.CredentialStatusPort;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcCredentialStatusAdapter implements CredentialStatusPort {

    private final JdbcTemplate jdbc;

    public JdbcCredentialStatusAdapter(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<CredentialStatus> find(UUID actorId, UUID tenantId) {
        List<CredentialStatus> rows = jdbc.query(
                "SELECT username, enabled FROM sec_credentials WHERE actor_id = ? AND tenant_id = ?",
                (rs, i) -> new CredentialStatus(rs.getString("username"), rs.getBoolean("enabled")),
                actorId, tenantId);
        return rows.stream().findFirst();
    }
}