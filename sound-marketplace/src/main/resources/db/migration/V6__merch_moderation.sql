-- V6__merch_moderation.sql
-- Merch Listing Guardian: moderation tables. MySQL 8.0.
-- Completely idempotent (safe to run multiple times without duplicate errors).

-- ---------------------------------------------------------------------------
-- 1. Changes to existing tables (using dynamic checks to prevent duplicate errors)
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS patch_merch_moderation;
DELIMITER //
CREATE PROCEDURE patch_merch_moderation()
BEGIN
    -- merchandise columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchandise' AND COLUMN_NAME = 'moderation_status') THEN
        ALTER TABLE merchandise ADD COLUMN moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, NEEDS_REVIEW, REJECTED, BLOCKED';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchandise' AND COLUMN_NAME = 'moderation_version') THEN
        ALTER TABLE merchandise ADD COLUMN moderation_version INT NOT NULL DEFAULT 0 COMMENT 'Incremented on every content edit; each version is moderated separately';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchandise' AND COLUMN_NAME = 'moderated_at') THEN
        ALTER TABLE merchandise ADD COLUMN moderated_at TIMESTAMP NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchandise' AND COLUMN_NAME = 'explicit') THEN
        ALTER TABLE merchandise ADD COLUMN explicit TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Approved with an age gate (e.g. artistic nudity)';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchandise' AND COLUMN_NAME = 'is_digital') THEN
        ALTER TABLE merchandise ADD COLUMN is_digital TINYINT(1) NOT NULL DEFAULT 0;
    END IF;

    -- merchandise constraints & indexes
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchandise' AND CONSTRAINT_NAME = 'chk_merch_moderation_status') THEN
        ALTER TABLE merchandise ADD CONSTRAINT chk_merch_moderation_status CHECK (moderation_status IN ('PENDING','APPROVED','NEEDS_REVIEW','REJECTED','BLOCKED'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchandise' AND INDEX_NAME = 'idx_merch_status_created') THEN
        ALTER TABLE merchandise ADD INDEX idx_merch_status_created (moderation_status, created_at);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchandise' AND INDEX_NAME = 'idx_merch_seller_status') THEN
        ALTER TABLE merchandise ADD INDEX idx_merch_seller_status (seller_id, moderation_status);
    END IF;

    -- user_account columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'suspended_at') THEN
        ALTER TABLE user_account ADD COLUMN suspended_at TIMESTAMP NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'suspended_reason') THEN
        ALTER TABLE user_account ADD COLUMN suspended_reason VARCHAR(255) NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND COLUMN_NAME = 'suspended_by') THEN
        ALTER TABLE user_account ADD COLUMN suspended_by INT NULL;
    END IF;

    -- user_account foreign key
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account' AND CONSTRAINT_NAME = 'fk_user_suspended_by') THEN
        ALTER TABLE user_account ADD CONSTRAINT fk_user_suspended_by FOREIGN KEY (suspended_by) REFERENCES user_account (id);
    END IF;
END //
DELIMITER ;

CALL patch_merch_moderation();
DROP PROCEDURE patch_merch_moderation;

-- Grandfather existing rows as APPROVED
UPDATE merchandise SET moderation_status = 'APPROVED', moderated_at = NOW() WHERE moderation_status = 'PENDING';

