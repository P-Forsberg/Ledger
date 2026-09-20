package io.github.pforsberg.ledger.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateTransferRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID targetAccountId,
        @Positive long amount,
        @NotBlank String currency){}
