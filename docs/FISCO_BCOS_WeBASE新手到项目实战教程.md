# FISCO BCOS 与 WeBASE 新手到项目实战教程

本文面向第一次接触 FISCO BCOS 和 WeBASE 的开发者，目标不是只跑一个 HelloWorld，而是最终能完成本系统的区块链部分：志愿服务完成报告存证、积分余额链上维护、积分交易链上记录、Spring Boot 通过 WeBASE 调用链上合约。

本文按本项目当前实现编写：

- 后端技术栈：Spring Boot + MySQL + FISCO BCOS + WeBASE-Front。
- 合约语言：Solidity `0.6.10`。
- 合约调用方式：后端通过 WeBASE-Front HTTP 接口 `/WeBASE-Front/trans/handle` 调用链上合约。
- 项目合约：`VolunteerEvidenceRegistry` 用于志愿服务存证，`VolunteerPointsLedger` 用于链上积分账本。
- 本文检查日期：2026-04-16。

官方资料建议同步查看：

- FISCO BCOS 官方介绍：https://fisco-bcos-documentation.readthedocs.io/en/latest/docs/introduction.html
- FISCO BCOS 建链脚本：https://fisco-bcos-documentation.readthedocs.io/en/latest/docs/manual/build_chain.html
- FISCO BCOS 控制台与合约部署：https://fisco-bcos-documentation.readthedocs.io/zh_CN/v2.2.0/en/docs/manual/console.html
- WeBASE 安装部署：https://webase-doc.readthedocs.io/en/latest/docs/WeBASE/install.html
- WeBASE-Front 接口说明：https://webase-doc.readthedocs.io/en/latest/docs/WeBASE-Front/interface.html

## 1. 先理解这两个东西是什么

### 1.1 FISCO BCOS 是什么

FISCO BCOS 是一个面向企业和机构场景的联盟链底层平台。你可以把它理解成“区块链网络本体”，它负责：

- 启动和维护多个区块链节点。
- 让节点之间达成共识。
- 保存区块、交易、合约状态。
- 部署和执行智能合约。
- 提供 SDK、控制台、RPC 等方式让业务系统访问链。

它和比特币、以太坊主网这种公链不同。FISCO BCOS 更适合“已知参与方共同维护数据可信账本”的场景，例如金融、政务、司法、公益、供应链。

本项目是志愿者服务认证系统，链上记录的是：

- 志愿者完成某次服务的存证摘要。
- 积分发放、消费、退款等积分交易。
- 志愿者链上积分余额。

### 1.2 WeBASE 是什么

WeBASE 是围绕 FISCO BCOS 的区块链中间件和管理平台。你可以把它理解成“开发和运维 FISCO BCOS 的工具箱”。

常见组件包括：

- WeBASE-Front：节点前置服务。它贴近区块链节点，对外提供 HTTP 接口，业务系统可以通过它调用合约。
- WeBASE-Node-Manager：节点管理服务。用于查看节点、区块、交易、合约等。
- WeBASE-Web：浏览器管理界面。用于可视化查看区块链状态、编译和部署合约。
- WeBASE-Sign：签名服务。生产环境建议使用，用于托管私钥并完成签名，降低私钥泄露风险。

本项目当前主要依赖 WeBASE-Front。Spring Boot 后端不是直接连 FISCO BCOS 节点，而是发送 HTTP 请求给 WeBASE-Front，再由 WeBASE-Front 调用 FISCO BCOS 链上合约。

调用链路如下：

```text
Vue 前端
  -> Spring Boot 后端
  -> WeBASE-Front HTTP 接口
  -> FISCO BCOS 节点
  -> Solidity 智能合约
  -> 区块链账本
```

### 1.3 本系统用的是联盟链还是私有链

结论：本系统的设计目标是联盟链。

原因如下：

- 志愿者服务认证涉及多个已知主体：平台方、志愿组织、审核方、监管或公示方。
- 这些主体不是完全匿名的公众用户，而是经过许可加入网络的机构或系统。
- 链上的价值在于“多个参与方共同维护可信记录”，而不是公开挖矿或代币投机。
- FISCO BCOS 本身就是面向联盟链和许可链场景设计的。

但要注意本地开发环境和生产环境的区别：

| 环境 | 实际形态 | 说明 |
|---|---|---|
| 本地学习环境 | 单机 4 节点测试链 | 节点都跑在你自己的机器上，严格说更像私有测试链，但用的是联盟链技术栈 |
| 学校毕设/演示环境 | 单服务器或虚拟机测试链 | 可以模拟联盟链，适合答辩和功能演示 |
| 真实生产环境 | 多机构多服务器联盟链 | 平台方、志愿组织、监管方可分别运行节点，共同维护账本 |

所以文档和系统设计中可以写：“系统基于 FISCO BCOS 联盟链实现；开发阶段采用单机多节点方式搭建联盟链测试环境。”

