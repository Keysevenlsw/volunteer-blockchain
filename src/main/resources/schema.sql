CREATE TABLE IF NOT EXISTS organizations (
    organization_id INT AUTO_INCREMENT PRIMARY KEY,
    organization_name VARCHAR(255) NOT NULL,
    organization_description TEXT,
    avatar_path VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    avatar_path VARCHAR(255),
    organization_id INT NULL,
    role ENUM('volunteer', 'organization_admin', 'system_admin', 'product_reviewer', 'activity_reviewer') NOT NULL,
    join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_points INT NOT NULL DEFAULT 0,
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id)
);

CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    is_system TINYINT(1) NOT NULL DEFAULT 0,
    data_scope VARCHAR(50) NOT NULL DEFAULT 'SELF',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    module VARCHAR(100) NOT NULL,
    http_method VARCHAR(20) NOT NULL,
    api_pattern VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_roles_user_role (user_id, role_id),
    KEY idx_user_roles_user_id (user_id),
    KEY idx_user_roles_role_id (role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_role_permissions_role_permission (role_id, permission_id),
    KEY idx_role_permissions_role_id (role_id),
    KEY idx_role_permissions_permission_id (permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE TABLE IF NOT EXISTS organization_join_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    organization_id INT NOT NULL,
    apply_reason VARCHAR(500),
    status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
    reviewed_by INT NULL,
    review_note VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS activities (
    activity_id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    activity_name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    publish_date DATETIME NULL,
    location VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    contact_phone VARCHAR(20),
    category_tags VARCHAR(255),
    image_path VARCHAR(255),
    max_participants INT NOT NULL DEFAULT 1,
    current_participants INT NOT NULL DEFAULT 0,
    enroll_deadline DATETIME NULL,
    created_by INT NOT NULL,
    requested_reward_points INT NOT NULL DEFAULT 0,
    approved_reward_points INT NULL,
    review_status ENUM('draft', 'pending_review', 'approved', 'rejected') NOT NULL DEFAULT 'draft',
    review_note VARCHAR(500),
    reviewed_by INT NULL,
    reviewed_at DATETIME NULL,
    status ENUM('pending', 'ongoing', 'completed') NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS activity_publish_reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL,
    reviewer_id INT NOT NULL,
    reviewer_role ENUM('organization_admin', 'activity_reviewer', 'system_admin') NOT NULL,
    review_action ENUM('submitted', 'approved', 'rejected', 'escalated') NOT NULL,
    requested_reward_points INT NOT NULL DEFAULT 0,
    approved_reward_points INT NULL,
    review_note VARCHAR(500),
    is_escalated TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id),
    FOREIGN KEY (reviewer_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS activity_participation (
    participation_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    activity_id INT NOT NULL,
    earned_points INT NOT NULL DEFAULT 0,
    participation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_activity_participation_user_activity (user_id, activity_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id)
);

CREATE TABLE IF NOT EXISTS activity_completion (
    completion_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    activity_id INT NOT NULL,
    completion_status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
    report_text TEXT,
    service_location VARCHAR(255) NOT NULL,
    service_start_time DATETIME NOT NULL,
    service_end_time DATETIME NOT NULL,
    contribution_details TEXT,
    attachment_paths TEXT,
    reject_reason VARCHAR(500),
    approved_by INT NULL,
    approved_at DATETIME NULL,
    submission_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (activity_id) REFERENCES activities(activity_id),
    FOREIGN KEY (approved_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS completion_reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    completion_id INT NOT NULL,
    reviewer_id INT NOT NULL,
    review_action ENUM('approved', 'rejected') NOT NULL,
    review_note VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (completion_id) REFERENCES activity_completion(completion_id),
    FOREIGN KEY (reviewer_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS points_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    organization_id INT NULL,
    points INT NOT NULL,
    transaction_type ENUM('earned', 'spent') NOT NULL,
    source VARCHAR(255) NOT NULL,
    reference_type VARCHAR(50),
    reference_id INT,
    biz_key VARCHAR(255),
    digest VARCHAR(128),
    tx_hash VARCHAR(255),
    block_number VARCHAR(64),
    contract_name VARCHAR(255),
    onchain_status ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'success',
    error_message VARCHAR(1000),
    onchain_at DATETIME NULL,
    chain_balance_after INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id)
);

CREATE TABLE IF NOT EXISTS points_rule_configs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activity_category VARCHAR(100) NOT NULL,
    min_service_hours DECIMAL(6,2) NOT NULL,
    max_service_hours DECIMAL(6,2) NOT NULL,
    suggested_points INT NOT NULL,
    max_points INT NOT NULL,
    escalation_threshold INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by INT NOT NULL,
    updated_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    FOREIGN KEY (updated_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_description TEXT,
    price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image_path VARCHAR(255),
    created_by INT NOT NULL,
    review_status ENUM('draft', 'pending_review', 'approved', 'rejected', 'off_shelf') NOT NULL DEFAULT 'draft',
    review_note VARCHAR(500),
    reviewed_by INT NULL,
    reviewed_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS product_reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    reviewer_id INT NOT NULL,
    review_action ENUM('submitted', 'approved', 'rejected', 'off_shelf') NOT NULL,
    review_note VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (reviewer_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS product_redemptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    organization_id INT NOT NULL,
    points_cost INT NOT NULL,
    status ENUM('pending', 'fulfilled', 'cancelled') NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    handled_at DATETIME NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id)
);

CREATE TABLE IF NOT EXISTS blockchain_evidences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    biz_type VARCHAR(50) NOT NULL,
    biz_id INT NOT NULL,
    digest VARCHAR(128) NOT NULL,
    tx_hash VARCHAR(255),
    block_number VARCHAR(64),
    contract_name VARCHAR(255),
    onchain_status ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'pending',
    error_message VARCHAR(1000),
    storage_path VARCHAR(255),
    reviewer_name VARCHAR(255),
    reviewed_at DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    onchain_at DATETIME NULL,
    UNIQUE KEY uq_blockchain_evidences_biz (biz_type, biz_id)
);

CREATE TABLE IF NOT EXISTS jwt_tokens (
    token_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_activities_review_org_publish
    ON activities(review_status, organization_id, publish_date);

CREATE INDEX idx_products_review_org_product
    ON products(review_status, organization_id, product_id);

CREATE INDEX idx_points_records_user_created
    ON points_records(user_id, created_at);

CREATE UNIQUE INDEX uq_points_records_biz_key
    ON points_records(biz_key);

CREATE INDEX idx_points_records_onchain_status
    ON points_records(onchain_status, created_at);

CREATE INDEX idx_product_redemptions_org_status_created
    ON product_redemptions(organization_id, status, created_at);
