CREATE TABLE sec_field_rule (
    id                    UUID         PRIMARY KEY,
    resource_type         VARCHAR(100) NOT NULL,
    field_name            VARCHAR(100) NOT NULL,
    required_permission   VARCHAR(100) NOT NULL,
    masking_when_denied   VARCHAR(30)  NOT NULL,
    CONSTRAINT uk_field_rule UNIQUE (resource_type, field_name),
    CONSTRAINT fk_field_rule_permission FOREIGN KEY (required_permission)
        REFERENCES sec_permission (permission_key) ON DELETE CASCADE
);

CREATE INDEX idx_field_rule_resource ON sec_field_rule (resource_type);
