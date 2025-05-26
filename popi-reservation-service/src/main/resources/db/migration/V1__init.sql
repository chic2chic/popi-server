CREATE TABLE member_reservation
(
    member_reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id        BIGINT   NOT NULL,
    member_id             BIGINT   NOT NULL,
    popup_id              BIGINT NULL,
    qr_image              TEXT NULL,
    reservation_date      DATE,
    reservation_time      TIME,
    created_at            DATETIME NOT NULL,
    updated_at            DATETIME
);