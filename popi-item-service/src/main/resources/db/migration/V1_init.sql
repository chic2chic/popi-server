CREATE TABLE item_recommendation (
    item_recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    popup_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    item_price INT NOT NULL,
    item_image_url VARCHAR(1000) NOT NULL
);