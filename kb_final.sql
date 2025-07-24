CREATE DATABASE kb_final_db ;

use kb_final_db;

CREATE TABLE investment_types (
                                  risk_type VARCHAR(10) PRIMARY KEY,        -- 예: CSD, AGR
                                  name_kr VARCHAR(100) NOT NULL,            -- 예: 신중한 안정형
                                  group_code ENUM('CONSERVATIVE', 'BALANCED', 'AGGRESSIVE', 'ANALYTICAL', 'EMOTIONAL') NOT NULL,
                                  description TEXT
);

CREATE TABLE user (
                      id INT PRIMARY KEY AUTO_INCREMENT,
                      username VARCHAR(50) NOT NULL UNIQUE,
                      password VARCHAR(200) NOT NULL,
                      name VARCHAR(100) NOT NULL,
                      nickname VARCHAR(50) NOT NULL UNIQUE,
                      profile_image VARCHAR(255),
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      risk_type VARCHAR(10) NOT NULL,
                      phone_number VARCHAR(100) NOT NULL,
                      email VARCHAR(100) NOT NULL,
                      provider VARCHAR(50) NOT NULL,
                      total_credit BIGINT DEFAULT 0,
                      FOREIGN KEY (risk_type) REFERENCES investment_types(risk_type)
);


CREATE TABLE user_setting (
                              user_id INT PRIMARY KEY,
                              push_notify BOOLEAN NOT NULL,
                              is_active BOOLEAN NOT NULL,
                              updated_at DATE NOT NULL,
                              FOREIGN KEY (user_id) REFERENCES user(id)
);
CREATE TABLE learning_contents (
                                   content_id INT PRIMARY KEY AUTO_INCREMENT,
                                   type ENUM('ARTICLE', 'YOUTUBE') NOT NULL,
                                   title VARCHAR(100) NOT NULL,
                                   body TEXT,
                                   image_url TEXT,
                                   youtube_url TEXT,
                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE learning_quiz (
                               quiz_id INT PRIMARY KEY,
                               question TEXT NOT NULL,
                               answer ENUM('O','X') NOT NULL,
                               comment TEXT NOT NULL,
                               credit_reward INT NOT NULL,
                               FOREIGN KEY (quiz_id) REFERENCES learning_contents(content_id)
);

CREATE TABLE user_quiz_results (
                                   user_id INT NOT NULL,
                                   quiz_id INT NOT NULL,
                                   is_correct BOOLEAN NOT NULL,
                                   selected_answer ENUM('O','X') NOT NULL,
                                   credit_earned INT,
                                   attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (user_id, quiz_id),
                                   FOREIGN KEY (user_id) REFERENCES user(id),
                                   FOREIGN KEY (quiz_id) REFERENCES learning_quiz(quiz_id)
);


CREATE TABLE learning_contents_tags (
                                        group_code ENUM('CONSERVATIVE', 'BALANCED', 'AGGRESSIVE', 'ANALYTICAL', 'EMOTIONAL') NOT NULL,
                                        content_id INT NOT NULL,
                                        PRIMARY KEY (group_code, content_id),
                                        FOREIGN KEY (content_id) REFERENCES learning_contents(content_id)
);

CREATE TABLE investment_journal (
                                    id INT PRIMARY KEY AUTO_INCREMENT,
                                    journal_date DATE NOT NULL,
                                    emotion VARCHAR(50),
                                    reason TEXT,
                                    mistake TEXT,
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    user_id INT NOT NULL,
                                    FOREIGN KEY (user_id) REFERENCES user(id)
);
CREATE TABLE journal_feedback (
                                  id INT PRIMARY KEY AUTO_INCREMENT,
                                  week_start DATE NOT NULL,
                                  week_end DATE NOT NULL,
                                  feedback TEXT,
                                  generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  user_id INT NOT NULL,
                                  FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE journal_feedback_mapping (
                                          journal_id INT NOT NULL,
                                          feedback_id INT NOT NULL,
                                          PRIMARY KEY (journal_id, feedback_id),
                                          FOREIGN KEY (journal_id) REFERENCES investment_journal(id),
                                          FOREIGN KEY (feedback_id) REFERENCES journal_feedback(id)
);

CREATE TABLE `user_accounts` (
                                 `account_id`	INT	AUTO_INCREMENT PRIMARY KEY,
                                 `user_id`	INT	NOT NULL,
                                 `account_number`	VARCHAR(20)	NOT NULL,
                                 `current_balance`	BIGINT	NOT NULL DEFAULT 10000000,
                                 `total_asset_value`	BIGINT,
                                 `total_profit_loss`	BIGINT,
                                 `profit_rate`	DECIMAL(6, 2),
                                 `reset_count`	INT DEFAULT 0,
                                 `created_at`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE `transactions` (
                                `transaction_id`	INT	AUTO_INCREMENT PRIMARY KEY,
                                `account_id`	INT	NOT NULL,
                                `stock_code`	VARCHAR(10)	NOT NULL,
                                `stock_name`	VARCHAR(20)	NOT NULL,
                                `transaction_type`	ENUM('BUY', 'SELL')	NOT NULL,
                                `order_type`	ENUM('MARKET', 'LIMIT')	NOT NULL,
                                `quantity`	INT	NOT NULL,
                                `price`	INT	NOT NULL,
                                `order_price`	INT,
                                `total_amount`	BIGINT	NOT NULL,
                                `executed_at`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                `order_created_at`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (`account_id`) REFERENCES `user_accounts`(`account_id`) ON DELETE CASCADE
);
CREATE TABLE investment_journal_trade (
                                          journal_id INT NOT NULL,
                                          transaction_id INT NOT NULL,
                                          PRIMARY KEY (journal_id, transaction_id),
                                          FOREIGN KEY (journal_id) REFERENCES investment_journal(id),
                                          FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);

INSERT INTO user (
    username, password, name, nickname, profile_image,
    created_at, updated_at, risk_type, phone_number,
    email, provider, total_credit
) VALUES (
             'testuser1', 'encrypted_password', '홍길동', '길동이', NULL,
             NOW(), NOW(), 'AGR', '010-1234-5678',
             'testuser1@example.com', 'local', 100000
         );
select * from journal_feedback;
delete from journal_feedback;
select * from journal_feedback_mapping;
SELECT week_start FROM journal_feedback WHERE user_id = 1;
