package io.github.pforsberg.ledger.transfer.dto;

import io.github.pforsberg.ledger.transfer.Transfer;

import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID targetAccountId,
        long amount,
        String currency,
        String status,
        UUID reversesTransferId,
        Instant createdAt) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getTargetAccountId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus().name(),
                transfer.getReversesTransferId(),
                transfer.getCreatedAt());
    }
}