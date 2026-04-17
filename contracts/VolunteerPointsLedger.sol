pragma solidity ^0.6.10;

contract VolunteerPointsLedger {
    struct PointTransaction {
        uint256 userId;
        uint256 organizationId;
        int256 pointsDelta;
        string transactionType;
        string source;
        string referenceType;
        string referenceId;
        string digest;
        string createdAt;
        uint256 balanceAfter;
        bool exists;
    }

    mapping(uint256 => uint256) private balances;
    mapping(bytes32 => PointTransaction) private transactions;

    function creditPoints(
        string memory bizKey,
        uint256 userId,
        uint256 organizationId,
        uint256 points,
        string memory source,
        string memory referenceType,
        string memory referenceId,
        string memory digest,
        string memory createdAt
    ) public {
        require(points > 0, "points must be positive");
        bytes32 key = buildKey(bizKey);
        require(!transactions[key].exists, "biz key already exists");

        balances[userId] = balances[userId] + points;
        transactions[key] = PointTransaction(
            userId,
            organizationId,
            int256(points),
            "earned",
            source,
            referenceType,
            referenceId,
            digest,
            createdAt,
            balances[userId],
            true
        );
    }

    function debitPoints(
        string memory bizKey,
        uint256 userId,
        uint256 organizationId,
        uint256 points,
        string memory source,
        string memory referenceType,
        string memory referenceId,
        string memory digest,
        string memory createdAt
    ) public {
        require(points > 0, "points must be positive");
        require(balances[userId] >= points, "insufficient points");
        bytes32 key = buildKey(bizKey);
        require(!transactions[key].exists, "biz key already exists");

        balances[userId] = balances[userId] - points;
        transactions[key] = PointTransaction(
            userId,
            organizationId,
            -int256(points),
            "spent",
            source,
            referenceType,
            referenceId,
            digest,
            createdAt,
            balances[userId],
            true
        );
    }

    function refundPoints(
        string memory bizKey,
        uint256 userId,
        uint256 organizationId,
        uint256 points,
        string memory source,
        string memory referenceType,
        string memory referenceId,
        string memory digest,
        string memory createdAt
    ) public {
        require(points > 0, "points must be positive");
        bytes32 key = buildKey(bizKey);
        require(!transactions[key].exists, "biz key already exists");

        balances[userId] = balances[userId] + points;
        transactions[key] = PointTransaction(
            userId,
            organizationId,
            int256(points),
            "earned",
            source,
            referenceType,
            referenceId,
            digest,
            createdAt,
            balances[userId],
            true
        );
    }

    function getBalance(uint256 userId) public view returns (uint256) {
        return balances[userId];
    }

    function getTransaction(string memory bizKey)
        public
        view
        returns (
            uint256 userId,
            uint256 organizationId,
            int256 pointsDelta,
            string memory transactionType,
            string memory source,
            string memory referenceType,
            string memory referenceId,
            string memory digest,
            string memory createdAt,
            uint256 balanceAfter,
            string memory status
        )
    {
        PointTransaction memory item = transactions[buildKey(bizKey)];
        if (!item.exists) {
            return (0, 0, 0, "", "", "", "", "", "", 0, "missing");
        }
        return (
            item.userId,
            item.organizationId,
            item.pointsDelta,
            item.transactionType,
            item.source,
            item.referenceType,
            item.referenceId,
            item.digest,
            item.createdAt,
            item.balanceAfter,
            "stored"
        );
    }

    function buildKey(string memory bizKey) private pure returns (bytes32) {
        return keccak256(abi.encodePacked(bizKey));
    }
}
