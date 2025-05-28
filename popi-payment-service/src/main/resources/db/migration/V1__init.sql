CREATE TABLE payment (
                         payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         member_id BIGINT NOT NULL,
                         merchant_uid VARCHAR(255) NOT NULL,
                         imp_uid VARCHAR(255),
                         pg_provider VARCHAR(255),
                         amount INT NOT NULL,
                         status ENUM('READY', 'PAID', 'FAILED', 'CANCELLED') NOT NULL,
                         created_at DATETIME NOT NULL,
                         updated_at DATETIME
);
