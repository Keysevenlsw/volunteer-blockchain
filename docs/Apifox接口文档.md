# Apifox 接口文档

本文档配套的 OpenAPI 导入文件为 [apifox-openapi.json](/d:/codes/volunteer-blockchain/docs/apifox-openapi.json)。

## 一、导入方式

1. 打开 Apifox，进入项目。
2. 选择 `项目设置` -> `导入数据` -> `OpenAPI/Swagger`。
3. 选择本仓库文件 [apifox-openapi.json](/d:/codes/volunteer-blockchain/docs/apifox-openapi.json)。
4. 导入后创建环境 `本地联调`，设置变量 `baseUrl=http://localhost:8080`。
5. 对需要登录的接口，在请求头中配置 `Authorization: Bearer {{token}}`，或按角色分别使用 `{{systemToken}}`、`{{orgToken}}`、`{{volunteerToken}}`、`{{activityReviewerToken}}`、`{{productReviewerToken}}`。

## 二、接口分组

| 分组 | 主要接口 | 鉴权 |
| --- | --- | --- |
| 认证 | 注册、登录、当前用户 | 登录和当前用户需要 Bearer Token |
| 组织 | 组织列表、默认归属组织申请和审核 | 登录用户 |
| 活动 | 活动草稿、提审、报名、完成报告、积分流水 | 按角色鉴权 |
| 商城 | 商品草稿、提审、统一商城、兑换履约 | 按角色鉴权 |
| 平台审核 | 活动审核、商品审核、积分规则、用户角色 | 按权限鉴权 |
| 存证 | 登录用户存证查询、上链重试 | 登录用户 |
| 公共 | 首页信息、完成项目、公开存证 | 免登录 |
| 文件 | 附件上传 | 登录用户 |

## 三、角色与 Token 使用

| 角色 | 角色编码 | 建议 Token 变量 | 说明 |
| --- | --- | --- | --- |
| 志愿者 | `volunteer` | `volunteerToken` | 报名活动、提交完成报告、兑换商品 |
| 组织管理员 | `organization_admin` | `orgToken` | 创建活动/商品、审核完成报告、处理兑换 |
| 活动审核员 | `activity_reviewer` | `activityReviewerToken` | 审核组织提交的活动 |
| 商品审核员 | `product_reviewer` | `productReviewerToken` | 审核组织提交的商品 |
| 超级管理员 | `system_admin` | `systemToken` | 分配角色、维护积分规则、复审高风险活动 |

## 四、通用响应格式

所有接口统一返回：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

失败时：

```json
{
  "success": false,
  "message": "错误原因",
  "data": null
}
```

## 五、关键枚举

| 场景 | 字段 | 可用值 |
| --- | --- | --- |
| 活动发布审核 | `reviewStatus` | `draft`、`pending_review`、`approved`、`rejected` |
| 活动业务状态 | `status` | `pending`、`ongoing`、`completed` |
| 活动审核动作 | `status` | `approved`、`rejected`、`escalated` |
| 完成报告审核 | `status` | `pending`、`approved`、`rejected` |
| 商品审核状态 | `reviewStatus` | `draft`、`pending_review`、`approved`、`rejected`、`off_shelf` |
| 商品审核动作 | `status` | `approved`、`rejected`、`off_shelf` |
| 兑换状态 | `status` | `pending`、`fulfilled`、`cancelled` |
| 积分流水类型 | `transactionType` | `earned`、`spent` |
| 存证业务类型 | `bizType` | `completion` |

## 六、Apifox 后置脚本示例

登录志愿者后保存 Token：

```javascript
const body = pm.response.json();
if (body && body.success && body.data && body.data.token) {
  pm.environment.set("volunteerToken", body.data.token);
}
```

创建活动后保存活动 ID：

```javascript
const body = pm.response.json();
if (body && body.success && body.data && body.data.activityId) {
  pm.environment.set("activityId", body.data.activityId);
}
```

提交完成报告后保存完成报告 ID 和公开存证业务键：

```javascript
const body = pm.response.json();
if (body && body.success && body.data && body.data.completionId) {
  pm.environment.set("completionId", body.data.completionId);
  pm.environment.set("evidenceBizType", "completion");
  pm.environment.set("evidenceBizId", body.data.completionId);
}
```

创建商品后保存商品 ID：

```javascript
const body = pm.response.json();
if (body && body.success && body.data && body.data.productId) {
  pm.environment.set("productId", body.data.productId);
}
```

兑换商品后保存兑换记录 ID：

```javascript
const body = pm.response.json();
if (body && body.success && body.data && body.data.id) {
  pm.environment.set("redemptionId", body.data.id);
}
```

## 七、推荐联调顺序

1. 登录超级管理员。
2. 注册并登录组织管理员。
3. 注册并登录志愿者。
4. 注册两个审核员账号，超级管理员分配 `activity_reviewer` 和 `product_reviewer`。
5. 组织管理员创建活动并提交审核。
6. 活动审核员审核普通活动，或升级高风险活动给超级管理员复审。
7. 志愿者浏览全平台活动、报名、提交完成报告。
8. 组织管理员审核完成报告，触发积分发放和链上存证。
9. 组织管理员创建商品并提交审核。
10. 商品审核员审核商品，志愿者在统一商城兑换，组织管理员履约。
11. 公开接口查询全平台存证列表和存证详情。

完整请求体示例见 [接口联调用例.md](/d:/codes/volunteer-blockchain/docs/接口联调用例.md)。

## 八、链上积分接口补充

积分系统改为链上为准后，Apifox 需要补充以下接口：

| 接口 | 角色 | 说明 |
| --- | --- | --- |
| `GET /api/points/balance` | 志愿者 | 查询当前用户链上余额、数据库缓存余额和一致性结果 |
| `POST /api/admin/points/sync-balances` | 超级管理员 | 批量读取链上余额并修正 `users.total_points` 缓存 |
| `POST /api/admin/points/migrate` | 超级管理员 | 将历史积分初始化迁移到链上，按 `migration:init:{userId}` 幂等 |

`GET /api/points` 的积分流水返回字段增加：

```json
{
  "bizKey": "completion:1:earn:2",
  "digest": "sha256摘要",
  "txHash": "0x...",
  "blockNumber": "123",
  "contractName": "VolunteerPointsLedger",
  "onchainStatus": "success",
  "onchainAt": "2026-05-10 12:00:00",
  "chainBalanceAfter": 30
}
```

链上积分相关接口的联调顺序建议：

1. 部署 `VolunteerPointsLedger` 合约并配置 `webase.points-contract-address`。
2. 若已有历史积分，先用超级管理员执行 `POST /api/admin/points/migrate`。
3. 志愿者执行 `GET /api/points/balance` 检查链上余额与缓存是否一致。
4. 完成活动、兑换商品、取消兑换后，分别检查 `GET /api/points` 中的 `txHash` 和 `chainBalanceAfter`。