## 2. 新手必须掌握的基础概念

### 2.1 节点

节点就是运行 FISCO BCOS 程序的服务器进程。一个链至少需要多个节点来形成共识。开发时常见配置是单机 4 节点。

你可以把节点理解成多个共同记账的服务器。

### 2.2 群组 group

FISCO BCOS 2.x 有群组概念，常见默认群组是 `groupId=1`。不同群组可以理解成不同账本。

本项目默认配置：

```properties
webase.group-id=1
```

如果你的链不是 `groupId=1`，后端配置必须同步改。

### 2.3 智能合约

智能合约是部署在链上的程序。它决定哪些数据可以写链、如何修改状态、如何查询状态。

本项目有两个合约：

```text
contracts/VolunteerEvidenceRegistry.sol
contracts/VolunteerPointsLedger.sol
```

其中：

- `VolunteerEvidenceRegistry` 保存志愿服务完成报告的摘要和审核信息。
- `VolunteerPointsLedger` 保存积分余额和积分交易。

### 2.4 ABI

ABI 是合约接口描述文件。后端调用合约时，需要知道合约有哪些方法、每个方法有哪些参数、返回什么值。

本项目 ABI 文件在：

```text
src/main/resources/blockchain/VolunteerEvidenceRegistry.abi.json
src/main/resources/blockchain/VolunteerPointsLedger.abi.json
```

如果你修改了 Solidity 合约，就必须重新编译并更新 ABI，否则后端可能调用失败。

### 2.5 合约地址

合约部署到链上后，会得到一个地址，例如：

```text
0x1234567890abcdef...
```

后端必须配置正确的合约地址：

```properties
webase.contract-address=志愿服务存证合约地址
webase.points-contract-address=积分合约地址
```

### 2.6 用户地址

调用合约需要一个链上用户地址。开发环境可以用 WeBASE 创建测试用户地址。

后端配置项是：

```properties
webase.user-address=链上用户地址
```

生产环境不要把私钥随意放在业务系统里，建议使用 WeBASE-Sign 或专门的密钥管理服务。

### 2.7 交易和查询

合约调用分两类：

| 类型 | 是否改变链上状态 | 示例 |
|---|---:|---|
| 写交易 | 是 | 发放积分、扣减积分、保存存证 |
| 只读查询 | 否 | 查询积分余额、查询单条存证 |

本项目中：

- `saveEvidence` 是写交易。
- `creditPoints` 是写交易。
- `debitPoints` 是写交易。
- `refundPoints` 是写交易。
- `getBalance` 是只读查询。
- `getTransaction` 是只读查询。
- `getEvidence` 是只读查询。

## 3. 学习路线

建议按这个顺序学习，不要一开始就直接接项目。

| 阶段 | 目标 | 你应该会什么 |
|---|---|---|
| 第 1 阶段 | 理解概念 | 知道节点、群组、合约、ABI、合约地址、交易哈希是什么 |
| 第 2 阶段 | 搭建环境 | 能启动 FISCO BCOS 节点和 WeBASE-Front |
| 第 3 阶段 | 跑 HelloWorld | 能部署一个简单合约并调用 |
| 第 4 阶段 | 跑项目合约 | 能部署本系统两个合约并手动调用 |
| 第 5 阶段 | 接后端 | 能把合约地址、ABI、WeBASE 地址填入 Spring Boot |
| 第 6 阶段 | 完整联调 | 能完成存证上链、积分发放、扣减、退款、余额校验 |
| 第 7 阶段 | 排错和验收 | 能定位 ABI 错误、地址错误、WeBASE 未启动、链交易失败等问题 |

## 4. 环境准备

### 4.1 推荐环境

如果你是 Windows 用户，推荐使用以下方式之一：

| 方式 | 推荐度 | 说明 |
|---|---:|---|
| WSL2 Ubuntu | 高 | 最适合本地学习，命令和 Linux 基本一致 |
| Linux 虚拟机 | 高 | 稳定，适合毕设演示 |
| 云服务器 Ubuntu/CentOS | 高 | 更接近生产环境 |
| 原生 Windows | 低 | FISCO BCOS 和 WeBASE 的安装脚本主要面向 Linux，容易踩坑 |

建议使用 Ubuntu 20.04 或 22.04。

### 4.2 基础软件

在 Linux/WSL2 中安装基础依赖：

```bash
sudo apt update
sudo apt install -y curl wget git unzip openssl dos2unix python3 python3-pip lsof net-tools
```

检查 Java：

```bash
java -version
```

说明：

- 本项目 Spring Boot 后端使用 Java 17。
- WeBASE 旧版本文档可能要求 Java 8 或 Java 11。实际安装时以你使用的 WeBASE 版本官方说明为准。
- 如果本机同时需要 Java 8/11/17，建议用 `sdkman` 或系统 alternatives 管理。

