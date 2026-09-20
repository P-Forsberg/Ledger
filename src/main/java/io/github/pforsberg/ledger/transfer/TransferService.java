package io.github.pforsberg.ledger.transfer;

import io.github.pforsberg.ledger.transfer.dto.CreateTransferRequest;
import io.github.pforsberg.ledger.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferService {

    public TransferResponse create(String idempotencyKey, @Valid CreateTransferRequest request) {
        return null;
    }
}
