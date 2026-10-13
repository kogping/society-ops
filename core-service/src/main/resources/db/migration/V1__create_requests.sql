CREATE TABLE requests (
    id                    UUID          PRIMARY KEY,
    requester             VARCHAR(100)  NOT NULL,
    status                VARCHAR(20)   NOT NULL,

    claimant_first_name   VARCHAR(100)  NOT NULL,
    claimant_last_name    VARCHAR(100)  NOT NULL,
    claimant_email        VARCHAR(254)  NOT NULL,
    bank_bsb              VARCHAR(6)   NOT NULL,
    bank_account_number   VARCHAR(10)   NOT NULL,
    portfolio             VARCHAR(20)   NOT NULL,
    event_name            VARCHAR(200)  NOT NULL,
    event_date            DATE          NOT NULL,
    amount                NUMERIC(19,4) NOT NULL,
    currency              VARCHAR(3)   NOT NULL,
    declaration_signature VARCHAR(200)  NOT NULL,

    decided_by            VARCHAR(100),
    decided_at            TIMESTAMPTZ,
    rejection_reason      VARCHAR(500),
    paid_by               VARCHAR(100),
    paid_at               TIMESTAMPTZ,

    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL,
    version               BIGINT        NOT NULL,

    CONSTRAINT requests_status_valid CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'PAID')),
    CONSTRAINT requests_portfolio_valid CHECK (portfolio IN (
        'EXEC', 'HR', 'SOCIALS', 'IT', 'MARKETING', 'PUBLICATIONS', 'EDUCATION', 'SPONSORSHIPS', 'COMPETITIONS')),
    CONSTRAINT requests_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT requests_amount_positive CHECK (amount > 0),
    CONSTRAINT requests_bsb_format CHECK (bank_bsb ~ '^[0-9]{6}$'),
    CONSTRAINT requests_account_format CHECK (bank_account_number ~ '^[0-9]{6,10}$'),
    CONSTRAINT requests_decision_recorded CHECK (
        status = 'SUBMITTED' OR (decided_by IS NOT NULL AND decided_at IS NOT NULL)),
    CONSTRAINT requests_rejection_has_reason CHECK (
        status <> 'REJECTED' OR rejection_reason IS NOT NULL),
    CONSTRAINT requests_payment_recorded CHECK (
        status <> 'PAID' OR (paid_by IS NOT NULL AND paid_at IS NOT NULL))
);

CREATE INDEX requests_status_created_at_idx ON requests (status, created_at);

CREATE INDEX requests_requester_created_at_idx ON requests (requester, created_at);