### 4.3 端口规划

本地开发常见端口：

| 组件 | 默认端口示例 | 说明 |
|---|---:|---|
| FISCO BCOS P2P | 30300 起 | 节点之间通信 |
| FISCO BCOS Channel | 20200 起 | SDK/Front 通信 |
| FISCO BCOS RPC | 8545 起 | RPC 通信 |
| WeBASE-Front | 5002 | 后端调用合约用 |
| WeBASE-Web | 5000 或 5001 | 浏览器管理界面，取决于部署配置 |
| Spring Boot 后端 | 8080 | 本项目后端 |
| MySQL | 3306 | 业务数据库 |

如果端口被占用，用下面命令检查：

```bash
lsof -i:5002
lsof -i:8080
```

## 5. 搭建方式选择

你有两种搭建方式。

### 5.1 推荐方式：WeBASE 一键部署

新手建议先用 WeBASE 一键部署，因为它能一次性安装节点、WeBASE-Front、管理平台等组件。

大致流程：

```bash
mkdir -p ~/fisco-webase
cd ~/fisco-webase

wget https://osp-1257653870.cos.ap-guangzhou.myqcloud.com/WeBASE/releases/download/v1.5.3/webase-deploy.zip
unzip webase-deploy.zip
cd webase-deploy

python3 deploy.py installAll
python3 deploy.py status
```

如果下载地址或版本变化，以官方 WeBASE 安装文档为准。

注意事项：

- 不要用 `sudo python3 deploy.py installAll`，官方文档也明确提醒过这一点。`sudo` 可能导致脚本读不到当前用户的环境变量。
- 安装失败时先看 `log` 目录，不要盲目重复安装。
- 先保证 WeBASE-Front 启动成功，再接 Spring Boot。

### 5.2 进阶方式：手动搭 FISCO BCOS + 控制台

手动方式更适合理解底层。

典型步骤：

```bash
mkdir -p ~/fisco
cd ~/fisco

# 从官方文档复制 build_chain.sh 下载命令。
# 下载完成后，构建单机 4 节点链。
bash build_chain.sh -l 127.0.0.1:4

# 启动全部节点。
bash nodes/127.0.0.1/start_all.sh

# 查看节点进程。
ps -ef | grep fisco-bcos
```

如果你看到多个 `fisco-bcos` 进程，说明节点启动成功。

然后下载并配置 FISCO BCOS 控制台。控制台用于部署合约、调用合约、查看区块信息。

启动控制台后，执行：

```text
[group:1]> getBlockNumber
[group:1]> getTotalTransactionCount
```

能返回区块高度和交易数量，就说明链是可用的。

## 6. Demo 1：用控制台跑 HelloWorld

### 6.1 新建合约

在 FISCO BCOS 控制台目录下找到：

```text
contracts/solidity/
```

新建 `HelloVolunteer.sol`：

```solidity
pragma solidity ^0.6.10;

contract HelloVolunteer {
    string private message;

    constructor() public {
        message = "Hello, volunteer blockchain";
    }

    function setMessage(string memory newMessage) public {
        message = newMessage;
    }

    function getMessage() public view returns (string memory) {
        return message;
    }
}
```

### 6.2 部署合约

启动控制台：

```bash
bash start.sh
```

部署：

```text
[group:1]> deploy HelloVolunteer.sol
```

控制台会输出合约地址：

```text
contract address:0x你的合约地址
```

保存这个地址，后面调用要用。

### 6.3 调用合约

查询：

```text
[group:1]> call HelloVolunteer 0x你的合约地址 getMessage
```

修改：

```text
[group:1]> call HelloVolunteer 0x你的合约地址 setMessage "Hello, FISCO BCOS"
```

再次查询：

```text
[group:1]> call HelloVolunteer 0x你的合约地址 getMessage
```

如果能看到新字符串，说明你已经完成了合约部署和调用。

## 7. Demo 2：用 WeBASE-Front 调用 HelloWorld

控制台调用成功后，再练习通过 HTTP 调用合约，因为本项目后端就是这样做的。

### 7.1 准备参数

你需要准备：

```text
WeBASE-Front 地址，例如：http://127.0.0.1:5002
groupId，例如：1
链上用户地址，例如：0xabc...
合约地址，例如：0x123...
合约 ABI
方法名
方法参数
```

用户地址可以通过 WeBASE 管理页面创建，也可以按 WeBASE-Front 官方接口创建。新手建议用 WeBASE-Web 页面创建并复制地址。

### 7.2 HTTP 调用示例

以 `getMessage` 为例，请求体大致如下：