-- ---------------------------------------------------------------------------
-- 2. Reason codes (seller-facing text lives here so the app and admin UI share it)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moderation_reason_codes (
    code VARCHAR(40) NOT NULL,
    kind VARCHAR(12) NOT NULL COMMENT 'PROHIBITED or RESTRICTED',
    title VARCHAR(120) NOT NULL,
    seller_message VARCHAR(500) NOT NULL,
    fix_hint VARCHAR(500) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (code),
    CONSTRAINT chk_reason_kind CHECK (kind IN ('PROHIBITED','RESTRICTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO moderation_reason_codes (code, kind, title, seller_message, fix_hint) VALUES
    ('P-WEAPONS','PROHIBITED','Weapons','Weapons, weapon parts and ammunition can''t be sold here.','Merch that shows a weapon in the artwork is fine; selling the item is not.'),
    ('P-DRUGS','PROHIBITED','Drugs','Drugs and cannabis or THC products can''t be sold here.',NULL),
    ('P-PARAPHERNALIA','PROHIBITED','Drug paraphernalia','Pipes, bongs and similar items can''t be sold here.',NULL),
    ('P-ALCOHOL-TOBACCO','PROHIBITED','Alcohol, tobacco or nicotine','Alcohol, tobacco, vapes and nicotine products can''t be sold here.','Branded empty glassware is fine.'),
    ('P-CSAM','PROHIBITED','Child safety','This listing was blocked.',NULL),
    ('P-ADULT','PROHIBITED','Sexually explicit','Sexually explicit products and imagery can''t be sold here.',NULL),
    ('P-COUNTERFEIT','PROHIBITED','Counterfeit','Copies of other brands'' or artists'' products can''t be sold here.','Sell only your own designs, or upload proof of a license.'),
    ('P-PIRACY','PROHIBITED','Pirated content','Cracked software, leaked music and uncleared copyrighted audio can''t be sold here.','Include only sounds you made or have cleared.'),
    ('P-AI-CLONE','PROHIBITED','Unauthorized voice or likeness','AI voice models or likenesses of other people can''t be sold here.','Your own voice model can be sold after verification.'),
    ('P-STREAM-FRAUD','PROHIBITED','Fake engagement','Selling streams, followers, playlist placement or accounts isn''t allowed.',NULL),
    ('P-HATE','PROHIBITED','Hate or extremism','Hateful or extremist content isn''t allowed.',NULL),
    ('P-VIOLENCE','PROHIBITED','Threats or real-world harm','Threats or content targeting real people isn''t allowed.',NULL),
    ('P-HARASSMENT','PROHIBITED','Harassment','Content targeting or exposing private people isn''t allowed.',NULL),
    ('P-FAKE-DOCS','PROHIBITED','Fake documents or money','Fake IDs, passes or realistic currency can''t be sold here.',NULL),
    ('P-FINANCIAL','PROHIBITED','Financial products or gambling','Investments, tokens, gift cards and paid raffles can''t be sold here.',NULL),
    ('P-HAZARDOUS','PROHIBITED','Hazardous or recalled','Hazardous or recalled products can''t be sold here.',NULL),
    ('P-WILDLIFE','PROHIBITED','Animals or animal parts','Live animals and protected-species products can''t be sold here.',NULL),
    ('P-MEDICAL','PROHIBITED','Health products or claims','Supplements, medicines and health claims aren''t allowed.','Remove any health or cure claims.'),
    ('P-SURVEILLANCE','PROHIBITED','Spy or hacking tools','Surveillance, tracking and hacking tools can''t be sold here.',NULL),
    ('P-OFFPLATFORM','PROHIBITED','Contact details or outside payment','Listings can''t include phone numbers, emails, social handles for contact, or outside payment links.','Remove the contact or payment details (including any in images) and resubmit.'),
    ('P-MALWARE','PROHIBITED','Unsafe files','Your files include a type we don''t accept or failed a safety scan.','Upload only audio, MIDI, preset, project, PDF or image files, in an unencrypted zip.'),
    ('P-SCAM','PROHIBITED','Misleading listing','The listing is misleading or incomplete.','Use real photos, describe exactly what ships, and give a ship date for pre-orders.'),
    ('R-NUDITY','RESTRICTED','Artistic nudity','We''re reviewing the artwork.',NULL),
    ('R-SIGNED','RESTRICTED','Signed or authentic item','We need to confirm the item is authentic.','Add a photo of the signing or a certificate.'),
    ('R-TICKETS','RESTRICTED','Tickets','We''re checking ticket details.',NULL),
    ('R-THIRD-PARTY-IP','RESTRICTED','Other artists or brands','We need proof you''re allowed to use this name, logo or likeness.','Upload the license or collaboration agreement.'),
    ('R-SAMPLES','RESTRICTED','Sample clearance','We''re checking that the sounds are original or cleared.',NULL),
    ('R-TYPE-BEAT','RESTRICTED','Type beat','We''re checking how another artist''s name is used.','Use the artist name only as a style description, not as the product title.'),
    ('R-PROP-MONEY','RESTRICTED','Prop money','We''re checking the prop currency meets our rules.',NULL),
    ('R-PARAPHERNALIA-LITE','RESTRICTED','Smoking accessories','We''re reviewing this accessory.',NULL),
    ('R-KIDS','RESTRICTED','Kids'' products','We''re checking children''s product requirements.','Confirm the item meets children''s product safety and labeling rules.'),
    ('R-FOOD','RESTRICTED','Food or drink','We''re reviewing this food or drink item.',NULL),
    ('R-RAFFLE','RESTRICTED','Giveaway or contest','We''re checking the contest rules.','Add official rules and a free way to enter.'),
    ('R-PARODY','RESTRICTED','Parody','We''re reviewing the parody content.',NULL),
    ('R-POLITICAL','RESTRICTED','Political content','We''re reviewing this political content.',NULL),
    ('R-HIGH-VALUE','RESTRICTED','High-value item','High-value items get a quick review.',NULL),
    ('R-NEW-SELLER','RESTRICTED','New seller','Your first listings get a quick review.',NULL),
    ('R-SERVICES','RESTRICTED','Service','We''re checking the service details.','Describe exactly what you deliver and when, through the platform.'),
    ('R-LOW-CONFIDENCE','RESTRICTED','Needs a closer look','A reviewer will take a quick look.',NULL);

-- ---------------------------------------------------------------------------
-- 3. Owner settings (flags from Part A6)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moderation_settings (
    setting_key VARCHAR(60) NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    value_type VARCHAR(10) NOT NULL COMMENT 'BOOL, INT, STRING, JSON',
    description VARCHAR(255) NULL,
    updated_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key),
    CONSTRAINT fk_mod_settings_user FOREIGN KEY (updated_by) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO moderation_settings (setting_key, setting_value, value_type, description) VALUES
    ('policyVersion', '2026.10-1', 'STRING', 'Recorded on every decision'),
    ('allowParaphernaliaLite', 'false', 'BOOL', 'Rolling trays, papers, grinders (via review)'),
    ('allowAlcoholBranded', 'false', 'BOOL', 'Branded alcohol (needs licensing)'),
    ('allowFood', 'false', 'BOOL', 'Food, drinks, hot sauce'),
    ('allowArtisticNudity', 'true', 'BOOL', 'Via review, with explicit flag and age gate'),
    ('allowTicketsFirstPartyOnly', 'true', 'BOOL', 'Artist/promoter tickets only; no resale'),
    ('allowPropMoney', 'false', 'BOOL', 'Music-video prop currency'),
    ('allowUsedGear', 'true', 'BOOL', 'Artist''s own gear with real photos'),
    ('allowOwnVoiceModels', 'true', 'BOOL', 'Via review, seller must prove the voice is theirs'),
    ('allowKidsApparel', 'true', 'BOOL', 'Via review'),
    ('highValueThresholdCents', '50000', 'INT', 'Above this goes to review'),
    ('newSellerReviewCount', '3', 'INT', 'First N listings per seller go to review'),
    ('aiApproveMinConfidence', '0.80', 'STRING', 'AI approve below this goes to review'),
    ('aiRejectMinConfidence', '0.90', 'STRING', 'AI reject below this goes to review'),
    ('reviewQueueSlaHours', '24', 'INT', 'Alert when an item waits longer'),
    ('shipToCountries', '["US"]', 'JSON', 'Countries the store ships to');

-- ---------------------------------------------------------------------------
-- 4. Term list for the rules stage (seeded from terms.yml by the app on first start)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moderation_terms (
    id INT NOT NULL AUTO_INCREMENT,
    reason_code VARCHAR(40) NOT NULL,
    pattern VARCHAR(255) NOT NULL,
    match_type VARCHAR(10) NOT NULL DEFAULT 'WORD' COMMENT 'WORD, PHRASE, REGEX',
    action VARCHAR(10) NOT NULL DEFAULT 'SIGNAL' COMMENT 'SIGNAL (AI decides) or REJECT (rules stage rejects)',
    active TINYINT(1) NOT NULL DEFAULT 1,
    notes VARCHAR(255) NULL,
    created_by INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_terms_code_pattern (reason_code, pattern),
    KEY idx_terms_active (active),
    CONSTRAINT fk_terms_code FOREIGN KEY (reason_code) REFERENCES moderation_reason_codes (code),
    CONSTRAINT fk_terms_user FOREIGN KEY (created_by) REFERENCES user_account (id),
    CONSTRAINT chk_terms_match CHECK (match_type IN ('WORD','PHRASE','REGEX')),
    CONSTRAINT chk_terms_action CHECK (action IN ('SIGNAL','REJECT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 5. Pipeline job queue (one row per listing version)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moderation_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchandise_id INT NOT NULL,
    version INT NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED, RUNNING, DONE, FAILED',
    current_stage VARCHAR(10) NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by VARCHAR(64) NULL COMMENT 'worker id, for safe parallel processing',
    locked_at TIMESTAMP NULL,
    last_error VARCHAR(500) NULL,
    request_id VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jobs_merch_version (merchandise_id, version),
    KEY idx_jobs_pick (status, next_attempt_at),
    CONSTRAINT fk_jobs_merch FOREIGN KEY (merchandise_id) REFERENCES merchandise (id) ON DELETE CASCADE,
    CONSTRAINT chk_jobs_status CHECK (status IN ('QUEUED','RUNNING','DONE','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 6. Decisions (audit trail) and signals
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moderation_decisions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchandise_id INT NOT NULL,
    version INT NOT NULL,
    stage VARCHAR(10) NOT NULL COMMENT 'RULES, FILES, IMAGES, AI, HUMAN, APPEAL, FINAL',
    decision VARCHAR(12) NOT NULL COMMENT 'PASS, APPROVE, REVIEW, REJECT, BLOCK',
    reason_codes JSON NOT NULL DEFAULT (JSON_ARRAY()),
    confidence DECIMAL(4,3) NULL,
    summary VARCHAR(1000) NULL COMMENT 'Internal; never shown to the seller',
    seller_message VARCHAR(500) NULL,
    model VARCHAR(100) NULL COMMENT 'AI model or service name/version',
    policy_version VARCHAR(20) NOT NULL,
    reviewer_id INT NULL COMMENT 'Set for HUMAN and APPEAL stages',
    duration_ms INT NULL,
    request_id VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_decisions_merch_version (merchandise_id, version, stage),
    KEY idx_decisions_reviewer (reviewer_id, created_at),
    KEY idx_decisions_created (created_at),
    CONSTRAINT fk_decisions_merch FOREIGN KEY (merchandise_id) REFERENCES merchandise (id) ON DELETE CASCADE,
    CONSTRAINT fk_decisions_reviewer FOREIGN KEY (reviewer_id) REFERENCES user_account (id),
    CONSTRAINT chk_decisions_stage CHECK (stage IN ('RULES','FILES','IMAGES','AI','HUMAN','APPEAL','FINAL')),
    CONSTRAINT chk_decisions_decision CHECK (decision IN ('PASS','APPROVE','REVIEW','REJECT','BLOCK'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS moderation_signals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchandise_id INT NOT NULL,
    version INT NOT NULL,
    source VARCHAR(10) NOT NULL COMMENT 'RULES, FILES, IMAGES',
    reason_code VARCHAR(40) NULL,
    signal_type VARCHAR(40) NOT NULL COMMENT 'term_match, price, image_label, ocr_text, new_seller, duplicate, prompt_injection, ...',
    field VARCHAR(40) NULL COMMENT 'title, description, options, image_1..5, file',
    matched VARCHAR(255) NULL COMMENT 'Matched term or label; never full OCR text',
    ai_verdict VARCHAR(10) NULL COMMENT 'ok or problem, filled after the AI stage',
    ai_why VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_signals_merch_version (merchandise_id, version),
    KEY idx_signals_code (reason_code, created_at),
    CONSTRAINT fk_signals_merch FOREIGN KEY (merchandise_id) REFERENCES merchandise (id) ON DELETE CASCADE,
    CONSTRAINT fk_signals_code FOREIGN KEY (reason_code) REFERENCES moderation_reason_codes (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 7. Digital product files and image scans
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS merchandise_digital_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchandise_id INT NOT NULL,
    version INT NOT NULL,
    archive_key VARCHAR(500) NOT NULL COMMENT 'Storage key of the uploaded zip/file',
    entry_path VARCHAR(500) NOT NULL COMMENT 'Path inside the archive (or the file name)',
    detected_type VARCHAR(100) NULL COMMENT 'From magic bytes, not the extension',
    extension VARCHAR(20) NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NULL,
    allowed TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Passed the extension/type allow-list',
    scan_status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, CLEAN, INFECTED, ERROR, SKIPPED',
    scan_engine VARCHAR(60) NULL COMMENT 'e.g. clamav 1.x + signature date',
    threat_name VARCHAR(200) NULL,
    fingerprint_match VARCHAR(255) NULL COMMENT 'Audio fingerprint match, if that service is enabled',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_files_merch_version (merchandise_id, version),
    KEY idx_files_sha (sha256),
    CONSTRAINT fk_files_merch FOREIGN KEY (merchandise_id) REFERENCES merchandise (id) ON DELETE CASCADE,
    CONSTRAINT chk_files_scan CHECK (scan_status IN ('PENDING','CLEAN','INFECTED','ERROR','SKIPPED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS moderation_image_scans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchandise_id INT NOT NULL,
    version INT NOT NULL,
    image_index TINYINT NOT NULL COMMENT '1-5, matching image_blob..image_blob_5 or the media table',
    image_sha256 CHAR(64) NULL COMMENT 'Lets the app skip re-scanning unchanged images and spot re-uploads of rejected ones',
    adult VARCHAR(16) NULL COMMENT 'Likelihood: VERY_UNLIKELY .. VERY_LIKELY',
    racy VARCHAR(16) NULL,
    violence VARCHAR(16) NULL,
    medical VARCHAR(16) NULL,
    spoof VARCHAR(16) NULL,
    labels JSON NULL COMMENT 'Top labels from the classifier',
    ocr_text VARCHAR(2000) NULL COMMENT 'Truncated; restricted to admins',
    provider VARCHAR(60) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_image_scan (merchandise_id, version, image_index),
    KEY idx_image_sha (image_sha256),
    CONSTRAINT fk_image_scans_merch FOREIGN KEY (merchandise_id) REFERENCES merchandise (id) ON DELETE CASCADE,
    CONSTRAINT chk_image_index CHECK (image_index BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 8. Appeals and seller stats
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moderation_appeals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchandise_id INT NOT NULL,
    version INT NOT NULL,
    seller_id INT NOT NULL,
    message VARCHAR(2000) NOT NULL,
    attachment_key VARCHAR(500) NULL COMMENT 'e.g. license or certificate of authenticity',
    status VARCHAR(12) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, UPHELD, OVERTURNED, WITHDRAWN',
    resolved_by INT NULL,
    resolution_note VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_appeal_once_per_version (merchandise_id, version),
    KEY idx_appeals_status_created (status, created_at),
    KEY idx_appeals_seller (seller_id),
    CONSTRAINT fk_appeals_merch FOREIGN KEY (merchandise_id) REFERENCES merchandise (id) ON DELETE CASCADE,
    CONSTRAINT fk_appeals_seller FOREIGN KEY (seller_id) REFERENCES user_account (id),
    CONSTRAINT fk_appeals_resolver FOREIGN KEY (resolved_by) REFERENCES user_account (id),
    CONSTRAINT chk_appeals_status CHECK (status IN ('OPEN','UPHELD','OVERTURNED','WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS seller_moderation_stats (
    seller_id INT NOT NULL,
    approved_count INT NOT NULL DEFAULT 0,
    rejected_count INT NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    blocked_count INT NOT NULL DEFAULT 0,
    strikes INT NOT NULL DEFAULT 0 COMMENT 'Prohibited-code rejections upheld by a human or appeal',
    last_violation_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (seller_id),
    CONSTRAINT fk_seller_stats_user FOREIGN KEY (seller_id) REFERENCES user_account (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 9. Child safety cases and admin alerts
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS child_safety_cases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchandise_id INT NULL,
    seller_id INT NULL,
    detected_by VARCHAR(20) NOT NULL COMMENT 'IMAGE_CLASSIFIER, HASH_MATCH, AI_REVIEW, HUMAN',
    evidence_key VARCHAR(500) NOT NULL COMMENT 'Location in restricted, encrypted storage; never the public bucket',
    evidence_sha256 CHAR(64) NULL,
    legal_hold TINYINT(1) NOT NULL DEFAULT 1,
    report_status VARCHAR(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, REPORTED, NOT_REQUIRED',
    report_reference VARCHAR(100) NULL COMMENT 'e.g. CyberTipline report id',
    reported_by INT NULL,
    reported_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_csc_status (report_status, created_at),
    CONSTRAINT fk_csc_merch FOREIGN KEY (merchandise_id) REFERENCES merchandise (id),
    CONSTRAINT fk_csc_seller FOREIGN KEY (seller_id) REFERENCES user_account (id),
    CONSTRAINT fk_csc_reporter FOREIGN KEY (reported_by) REFERENCES user_account (id),
    CONSTRAINT chk_csc_report CHECK (report_status IN ('PENDING','REPORTED','NOT_REQUIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS admin_alerts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alert_type VARCHAR(40) NOT NULL COMMENT 'CHILD_SAFETY, MALWARE, QUEUE_SLA, AI_OUTAGE, REPEAT_OFFENDER',
    severity VARCHAR(10) NOT NULL COMMENT 'CRITICAL, HIGH, NORMAL',
    merchandise_id INT NULL,
    seller_id INT NULL,
    message VARCHAR(500) NOT NULL COMMENT 'Never contains images, file contents or OCR text',
    acknowledged_by INT NULL,
    acknowledged_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_alerts_open (acknowledged_at, severity, created_at),
    CONSTRAINT fk_alerts_merch FOREIGN KEY (merchandise_id) REFERENCES merchandise (id) ON DELETE SET NULL,
    CONSTRAINT fk_alerts_seller FOREIGN KEY (seller_id) REFERENCES user_account (id) ON DELETE SET NULL,
    CONSTRAINT fk_alerts_ack FOREIGN KEY (acknowledged_by) REFERENCES user_account (id),
    CONSTRAINT chk_alerts_severity CHECK (severity IN ('CRITICAL','HIGH','NORMAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO seller_moderation_stats (seller_id)
SELECT DISTINCT seller_id FROM merchandise WHERE seller_id IS NOT NULL;
