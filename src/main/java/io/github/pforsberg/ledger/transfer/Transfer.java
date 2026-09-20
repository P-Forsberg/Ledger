package io.github.pforsberg.ledger.transfer;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "initiator_id", nullable = false, updatable = false)
    private UUID initiatorId;

    @Column(name = "source_account_id", nullable = false, updatable = false)
    private UUID sourceAccountId;

    @Column(name = "target_account_id", nullable = false, updatable = false)
    private UUID targetAccountId;

    @Column(nullable = false, updatable = false)
    private long amount;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(name = "reverses_transfer_id", updatable = false)
    private UUID reversesTransferId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    protected Transfer() {
        // Required by Hibernate. Not for application code.
    }

    public Transfer(String idempotencyKey, UUID initiatorId, UUID sourceAccountId,
                    UUID targetAccountId, long amount, String currency, TransferStatus status) {
        this.idempotencyKey = idempotencyKey;
        this.initiatorId = initiatorId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getInitiatorId() { return initiatorId; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransferStatus getStatus() { return status; }
    public UUID getReversesTransferId() { return reversesTransferId; }
    public Instant getCreatedAt() { return createdAt; }
}