```json
{
  "groupId": 1,
  "user": "0x你的用户地址",
  "contractName": "HelloVolunteer",
  "contractAddress": "0x你的合约地址",
  "contractAbi": [
    {
      "constant": true,
      "inputs": [],
      "name": "getMessage",
      "outputs": [{"name": "", "type": "string"}],
      "payable": false,
      "stateMutability": "view",
      "type": "function"
    }
  ],
  "funcName": "getMessage",
  "funcParam": [],
  "useCns": false
}
```

请求地址：

```text
POST http://127.0.0.1:5002/WeBASE-Front/trans/handle
```

使用 curl：

```bash
curl -X POST "http://127.0.0.1:5002/WeBASE-Front/trans/handle" \
  -H "Content-Type: application/json" \
  -d @request.json
```

说明：

- 官方接口文档中 `contractName`、`contractAddress`、`contractAbi`、`funcName`、`funcParam`、`groupId`、`user` 是核心参数。
- 本项目后端会读取 `application.properties` 中的 WeBASE 配置和 ABI 文件，然后自动构造类似请求。
- 如果 HTTP 调用失败，先检查 WeBASE-Front 是否启动、合约地址是否正确、ABI 是否匹配、用户地址是否存在。

## 8. Demo 3：部署并练习本项目存证合约

项目合约路径：

```text
contracts/VolunteerEvidenceRegistry.sol
```

合约核心能力：

| 方法 | 类型 | 作用 |
|---|---|---|
| `saveEvidence` | 写交易 | 保存某个业务记录的摘要和审核信息 |
| `getEvidence` | 查询 | 按业务类型和业务 ID 查询存证 |

### 8.1 合约代码理解

核心结构：

```solidity
struct Evidence {
    string digest;
    string reviewer;
    string reviewedAt;
    string storagePath;
    bool exists;
}
```

这表示链上不保存完整完成报告正文，只保存摘要和关键审核信息。

为什么这样设计：

- 完成报告正文、图片路径、贡献详情等可能比较长，不适合全部上链。
- 链上保存 SHA-256 摘要，后续可以验证数据库内容是否被篡改。
- MySQL 保存可查询详情，区块链保存可信校验依据。

### 8.2 部署

将合约复制到控制台合约目录：

```bash
cp /d/codes/volunteer-blockchain/contracts/VolunteerEvidenceRegistry.sol ~/fisco/console/contracts/solidity/
```

如果你在 Linux 服务器上，按实际项目路径调整。

启动控制台：

```bash
cd ~/fisco/console
bash start.sh
```

部署：

```text
[group:1]> deploy VolunteerEvidenceRegistry.sol
```

记录输出的合约地址：

```text
VolunteerEvidenceRegistry address = 0x...
```

### 8.3 手动调用写入存证

调用：

```text
[group:1]> call VolunteerEvidenceRegistry 0x合约地址 saveEvidence "completion" "1001" "abc123digest" "审核员A" "2026-04-16 10:00:00" "/uploads/report-1001.png"
```

查询：

```text
[group:1]> call VolunteerEvidenceRegistry 0x合约地址 getEvidence "completion" "1001"
```

期望返回：

```text
abc123digest
审核员A
2026-04-16 10:00:00
/uploads/report-1001.png
stored
```

### 8.4 接入本项目后端

把合约地址填入：

```properties
webase.contract-name=VolunteerEvidenceRegistry
webase.contract-address=0x你的存证合约地址
webase.contract-abi=classpath:blockchain/VolunteerEvidenceRegistry.abi.json
```

后端对应代码：

```text
src/main/java/com/gzu/volunteerblockchain/service/impl/BlockchainServiceImpl.java
```

后端触发时机：

```text
组织管理员审核志愿者完成报告通过
  -> 后端生成完成报告摘要 digest
  -> 写入 blockchain_evidences 数据库索引
  -> 调用 VolunteerEvidenceRegistry.saveEvidence
  -> 回填 tx_hash、block_number、onchain_status
```

## 9. Demo 4：部署并练习本项目积分合约

项目合约路径：

```text
contracts/VolunteerPointsLedger.sol
```

合约核心能力：

| 方法 | 类型 | 作用 |
|---|---|---|
| `creditPoints` | 写交易 | 发放积分 |
| `debitPoints` | 写交易 | 消费积分 |
| `refundPoints` | 写交易 | 退回积分 |
| `getBalance` | 查询 | 查询用户链上余额 |
| `getTransaction` | 查询 | 查询单条积分交易 |

### 9.1 合约设计理解

积分合约维护两个核心状态：

```solidity
mapping(uint256 => uint256) private balances;
mapping(bytes32 => PointTransaction) private transactions;
```

含义：

- `balances[userId]` 是用户链上积分余额。
- `transactions[hash(bizKey)]` 是某笔业务积分交易。

每笔积分交易必须有一个唯一业务键 `bizKey`。

本项目约定：

