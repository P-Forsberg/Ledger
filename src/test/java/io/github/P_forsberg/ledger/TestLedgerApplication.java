package io.github.P_forsberg.ledger;

import org.springframework.boot.SpringApplication;

public class TestLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.from(LedgerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
