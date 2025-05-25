CREATE TABLE member_reservation
(
    member_reservation_id BIGINT AUTO_INCREMENT NOT NULL,
    reservation_id        BIGINT NOT NULL,
    member_id             BIGINT NOT NULL,
    popup_id              BIGINT NULL,
    image_byte            BLOB NULL,
    reservation_date      DATE,
    reservation_time      TIME
);