```text
completion:{completionId}:earn:{userId}
redemption:{redemptionId}:spend:{userId}
redemption:{redemptionId}:refund:{userId}
migration:init:{userId}
```

为什么要有 `bizKey`：

- 防止重复发积分。
- 防止重复扣积分。
- 防止重复退款。
- 后端重试时可以识别同一笔业务。

### 9.2 部署

复制合约：

```bash
cp /d/codes/volunteer-blockchain/contracts/VolunteerPointsLedger.sol ~/fisco/console/contracts/solidity/
```

部署：

```text
[group:1]> deploy VolunteerPointsLedger.sol
```

记录输出的合约地址：

```text
VolunteerPointsLedger address = 0x...
```

### 9.3 手动调用发积分

给 `userId=1` 发 30 积分：

```text
[group:1]> call VolunteerPointsLedger 0x合约地址 creditPoints "completion:1001:earn:1" 1 10 30 "Activity completion reward" "completion" "1001" "digest001" "2026-04-16 10:00:00"
```

参数解释：

| 参数 | 示例 | 说明 |
|---|---|---|
| `bizKey` | `completion:1001:earn:1` | 幂等业务键 |
| `userId` | `1` | 志愿者用户 ID |
| `organizationId` | `10` | 活动所属组织 ID |
| `points` | `30` | 积分数量 |
| `source` | `Activity completion reward` | 来源说明 |
| `referenceType` | `completion` | 业务类型 |
| `referenceId` | `1001` | 业务 ID |
| `digest` | `digest001` | 交易摘要 |
| `createdAt` | `2026-04-16 10:00:00` | 创建时间 |

查询余额：

```text
[group:1]> call VolunteerPointsLedger 0x合约地址 getBalance 1
```

期望返回：

```text
30
```

重复执行同一个 `bizKey` 的发积分调用，应失败并提示类似：

```text
biz key already exists
```

这说明幂等控制生效。

### 9.4 手动调用消费积分

扣减 10 积分：

```text
[group:1]> call VolunteerPointsLedger 0x合约地址 debitPoints "redemption:2001:spend:1" 1 10 10 "Unified mall redemption" "redemption" "2001" "digest002" "2026-04-16 11:00:00"
```

查询余额：

```text
[group:1]> call VolunteerPointsLedger 0x合约地址 getBalance 1
```

期望余额：

```text
20
```

如果余额不足，合约会拒绝交易：

```text
insufficient points
```

### 9.5 手动调用退款

退回 10 积分：

```text
[group:1]> call VolunteerPointsLedger 0x合约地址 refundPoints "redemption:2001:refund:1" 1 10 10 "Redemption cancelled refund" "redemption" "2001" "digest003" "2026-04-16 12:00:00"
```

查询余额：

```text
[group:1]> call VolunteerPointsLedger 0x合约地址 getBalance 1
```

期望余额：

```text
30
```

### 9.6 查询单条积分交易

```text
[group:1]> call VolunteerPointsLedger 0x合约地址 getTransaction "completion:1001:earn:1"
```

期望返回交易详情，最后一个字段为：

```text
stored
```

查询不存在的交易：

```text
[group:1]> call VolunteerPointsLedger 0x合约地址 getTransaction "not-exists"
```

最后一个字段应为：

```text
missing
```

## 10. 把区块链接入本系统

### 10.1 你最终需要完成的东西

要让本系统区块链部分完整可用，你需要完成：

```text
1. FISCO BCOS 节点运行正常
2. WeBASE-Front 运行正常
3. 创建可调用合约的链上用户地址
4. 部署 VolunteerEvidenceRegistry 合约
5. 部署 VolunteerPointsLedger 合约
6. 获取两个合约地址
7. 确认项目 ABI 和链上合约一致
8. 填写 Spring Boot 的 webase 配置
9. 启动后端
10. 完成存证、发积分、扣积分、退款联调
```

### 10.2 后端配置

打开：

```text
src/main/resources/application.properties
```

配置：

```properties
webase.front-url=http://127.0.0.1:5002
webase.sign-url=
webase.group-id=1
webase.user-address=0x你的链上用户地址

webase.contract-name=VolunteerEvidenceRegistry
webase.contract-address=0x你的存证合约地址
webase.contract-abi=classpath:blockchain/VolunteerEvidenceRegistry.abi.json

webase.points-contract-name=VolunteerPointsLedger
webase.points-contract-address=0x你的积分合约地址
webase.points-contract-abi=classpath:blockchain/VolunteerPointsLedger.abi.json
```

如果 `webase.front-url`、`webase.user-address`、`webase.points-contract-address` 为空，积分发放、兑换扣分、取消兑换退款会直接失败。这是正确行为，因为本系统要求“链上成功后才更新数据库”。

### 10.3 后端区块链代码位置

存证服务：

