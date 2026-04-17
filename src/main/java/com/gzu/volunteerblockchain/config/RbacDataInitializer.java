package com.gzu.volunteerblockchain.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gzu.volunteerblockchain.common.PermissionConstants;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.entity.Permission;
import com.gzu.volunteerblockchain.entity.PointsRuleConfig;
import com.gzu.volunteerblockchain.entity.Role;
import com.gzu.volunteerblockchain.entity.RolePermission;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.entity.UserRole;
import com.gzu.volunteerblockchain.mapper.PermissionMapper;
import com.gzu.volunteerblockchain.mapper.PointsRuleConfigMapper;
import com.gzu.volunteerblockchain.mapper.RoleMapper;
import com.gzu.volunteerblockchain.mapper.RolePermissionMapper;
import com.gzu.volunteerblockchain.mapper.UserMapper;
import com.gzu.volunteerblockchain.mapper.UserRoleMapper;
import com.gzu.volunteerblockchain.service.RbacService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RbacDataInitializer implements ApplicationRunner {

    private static final List<RoleSeed> ROLE_SEEDS = List.of(
        new RoleSeed(RoleConstants.VOLUNTEER, "志愿者", false, "SELF", 10),
        new RoleSeed(RoleConstants.ORGANIZATION_ADMIN, "组织管理员", false, "ORG", 20),
        new RoleSeed(RoleConstants.ACTIVITY_REVIEWER, "活动审核员", false, "PLATFORM", 30),
        new RoleSeed(RoleConstants.PRODUCT_REVIEWER, "商品审核员", false, "PLATFORM", 40),
        new RoleSeed(RoleConstants.SYSTEM_ADMIN, "超级管理员", true, "ALL", 50)
    );

    private static final List<PermissionSeed> PERMISSION_SEEDS = List.of(
        new PermissionSeed("org:list", "查看组织列表", "org", "GET", "/api/organizations"),
        new PermissionSeed("org:join_request:create", "发起归属组织申请", "org", "POST", "/api/join-requests"),
        new PermissionSeed("org:join_request:list_mine", "查看我的归属组织申请", "org", "GET", "/api/join-requests/mine"),
        new PermissionSeed("org:join_request:list_org", "查看组织归属申请", "org", "GET", "/api/join-requests/organization"),
        new PermissionSeed("org:join_request:review", "审核归属组织申请", "org", "POST", "/api/join-requests/{id}/review"),
        new PermissionSeed("activity:create", "创建活动草稿", "activity", "POST", "/api/activities"),
        new PermissionSeed("activity:update", "更新活动草稿", "activity", "PUT", "/api/activities/{id}"),
        new PermissionSeed("activity:delete", "删除活动草稿", "activity", "DELETE", "/api/activities/{id}"),
        new PermissionSeed("activity:submit", "提交活动审核", "activity", "POST", "/api/activities/{id}/submit"),
        new PermissionSeed("activity:list_volunteer", "查看全平台活动列表", "activity", "GET", "/api/activities/volunteer"),
        new PermissionSeed("activity:list_org", "查看本组织活动列表", "activity", "GET", "/api/activities/organization"),
        new PermissionSeed("participation:join", "报名活动", "participation", "POST", "/api/participations/{activityId}"),
        new PermissionSeed("participation:cancel", "取消报名", "participation", "DELETE", "/api/participations/{activityId}"),
        new PermissionSeed("participation:list_mine", "查看我的报名记录", "participation", "GET", "/api/participations/mine"),
        new PermissionSeed("completion:submit", "提交完成报告", "completion", "POST", "/api/completions"),
        new PermissionSeed("completion:list_mine", "查看我的完成报告", "completion", "GET", "/api/completions/mine"),
        new PermissionSeed("completion:review:list", "查看组织待审完成报告", "completion", "GET", "/api/reviews"),
        new PermissionSeed("completion:review", "审核完成报告", "completion", "POST", "/api/reviews/{completionId}"),
        new PermissionSeed("points:list", "查看积分流水", "points", "GET", "/api/points"),
        new PermissionSeed("product:create", "创建商品草稿", "product", "POST", "/api/products"),
        new PermissionSeed("product:update", "更新商品草稿", "product", "PUT", "/api/products/{id}"),
        new PermissionSeed("product:delete", "删除商品草稿", "product", "DELETE", "/api/products/{id}"),
        new PermissionSeed("product:submit", "提交商品审核", "product", "POST", "/api/products/{id}/submit"),
        new PermissionSeed("product:list_volunteer", "查看统一商城商品列表", "product", "GET", "/api/products/volunteer"),
        new PermissionSeed("product:list_org", "查看本组织商品列表", "product", "GET", "/api/products/organization"),
        new PermissionSeed("redemption:create", "发起商品兑换", "redemption", "POST", "/api/redemptions"),
        new PermissionSeed("redemption:list_mine", "查看我的兑换记录", "redemption", "GET", "/api/redemptions/mine"),
        new PermissionSeed("redemption:list_org", "查看组织兑换记录", "redemption", "GET", "/api/redemptions/organization"),
        new PermissionSeed("redemption:status:update", "处理兑换状态", "redemption", "POST", "/api/redemptions/{id}/status"),
        new PermissionSeed("evidence:view", "查看链上存证", "evidence", "GET", "/api/evidences/{bizType}/{bizId}"),
        new PermissionSeed("evidence:list_org", "查看组织链上存证", "evidence", "GET", "/api/evidences/organization"),
        new PermissionSeed("evidence:retry", "重试链上存证", "evidence", "POST", "/api/evidences/{id}/retry"),
        new PermissionSeed("file:upload", "上传附件", "file", "POST", "/api/files/upload"),
        new PermissionSeed(PermissionConstants.ADMIN_USER_LIST, "查看用户列表", "admin", "GET", "/api/admin/users"),
        new PermissionSeed(PermissionConstants.ADMIN_USER_ROLES_VIEW, "查看用户角色", "admin", "GET", "/api/admin/users/{userId}/roles"),
        new PermissionSeed(PermissionConstants.ADMIN_USER_ROLES_ASSIGN, "分配用户角色", "admin", "PUT", "/api/admin/users/{userId}/roles"),
        new PermissionSeed(PermissionConstants.ADMIN_ROLE_LIST, "查看角色列表", "admin", "GET", "/api/admin/roles"),
        new PermissionSeed(PermissionConstants.ADMIN_ROLE_PERMISSIONS_VIEW, "查看角色权限", "admin", "GET", "/api/admin/roles/{roleCode}/permissions"),
        new PermissionSeed(PermissionConstants.ADMIN_PERMISSION_LIST, "查看权限列表", "admin", "GET", "/api/admin/permissions"),
        new PermissionSeed(PermissionConstants.ADMIN_ACTIVITY_REVIEW_LIST, "查看活动审核列表", "admin", "GET", "/api/admin/activity-reviews"),
        new PermissionSeed(PermissionConstants.ADMIN_ACTIVITY_REVIEW_HANDLE, "处理活动审核", "admin", "POST", "/api/admin/activity-reviews/{activityId}"),
        new PermissionSeed(PermissionConstants.ADMIN_PRODUCT_REVIEW_LIST, "查看商品审核列表", "admin", "GET", "/api/admin/product-reviews"),
        new PermissionSeed(PermissionConstants.ADMIN_PRODUCT_REVIEW_HANDLE, "处理商品审核", "admin", "POST", "/api/admin/product-reviews/{productId}"),
        new PermissionSeed(PermissionConstants.ADMIN_POINTS_RULE_LIST, "查看积分规则", "admin", "GET", "/api/admin/points-rules"),
        new PermissionSeed(PermissionConstants.ADMIN_POINTS_RULE_CREATE, "创建积分规则", "admin", "POST", "/api/admin/points-rules"),
        new PermissionSeed(PermissionConstants.ADMIN_POINTS_RULE_UPDATE, "更新积分规则", "admin", "PUT", "/api/admin/points-rules/{id}"),
        new PermissionSeed(PermissionConstants.ADMIN_POINTS_SYNC_BALANCES, "同步链上积分余额", "admin", "POST", "/api/admin/points/sync-balances"),
        new PermissionSeed(PermissionConstants.ADMIN_POINTS_MIGRATE, "历史积分迁移上链", "admin", "POST", "/api/admin/points/migrate")
    );

    private final JdbcTemplate jdbcTemplate;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserMapper userMapper;
    private final PointsRuleConfigMapper pointsRuleConfigMapper;
    private final PasswordEncoder passwordEncoder;
    private final RbacService rbacService;

    @Value("${rbac.system-admin.username:}")
    private String systemAdminUsername;

    @Value("${rbac.system-admin.email:}")
    private String systemAdminEmail;

    @Value("${rbac.system-admin.password:}")
    private String systemAdminPassword;

    public RbacDataInitializer(
        JdbcTemplate jdbcTemplate,
        RoleMapper roleMapper,
        PermissionMapper permissionMapper,
        UserRoleMapper userRoleMapper,
        RolePermissionMapper rolePermissionMapper,
        UserMapper userMapper,
        PointsRuleConfigMapper pointsRuleConfigMapper,
        PasswordEncoder passwordEncoder,
        RbacService rbacService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userMapper = userMapper;
        this.pointsRuleConfigMapper = pointsRuleConfigMapper;
        this.passwordEncoder = passwordEncoder;
        this.rbacService = rbacService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureCoreTables();
        Map<String, Role> roleMap = seedRoles();
        Map<String, Permission> permissionMap = seedPermissions();
        seedRolePermissions(roleMap, permissionMap);
        backfillUserRoles(roleMap);
        syncPrimaryRoles();
        User systemAdmin = bootstrapSystemAdmin();
        if (systemAdmin != null) {
            seedDefaultPointsRules(systemAdmin.getUserId());
        }
    }

    private void ensureCoreTables() {
        execute("""
            CREATE TABLE IF NOT EXISTS organizations (
                organization_id INT AUTO_INCREMENT PRIMARY KEY,
                organization_name VARCHAR(255) NOT NULL,
                organization_description TEXT,
                avatar_path VARCHAR(255),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
        execute("""
            CREATE TABLE IF NOT EXISTS users (
                user_id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(100) NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                email VARCHAR(100) NOT NULL UNIQUE,
                avatar_path VARCHAR(255),
                organization_id INT NULL,
                role ENUM('volunteer', 'organization_admin', 'system_admin', 'product_reviewer', 'activity_reviewer') NOT NULL,
                join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                total_points INT NOT NULL DEFAULT 0
            )
            """);
        execute("""
            CREATE TABLE IF NOT EXISTS activities (
                activity_id INT AUTO_INCREMENT PRIMARY KEY,
                organization_id INT NOT NULL,
                activity_name VARCHAR(255) NOT NULL,
                description TEXT,
                start_date DATETIME,
                end_date DATETIME,
                publish_date DATETIME NULL,
                location VARCHAR(255),
                contact_name VARCHAR(255),
                contact_phone VARCHAR(20),
                category_tags VARCHAR(255),
                image_path VARCHAR(255),
                max_participants INT NOT NULL DEFAULT 1,
                current_participants INT NOT NULL DEFAULT 0,
                enroll_deadline DATETIME NULL,
                created_by INT NULL,
                requested_reward_points INT NOT NULL DEFAULT 0,
                approved_reward_points INT NULL,
                review_status ENUM('draft', 'pending_review', 'approved', 'rejected') NOT NULL DEFAULT 'draft',
                review_note VARCHAR(500),
                reviewed_by INT NULL,
                reviewed_at DATETIME NULL,
                status ENUM('pending', 'ongoing', 'completed') NOT NULL DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """);
        execute("""
            CREATE TABLE IF NOT EXISTS products (
                product_id INT AUTO_INCREMENT PRIMARY KEY,
                organization_id INT NOT NULL,
                product_name VARCHAR(255) NOT NULL,
                product_description TEXT,
                price INT NOT NULL,
                stock INT NOT NULL DEFAULT 0,
                image_path VARCHAR(255),
                created_by INT NULL,
                review_status ENUM('draft', 'pending_review', 'approved', 'rejected', 'off_shelf') NOT NULL DEFAULT 'draft',
                review_note VARCHAR(500),
                reviewed_by INT NULL,
                reviewed_at DATETIME NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """);
        execute("""
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
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
        execute("""
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
            )
            """);
        execute("""
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
            )
            """);
        execute("""
            CREATE TABLE IF NOT EXISTS user_roles (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                role_id INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uq_user_roles_user_role (user_id, role_id)
            )
            """);
        execute("""
            CREATE TABLE IF NOT EXISTS role_permissions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                role_id INT NOT NULL,
                permission_id INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uq_role_permissions_role_permission (role_id, permission_id)
            )
            """);
        execute("""
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
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
        execute("""
            CREATE TABLE IF NOT EXISTS product_reviews (
                id INT AUTO_INCREMENT PRIMARY KEY,
                product_id INT NOT NULL,
                reviewer_id INT NOT NULL,
                review_action ENUM('submitted', 'approved', 'rejected', 'off_shelf') NOT NULL,
                review_note VARCHAR(500),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
        execute("""
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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """);
        execute("""
            ALTER TABLE users
            MODIFY COLUMN role ENUM('volunteer', 'organization_admin', 'system_admin', 'product_reviewer', 'activity_reviewer') NOT NULL
            """);
        ensureColumn("organizations", "created_at", "ALTER TABLE organizations ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        ensureColumn("activities", "requested_reward_points", "ALTER TABLE activities ADD COLUMN requested_reward_points INT NOT NULL DEFAULT 0");
        ensureColumn("activities", "approved_reward_points", "ALTER TABLE activities ADD COLUMN approved_reward_points INT NULL");
        ensureColumn("activities", "review_status", "ALTER TABLE activities ADD COLUMN review_status ENUM('draft', 'pending_review', 'approved', 'rejected') NOT NULL DEFAULT 'draft'");
        ensureColumn("activities", "review_note", "ALTER TABLE activities ADD COLUMN review_note VARCHAR(500)");
        ensureColumn("activities", "reviewed_by", "ALTER TABLE activities ADD COLUMN reviewed_by INT NULL");
        ensureColumn("activities", "reviewed_at", "ALTER TABLE activities ADD COLUMN reviewed_at DATETIME NULL");
        ensureColumn("activities", "created_at", "ALTER TABLE activities ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        ensureColumn("activities", "updated_at", "ALTER TABLE activities ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        ensureColumn("products", "created_by", "ALTER TABLE products ADD COLUMN created_by INT NULL");
        ensureColumn("products", "review_status", "ALTER TABLE products ADD COLUMN review_status ENUM('draft', 'pending_review', 'approved', 'rejected', 'off_shelf') NOT NULL DEFAULT 'draft'");
        ensureColumn("products", "review_note", "ALTER TABLE products ADD COLUMN review_note VARCHAR(500)");
        ensureColumn("products", "reviewed_by", "ALTER TABLE products ADD COLUMN reviewed_by INT NULL");
        ensureColumn("products", "reviewed_at", "ALTER TABLE products ADD COLUMN reviewed_at DATETIME NULL");
        ensureColumn("products", "created_at", "ALTER TABLE products ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        ensureColumn("products", "updated_at", "ALTER TABLE products ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        execute("ALTER TABLE points_records MODIFY COLUMN organization_id INT NULL");
        ensureColumn("points_records", "biz_key", "ALTER TABLE points_records ADD COLUMN biz_key VARCHAR(255)");
        ensureColumn("points_records", "digest", "ALTER TABLE points_records ADD COLUMN digest VARCHAR(128)");
        ensureColumn("points_records", "tx_hash", "ALTER TABLE points_records ADD COLUMN tx_hash VARCHAR(255)");
        ensureColumn("points_records", "block_number", "ALTER TABLE points_records ADD COLUMN block_number VARCHAR(64)");
        ensureColumn("points_records", "contract_name", "ALTER TABLE points_records ADD COLUMN contract_name VARCHAR(255)");
        ensureColumn("points_records", "onchain_status", "ALTER TABLE points_records ADD COLUMN onchain_status ENUM('pending', 'success', 'failed') NOT NULL DEFAULT 'success'");
        ensureColumn("points_records", "error_message", "ALTER TABLE points_records ADD COLUMN error_message VARCHAR(1000)");
        ensureColumn("points_records", "onchain_at", "ALTER TABLE points_records ADD COLUMN onchain_at DATETIME NULL");
        ensureColumn("points_records", "chain_balance_after", "ALTER TABLE points_records ADD COLUMN chain_balance_after INT");
        ensureIndex("activities", "idx_activities_review_org_publish",
            "CREATE INDEX idx_activities_review_org_publish ON activities(review_status, organization_id, publish_date)");
        ensureIndex("products", "idx_products_review_org_product",
            "CREATE INDEX idx_products_review_org_product ON products(review_status, organization_id, product_id)");
        ensureIndex("points_records", "idx_points_records_user_created",
            "CREATE INDEX idx_points_records_user_created ON points_records(user_id, created_at)");
        ensureIndex("points_records", "uq_points_records_biz_key",
            "CREATE UNIQUE INDEX uq_points_records_biz_key ON points_records(biz_key)");
        ensureIndex("points_records", "idx_points_records_onchain_status",
            "CREATE INDEX idx_points_records_onchain_status ON points_records(onchain_status, created_at)");
        ensureIndex("product_redemptions", "idx_product_redemptions_org_status_created",
            "CREATE INDEX idx_product_redemptions_org_status_created ON product_redemptions(organization_id, status, created_at)");
    }

    private Map<String, Role> seedRoles() {
        Map<String, Role> roleMap = new LinkedHashMap<>();
        for (RoleSeed seed : ROLE_SEEDS) {
            Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, seed.code())
                .last("LIMIT 1"));
            if (role == null) {
                role = new Role();
                role.setCode(seed.code());
                role.setCreatedAt(LocalDateTime.now());
            }
            role.setName(seed.name());
            role.setStatus(1);
            role.setIsSystem(seed.systemRole());
            role.setDataScope(seed.dataScope());
            role.setSortOrder(seed.sortOrder());
            role.setUpdatedAt(LocalDateTime.now());
            if (role.getId() == null) {
                roleMapper.insert(role);
            } else {
                roleMapper.updateById(role);
            }
            roleMap.put(role.getCode(), role);
        }
        return roleMap;
    }

    private Map<String, Permission> seedPermissions() {
        Map<String, Permission> permissionMap = new LinkedHashMap<>();
        for (PermissionSeed seed : PERMISSION_SEEDS) {
            Permission permission = permissionMapper.selectOne(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getCode, seed.code())
                .last("LIMIT 1"));
            if (permission == null) {
                permission = new Permission();
                permission.setCode(seed.code());
                permission.setCreatedAt(LocalDateTime.now());
            }
            permission.setName(seed.name());
            permission.setModule(seed.module());
            permission.setHttpMethod(seed.httpMethod());
            permission.setApiPattern(seed.apiPattern());
            permission.setStatus(1);
            permission.setUpdatedAt(LocalDateTime.now());
            if (permission.getId() == null) {
                permissionMapper.insert(permission);
            } else {
                permissionMapper.updateById(permission);
            }
            permissionMap.put(permission.getCode(), permission);
        }
        return permissionMap;
    }

    private void seedRolePermissions(Map<String, Role> roleMap, Map<String, Permission> permissionMap) {
        Map<String, List<String>> permissionCodesByRole = new LinkedHashMap<>();
        permissionCodesByRole.put(RoleConstants.VOLUNTEER, List.of(
            "org:list",
            "org:join_request:create",
            "org:join_request:list_mine",
            "activity:list_volunteer",
            "participation:join",
            "participation:cancel",
            "participation:list_mine",
            "completion:submit",
            "completion:list_mine",
            "points:list",
            "product:list_volunteer",
            "redemption:create",
            "redemption:list_mine",
            "evidence:view",
            "file:upload"
        ));
        permissionCodesByRole.put(RoleConstants.ORGANIZATION_ADMIN, List.of(
            "org:list",
            "org:join_request:list_org",
            "org:join_request:review",
            "activity:create",
            "activity:update",
            "activity:delete",
            "activity:submit",
            "activity:list_org",
            "completion:review:list",
            "completion:review",
            "product:create",
            "product:update",
            "product:delete",
            "product:submit",
            "product:list_org",
            "redemption:list_org",
            "redemption:status:update",
            "evidence:view",
            "evidence:list_org",
            "evidence:retry",
            "file:upload"
        ));
        permissionCodesByRole.put(RoleConstants.ACTIVITY_REVIEWER, List.of(
            PermissionConstants.ADMIN_ACTIVITY_REVIEW_LIST,
            PermissionConstants.ADMIN_ACTIVITY_REVIEW_HANDLE
        ));
        permissionCodesByRole.put(RoleConstants.PRODUCT_REVIEWER, List.of(
            PermissionConstants.ADMIN_PRODUCT_REVIEW_LIST,
            PermissionConstants.ADMIN_PRODUCT_REVIEW_HANDLE
        ));
        permissionCodesByRole.put(RoleConstants.SYSTEM_ADMIN, PERMISSION_SEEDS.stream().map(PermissionSeed::code).toList());

        for (Map.Entry<String, List<String>> entry : permissionCodesByRole.entrySet()) {
            Role role = roleMap.get(entry.getKey());
            if (role == null) {
                continue;
            }
            for (String permissionCode : entry.getValue()) {
                Permission permission = permissionMap.get(permissionCode);
                if (permission == null) {
                    continue;
                }
                Long exists = rolePermissionMapper.selectCount(new LambdaQueryWrapper<RolePermission>()
                    .eq(RolePermission::getRoleId, role.getId())
                    .eq(RolePermission::getPermissionId, permission.getId()));
                if (exists != null && exists > 0) {
                    continue;
                }
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(role.getId());
                rolePermission.setPermissionId(permission.getId());
                rolePermission.setCreatedAt(LocalDateTime.now());
                rolePermissionMapper.insert(rolePermission);
            }
        }
    }

    private void backfillUserRoles(Map<String, Role> roleMap) {
        for (User user : userMapper.selectList(null)) {
            if (user.getRole() == null || user.getRole().isBlank()) {
                continue;
            }
            Role role = roleMap.get(user.getRole());
            if (role == null) {
                continue;
            }
            Long exists = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getUserId())
                .eq(UserRole::getRoleId, role.getId()));
            if (exists != null && exists > 0) {
                continue;
            }
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getUserId());
            userRole.setRoleId(role.getId());
            userRole.setCreatedAt(LocalDateTime.now());
            userRoleMapper.insert(userRole);
        }
    }

    private void syncPrimaryRoles() {
        for (User user : userMapper.selectList(null)) {
            List<String> roles = rbacService.getUserRoleCodes(user.getUserId(), user.getRole());
            if (roles.isEmpty()) {
                continue;
            }
            String primaryRole = roles.contains(user.getRole()) ? user.getRole() : roles.get(0);
            if (!Objects.equals(primaryRole, user.getRole())) {
                user.setRole(primaryRole);
                userMapper.updateById(user);
            }
        }
    }

    private User bootstrapSystemAdmin() {
        if (isBlank(systemAdminEmail) || isBlank(systemAdminPassword)) {
            return null;
        }

        String normalizedEmail = systemAdminEmail.trim().toLowerCase();
        User existing = userMapper.selectByEmail(normalizedEmail);
        if (existing == null) {
            User user = new User();
            user.setUsername(isBlank(systemAdminUsername) ? "system-admin" : systemAdminUsername.trim());
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(systemAdminPassword));
            user.setRole(RoleConstants.SYSTEM_ADMIN);
            user.setJoinDate(LocalDateTime.now());
            user.setTotalPoints(0);
            userMapper.insert(user);
            rbacService.ensureUserPrimaryRole(user.getUserId(), RoleConstants.SYSTEM_ADMIN);
            return userMapper.selectById(user.getUserId());
        }

        LinkedHashSet<String> roleCodes = new LinkedHashSet<>();
        roleCodes.add(RoleConstants.SYSTEM_ADMIN);
        roleCodes.addAll(rbacService.getUserRoleCodes(existing.getUserId(), existing.getRole()));
        rbacService.replaceUserRoles(existing.getUserId(), new ArrayList<>(roleCodes));
        return userMapper.selectById(existing.getUserId());
    }

    private void seedDefaultPointsRules(Integer systemAdminUserId) {
        if (systemAdminUserId == null) {
            return;
        }
        if (pointsRuleConfigMapper.selectCount(null) != 0) {
            return;
        }

        insertPointsRule("GENERAL", "0.00", "2.00", 10, 20, 15, systemAdminUserId);
        insertPointsRule("GENERAL", "2.01", "4.00", 20, 35, 30, systemAdminUserId);
        insertPointsRule("GENERAL", "4.01", "8.00", 40, 60, 50, systemAdminUserId);
    }

    private void insertPointsRule(
        String category,
        String minHours,
        String maxHours,
        int suggestedPoints,
        int maxPoints,
        int escalationThreshold,
        Integer userId
    ) {
        PointsRuleConfig rule = new PointsRuleConfig();
        rule.setActivityCategory(category);
        rule.setMinServiceHours(new BigDecimal(minHours));
        rule.setMaxServiceHours(new BigDecimal(maxHours));
        rule.setSuggestedPoints(suggestedPoints);
        rule.setMaxPoints(maxPoints);
        rule.setEscalationThreshold(escalationThreshold);
        rule.setStatus(1);
        rule.setCreatedBy(userId);
        rule.setUpdatedBy(userId);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        pointsRuleConfigMapper.insert(rule);
    }

    private void execute(String sql) {
        jdbcTemplate.execute(sql);
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(1)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
            Integer.class,
            tableName,
            columnName
        );
        if (count != null && count == 0) {
            execute(ddl);
        }
    }

    private void ensureIndex(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(1)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """,
            Integer.class,
            tableName,
            indexName
        );
        if (count != null && count == 0) {
            execute(ddl);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record RoleSeed(String code, String name, boolean systemRole, String dataScope, int sortOrder) {
    }

    private record PermissionSeed(String code, String name, String module, String httpMethod, String apiPattern) {
    }
}
