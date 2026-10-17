-- Sound Marketplace: Schema Updates for Merchandise Reviews, Stock Tracking & Email Verification

-- 1. Create merchandise_reviews table
CREATE TABLE IF NOT EXISTS merchandise_reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    merchandise_id INT NOT NULL,
    user_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_merch_reviews_merchandise FOREIGN KEY (merchandise_id) REFERENCES merchandise(id) ON DELETE CASCADE,
    CONSTRAINT fk_merch_reviews_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE
);

-- 2. Add stock_quantity column to merchandise table if not exists
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'sound_marketplace_db' 
  AND TABLE_NAME = 'merchandise' 
  AND COLUMN_NAME = 'stock_quantity';

SET @sql = IF(@col_exists = 0, 'ALTER TABLE merchandise ADD COLUMN stock_quantity INT NOT NULL DEFAULT 10', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure existing merchandise has positive initial stock
UPDATE merchandise SET stock_quantity = 20 WHERE stock_quantity = 0 OR stock_quantity IS NULL;

-- 3. Ensure user_account email verification fields
UPDATE user_account SET is_email_verified = 0 WHERE is_email_verified IS NULL;