```text
src/main/java/com/gzu/volunteerblockchain/service/impl/BlockchainServiceImpl.java
```

积分链服务：

```text
src/main/java/com/gzu/volunteerblockchain/service/impl/PointBlockchainServiceImpl.java
```

合约 ABI：

```text
src/main/resources/blockchain/VolunteerEvidenceRegistry.abi.json
src/main/resources/blockchain/VolunteerPointsLedger.abi.json
```

配置：

```text
src/main/resources/application.properties
```

数据库链上索引表：

```text
blockchain_evidences
points_records
```

### 10.4 启动后端

在项目根目录：

```bash
mvn spring-boot:run
```

或者：

```bash
mvn -DskipTests package
java -jar target/volunteer-blockchain-0.0.1-SNAPSHOT.jar
```

启动后先确认没有配置错误。

## 11. 完整联调流程

### 11.1 联调前检查

检查 WeBASE-Front：

```bash
curl http://127.0.0.1:5002/WeBASE-Front
```

不同版本返回可能不同，只要服务能连接即可。

检查 Spring Boot：

```bash
curl http://127.0.0.1:8080/api/public/info
```

检查数据库表：

```sql
SELECT COUNT(*) FROM blockchain_evidences;
SELECT COUNT(*) FROM points_records;
```

### 11.2 存证联调

业务流程：

```text
志愿者报名活动
  -> 志愿者提交完成报告
  -> 组织管理员审核通过
  -> 后端生成 digest
  -> 写 blockchain_evidences
  -> 调用 saveEvidence 上链
  -> 回填 tx_hash、block_number、onchain_status
```

验收 SQL：

```sql
SELECT id, biz_type, biz_id, digest, tx_hash, block_number, onchain_status, error_message
FROM blockchain_evidences
ORDER BY id DESC
LIMIT 5;
```

通过标准：

```text
onchain_status = success
tx_hash 不为空
digest 不为空
```

如果失败，后台可以调用重试接口：

```text
POST /api/evidences/{id}/retry
```

### 11.3 积分发放联调

业务流程：

```text
活动审核通过并设置 approved_reward_points
  -> 志愿者完成活动
  -> 组织管理员审核完成报告通过
  -> 后端调用 creditPoints
  -> 链上余额增加
  -> users.total_points 缓存更新
  -> points_records 写入 tx_hash、digest、chain_balance_after
```

验收 SQL：

```sql
SELECT record_id, user_id, points, transaction_type, reference_type, reference_id,
       biz_key, tx_hash, block_number, onchain_status, chain_balance_after
FROM points_records
ORDER BY record_id DESC
LIMIT 5;
```

通过标准：

```text
transaction_type = earned
biz_key 类似 completion:{completionId}:earn:{userId}
tx_hash 不为空
onchain_status = success
chain_balance_after 等于链上余额
users.total_points 等于链上余额缓存
```

查询当前用户余额接口：

```text
GET /api/points/balance
```

期望返回：

```json
{
  "chainBalance": 30,
  "cachedBalance": 30,
  "consistent": true
}
```

### 11.4 商品兑换扣积分联调

业务流程：

```text
志愿者兑换商品
  -> 后端创建兑换业务
  -> 调用 debitPoints
  -> 链上余额扣减
  -> 扣库存
  -> 更新 users.total_points 缓存
  -> 写 points_records 消费流水
```

通过标准：

```text
transaction_type = spent
biz_key 类似 redemption:{redemptionId}:spend:{userId}
链上余额减少
数据库缓存余额同步减少
库存减少
```

余额不足时，合约应拒绝交易：

```text
insufficient points
```

数据库不应产生成功兑换结果。

### 11.5 取消兑换退款联调

业务流程：

```text
组织管理员取消兑换
  -> 后端调用 refundPoints
  -> 链上余额增加
  -> 库存加回
  -> 兑换状态变为 cancelled
  -> 写退款积分流水
```

通过标准：

```text
transaction_type = earned
biz_key 类似 redemption:{redemptionId}:refund:{userId}
链上余额恢复
库存加回
兑换状态 cancelled
```

### 11.6 历史积分迁移联调

如果系统上线前数据库里已经有积分，需要迁移到链上。

接口：

```text
POST /api/admin/points/migrate
```

特点：

- 只有超级管理员可执行。
- 只迁移 `users.total_points > 0` 的用户。
- 使用 `migration:init:{userId}` 作为幂等业务键。
- 重复执行不会重复加积分。

迁移后检查：

```text
GET /api/points/balance
```

如果发现缓存和链上不一致，超级管理员可执行：

```text
POST /api/admin/points/sync-balances
```

该接口会以链上余额为准修正 `users.total_points` 缓存。

## 12. 项目区块链部分的最终验收清单

完成以下检查，基本可以认为本系统区块链部分已完成。

### 12.1 环境验收

