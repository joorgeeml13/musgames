package dev.ordago.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import dev.ordago.auth.domain.model.AccountIdentity;
import dev.ordago.auth.domain.model.OAuthProvider;

@Entity
@Table(
        name = "account_identities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_account_identities_provider_subject", columnNames = {"provider", "provider_subject"})
        },
        indexes = {
                @Index(name = "idx_account_identities_account_id", columnList = "account_id"),
                @Index(name = "idx_account_identities_provider_subject", columnList = "provider, provider_subject")
        }
)
public class AccountIdentityEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public UUID accountId;

    @Column(name = "provider", nullable = false, length = 32)
    public String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    public String providerSubject;

    @Column(name = "email", length = 255)
    public String email;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    public AccountIdentity toDomain() {
        return new AccountIdentity(
                this.id,
                this.accountId,
                OAuthProvider.fromString(this.provider),
                this.providerSubject,
                this.email,
                this.createdAt,
                this.updatedAt
        );
    }

    public static AccountIdentityEntity fromDomain(AccountIdentity domain) {
        AccountIdentityEntity entity = new AccountIdentityEntity();
        entity.id = domain.id();
        entity.accountId = domain.accountId();
        entity.provider = domain.provider().name();
        entity.providerSubject = domain.providerSubject();
        entity.email = domain.email();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        return entity;
    }
}
