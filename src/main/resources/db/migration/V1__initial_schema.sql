-- V1__initial_schema.sql
-- Core ledger schema. Append-only by design: transfers and entries are never
-- updated or deleted once written. Corrections are made by reversal.

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
create table users (
    id            uuid        primary key default gen_random_uuid(),
    email         text        not null,
    password_hash text        not null,
    created_at    timestamptz not null default now(),

    constraint users_email_unique unique (email)
);

-- ---------------------------------------------------------------------------
-- accounts
-- One owned balance in a single currency. There is deliberately no balance
-- column: a balance is derived from entries, so it cannot drift from history.
-- 'version' supports JPA optimistic locking (@Version).
-- ---------------------------------------------------------------------------
create table accounts (
    id         uuid        primary key default gen_random_uuid(),
    user_id    uuid        not null,
    currency   char(3)     not null,
    version    bigint      not null default 0,
    created_at timestamptz not null default now(),

    constraint accounts_user_fk      foreign key (user_id) references users (id),
    constraint accounts_currency_iso check (currency ~ '^[A-Z]{3}$')
);

create index accounts_user_idx on accounts (user_id);

-- ---------------------------------------------------------------------------
-- transfers
-- The intent to move money. Amount is always positive and always in the
-- currency's minor unit (öre, cents) as an integer -- never floating point.
-- ---------------------------------------------------------------------------
create table transfers (
    id                   uuid        primary key default gen_random_uuid(),
    idempotency_key      text        not null,
    initiator_id         uuid        not null,
    source_account_id    uuid        not null,
    target_account_id    uuid        not null,
    amount               bigint      not null,
    currency             char(3)     not null,
    status               text        not null,
    reverses_transfer_id uuid,
    created_at           timestamptz not null default now(),

    constraint transfers_initiator_fk foreign key (initiator_id)         references users (id),
    constraint transfers_source_fk    foreign key (source_account_id)    references accounts (id),
    constraint transfers_target_fk    foreign key (target_account_id)    references accounts (id),
    constraint transfers_reverses_fk  foreign key (reverses_transfer_id) references transfers (id),

    -- A transfer of zero or a negative amount is not a transfer. Direction is
    -- expressed by which account is source and which is target, not by sign.
    constraint transfers_amount_positive check (amount > 0),

    -- Moving money to the account it came from is always a mistake.
    constraint transfers_accounts_differ check (source_account_id <> target_account_id),

    constraint transfers_status_valid check (status in ('PENDING', 'COMPLETED', 'FAILED')),

    constraint transfers_currency_iso check (currency ~ '^[A-Z]{3}$'),

    -- Idempotency: the same key replayed by the same user must resolve to the
    -- same transfer rather than moving money twice. Scoped per user so that
    -- two clients cannot collide on each other's keys.
    constraint transfers_idempotency_unique unique (initiator_id, idempotency_key)
);

-- A transfer may be reversed at most once. A partial unique index is used
-- instead of a plain unique constraint so that the many NULLs (ordinary,
-- non-reversing transfers) do not conflict with one another.
create unique index transfers_reverses_once_idx
    on transfers (reverses_transfer_id)
    where reverses_transfer_id is not null;

create index transfers_source_idx    on transfers (source_account_id, created_at desc);
create index transfers_target_idx    on transfers (target_account_id, created_at desc);
create index transfers_initiator_idx on transfers (initiator_id, created_at desc);

-- ---------------------------------------------------------------------------
-- entries
-- The actual movement. Every transfer writes exactly two rows here, in the
-- same database transaction: one negative on the source, one positive on the
-- target. The sum of all entries in the table is therefore always zero, which
-- is the single strongest invariant in the system and worth an explicit test.
-- ---------------------------------------------------------------------------
create table entries (
    id          uuid        primary key default gen_random_uuid(),
    transfer_id uuid        not null,
    account_id  uuid        not null,
    amount      bigint      not null,
    created_at  timestamptz not null default now(),

    constraint entries_transfer_fk foreign key (transfer_id) references transfers (id),
    constraint entries_account_fk  foreign key (account_id)  references accounts (id),

    -- Sign carries meaning here: negative leaves the account, positive arrives.
    -- Zero would be a row that records nothing.
    constraint entries_amount_nonzero check (amount <> 0),

    -- One entry per account per transfer. Prevents a bug from writing a
    -- transfer's debit twice and silently draining an account.
    constraint entries_one_per_account_per_transfer unique (transfer_id, account_id)
);

-- Balance lookups and statement listings both read by account, most recent
-- first, so the index carries created_at rather than leaving it to a sort.
create index entries_account_idx on entries (account_id, created_at desc);