- FISCO BCOS 节点正常运行。
- WeBASE-Front 正常运行。
- WeBASE-Web 能查看区块、交易、合约。
- Spring Boot 能访问 WeBASE-Front。
- `application.properties` 中的 `groupId`、`userAddress`、两个合约地址正确。

### 12.2 合约验收

- `VolunteerEvidenceRegistry` 已部署。
- `VolunteerPointsLedger` 已部署。
- 项目 ABI 与已部署合约一致。
- 控制台能手动调用 `saveEvidence` 和 `getEvidence`。
- 控制台能手动调用 `creditPoints`、`debitPoints`、`refundPoints`、`getBalance`、`getTransaction`。
- 重复 `bizKey` 会被拒绝。
- 积分余额不足时扣减会失败。

### 12.3 后端验收

- 完成报告审核通过后，`blockchain_evidences.onchain_status=success`。
- 完成报告审核通过后，链上积分余额增加。
- 积分发放值等于活动的 `approved_reward_points`，不是 `requested_reward_points`。
- 兑换商品后，链上积分余额减少。
- 取消兑换后，链上积分余额恢复。
- `points_records` 中有 `biz_key`、`digest`、`tx_hash`、`block_number`、`chain_balance_after`。
- `GET /api/points/balance` 能返回链上余额和数据库缓存余额是否一致。
- WeBASE 未配置或链调用失败时，数据库积分不应被当作成功更新。

## 13. 常见错误和排查

### 13.1 WeBASE 配置不完整

错误表现：

```text
WeBASE points contract config is incomplete
```

检查：

```properties
webase.front-url=
webase.user-address=
webase.points-contract-address=
```

这些不能留空。

### 13.2 合约地址错误

表现：

```text
contract not found
method not found
execution reverted
```

处理：

- 确认地址来自当前 `groupId`。
- 确认地址对应的是正确合约，不要把存证合约地址填到积分合约配置。
- 用控制台手动调用验证。

### 13.3 ABI 不匹配

表现：

```text
method not found
decode failed
parameter type mismatch
```

处理：

- 重新编译 Solidity 合约。
- 更新 `src/main/resources/blockchain/*.abi.json`。
- 重启 Spring Boot。

### 13.4 groupId 错误

表现：

```text
合约地址存在，但调用一直失败
```

处理：

- 确认合约部署在哪个群组。
- 修改：

```properties
webase.group-id=1
```

### 13.5 用户地址错误

表现：

```text
user not found
private key not found
sign failed
```

处理：

- 在 WeBASE 中创建或导入用户。
- 确认 `webase.user-address` 是可用地址。
- 生产环境使用 WeBASE-Sign，不要把私钥明文散落在服务器。

### 13.6 重复发积分

表现：

```text
biz key already exists
```

这通常不是坏事，而是幂等保护生效。

处理：

- 如果是同一业务重复提交，不应该再次发积分。
- 如果确实是新业务，检查 `bizKey` 生成逻辑是否错误。

### 13.7 积分不足

表现：

```text
insufficient points
```

处理：

- 查询链上余额 `getBalance(userId)`。
- 查询接口 `GET /api/points/balance`。
- 如果数据库缓存和链上不一致，用超级管理员执行同步接口。

### 13.8 链上成功但数据库失败

区块链交易一旦成功不能回滚，MySQL 事务可以回滚，这是区块链系统必须面对的问题。

本项目当前策略：

- 正常业务中先链上成功，再更新数据库。
- 数据库保留 `points_records` 和 `blockchain_evidences` 作为链上索引。
- 管理端提供余额同步和存证重试能力。

生产系统建议进一步增强：

- 增加链上交易结果扫描任务。
- 增加失败补偿队列。
- 对每笔业务使用唯一 `bizKey`。
- 所有链上写交易都设计为幂等。

## 14. 你需要真正理解的设计原则

### 14.1 链上不适合保存所有数据

链上适合保存：

- 关键字段。
- 摘要。
- 状态。
- 不可篡改的交易记录。
- 可验证的业务引用。

链上不适合保存：

- 大文本。
- 大图片。
- 隐私信息。
- 高频复杂查询需要的数据。

所以本项目采用：

```text
MySQL 保存业务详情和查询索引
FISCO BCOS 保存摘要、积分余额和积分交易
```

### 14.2 积分余额以链上为准

本项目改造后：

```text
链上 balances[userId] 是权威积分余额
users.total_points 只是数据库缓存
points_records 是链上积分交易索引
```

不要再通过 SQL 直接给用户加减积分。

### 14.3 所有积分写交易必须幂等

必须保证同一业务不会重复上链。

正确示例：

```text
completion:1001:earn:1
redemption:2001:spend:1
redemption:2001:refund:1
```

错误示例：

```text
earn:1
spend:1
random:timestamp
```

