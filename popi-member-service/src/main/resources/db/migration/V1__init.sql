CREATE TABLE member (
                         member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         nickname VARCHAR(255) NOT NULL,
                         oauth_id VARCHAR(255) NOT NULL,
                         oauth_provider VARCHAR(255) NOT NULL,
                         age ENUM('TEENAGER', 'TWENTIES', 'THIRTIES', 'FORTIES_AND_ABOVE') NOT NULL,
                         gender ENUM('MALE', 'FEMALE') NOT NULL,
                         status ENUM('NORMAL', 'DELETED', 'FORBIDDEN') NOT NULL,
                         role ENUM('ADMIN', 'USER') NOT NULL,
                         created_at DATETIME NOT NULL,
                         updated_at DATETIME
);