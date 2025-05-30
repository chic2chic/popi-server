CREATE TABLE payment_item (
                         payment_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         payment_id BIGINT NOT NULL,
                         item_id BIGINT NOT NULL,
                         quantity INT NOT NULL,
                         created_at DATETIME NOT NULL,
                         updated_at DATETIME,
                         CONSTRAINT fk_payment_item_payment FOREIGN KEY (payment_id) REFERENCES payment(payment_id) ON DELETE CASCADE
)