错误示例的问题是无法准确对应业务，也不能稳定防重。

### 14.4 审核通过不等于一定发积分成功

如果链上发积分失败，业务不能假装成功。

正确流程：

```text
审核通过
  -> 调用 creditPoints
  -> 链上成功
  -> 更新完成报告状态
  -> 更新积分缓存
  -> 写积分流水
```

如果链失败：

```text
数据库不应发积分
应提示链上调用失败
管理员后续排查 WeBASE、合约、余额、ABI、地址等问题
```

## 15. 建议的练手任务

按顺序完成这些任务，你就能真正掌握本系统区块链部分。

### 任务 1：搭建链

目标：

```text
启动 FISCO BCOS 单机 4 节点
启动 WeBASE-Front
能看到区块高度
```

验收：

```text
getBlockNumber 能返回数字
WeBASE 页面能看到节点
```

### 任务 2：部署 HelloVolunteer

目标：

```text
部署 HelloVolunteer.sol
调用 setMessage/getMessage
```

验收：

```text
getMessage 返回你设置的新内容
```

### 任务 3：通过 WeBASE-Front 调用 HelloVolunteer

目标：

```text
不用控制台，直接用 curl 调用合约
```

验收：

```text
HTTP 响应成功
返回合约方法结果或交易哈希
```

### 任务 4：部署存证合约

目标：

```text
部署 VolunteerEvidenceRegistry
调用 saveEvidence/getEvidence
```

验收：

```text
getEvidence 返回 stored
```

### 任务 5：部署积分合约

目标：

```text
部署 VolunteerPointsLedger
调用 creditPoints/debitPoints/refundPoints/getBalance/getTransaction
```

验收：

```text
余额能正确增加、减少、恢复
重复 bizKey 被拒绝
余额不足扣减失败
```

### 任务 6：接入 Spring Boot

目标：

```text
填好 application.properties
启动后端
完成一次存证上链
完成一次积分发放
```

验收：

```text
blockchain_evidences.onchain_status = success
points_records.onchain_status = success
GET /api/points/balance 返回 consistent = true
```

### 任务 7：测试异常场景

目标：

```text
故意填错合约地址
故意填错 ABI
故意重复提交同一 bizKey
故意用余额不足的用户兑换商品
```

验收：

```text
你能判断错误原因，并知道应该改哪里
```

## 16. 最终项目区块链实现说明

本系统最终区块链部分由以下模块组成：

```text
FISCO BCOS 联盟链
  -> 保存合约状态、交易、区块

WeBASE-Front
  -> 提供 HTTP 合约调用接口

VolunteerEvidenceRegistry
  -> 保存志愿服务完成报告存证摘要

VolunteerPointsLedger
  -> 保存积分余额和积分交易

Spring Boot BlockchainServiceImpl
  -> 完成报告存证上链

Spring Boot PointBlockchainServiceImpl
  -> 积分发放、扣减、退款、余额查询

MySQL blockchain_evidences
  -> 链上存证索引和交易哈希缓存

MySQL points_records
  -> 链上积分交易索引和交易哈希缓存

MySQL users.total_points
  -> 链上积分余额缓存，不是权威余额
```

一条完成报告上链路径：

```text
完成报告审核通过
  -> 生成 digest
  -> blockchain_evidences 插入 pending/success 记录
  -> WeBASE-Front 调用 saveEvidence
  -> FISCO BCOS 写入交易
  -> 回填 tx_hash/block_number/onchain_status
  -> 公开存证页面可查询
```

一条积分发放路径：

```text
完成报告审核通过
  -> 读取 activities.approved_reward_points
  -> 构造 bizKey = completion:{completionId}:earn:{userId}
  -> WeBASE-Front 调用 creditPoints
  -> FISCO BCOS 修改 balances[userId]
  -> 写入链上交易 transactions[hash(bizKey)]
  -> 更新 users.total_points 缓存
  -> 写 points_records 链上索引
```

一条积分兑换路径：

```text
志愿者兑换商品
  -> 构造 bizKey = redemption:{redemptionId}:spend:{userId}
  -> WeBASE-Front 调用 debitPoints
  -> 链上余额足够则扣减
  -> 更新库存和兑换记录
  -> 更新 users.total_points 缓存
  -> 写 points_records 消费索引
```

一条取消兑换退款路径：

```text
组织管理员取消兑换
  -> 构造 bizKey = redemption:{redemptionId}:refund:{userId}
  -> WeBASE-Front 调用 refundPoints
  -> 链上余额增加
  -> 兑换状态改为 cancelled
  -> 库存加回
  -> 更新 users.total_points 缓存
  -> 写 points_records 退款索引
```

到这里，你已经完成了从 FISCO BCOS、WeBASE 基础概念，到合约部署、手动调用、HTTP 调用、后端接入、业务联调、异常排查的完整路径。

