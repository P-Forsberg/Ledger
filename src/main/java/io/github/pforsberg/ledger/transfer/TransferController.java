package io.github.pforsberg.ledger.transfer;
import io.github.pforsberg.ledger.transfer.dto.CreateTransferRequest;
import io.github.pforsberg.ledger.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transactionService;

    @PostMapping
    public ResponseEntity<TransferResponse> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                   @Valid @RequestBody CreateTransferRequest request){
        TransferResponse response = transactionService.create(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}