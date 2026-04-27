pragma solidity ^0.6.10;

contract VolunteerPointsLedger {
    uint256 private constant MAX_INT256 = 2**255 - 1;

    struct PointTransaction {
        uint256 userId;
        uint256 organizationId;
        int256 pointsDelta;
        uint8 transactionCode;
        bytes32 sourceHash;
        bytes32 referenceTypeHash;
        bytes32 referenceIdHash;
        bytes32 digestHash;
        bytes32 createdAtHash;
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
        require(bytes(bizKey).length > 0, "bizKey is required");
        require(userId > 0, "userId is required");
        require(points > 0, "points must be positive");
        require(points <= MAX_INT256, "points overflow");

        bytes32 key = buildKey(bizKey);
        require(!transactions[key].exists, "biz key already exists");

        balances[userId] = balances[userId] + points;
        transactions[key] = PointTransaction(
            userId,
            organizationId,
            int256(points),
            1,
            hashText(source),
            hashText(referenceType),
            hashText(referenceId),
            hashText(digest),
            hashText(createdAt),
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
        require(bytes(bizKey).length > 0, "bizKey is required");
        require(userId > 0, "userId is required");
        require(points > 0, "points must be positive");
        require(points <= MAX_INT256, "points overflow");
        require(balances[userId] >= points, "insufficient points");

        bytes32 key = buildKey(bizKey);
        require(!transactions[key].exists, "biz key already exists");

        balances[userId] = balances[userId] - points;
        transactions[key] = PointTransaction(
            userId,
            organizationId,
            -int256(points),
            2,
            hashText(source),
            hashText(referenceType),
            hashText(referenceId),
            hashText(digest),
            hashText(createdAt),
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
        require(bytes(bizKey).length > 0, "bizKey is required");
        require(userId > 0, "userId is required");
        require(points > 0, "points must be positive");
        require(points <= MAX_INT256, "points overflow");

        bytes32 key = buildKey(bizKey);
        require(!transactions[key].exists, "biz key already exists");

        balances[userId] = balances[userId] + points;
        transactions[key] = PointTransaction(
            userId,
            organizationId,
            int256(points),
            3,
            hashText(source),
            hashText(referenceType),
            hashText(referenceId),
            hashText(digest),
            hashText(createdAt),
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
            int256 pointsDelta,
            uint256 balanceAfter,
            bool exists
        )
    {
        PointTransaction memory item = transactions[buildKey(bizKey)];
        if (!item.exists) {
            return (0, 0, 0, false);
        }
        return (item.userId, item.pointsDelta, item.balanceAfter, true);
    }

    function buildKey(string memory bizKey) private pure returns (bytes32) {
        return keccak256(abi.encodePacked(bizKey));
    }

    function hashText(string memory value) private pure returns (bytes32) {
        return keccak256(abi.encodePacked(value));
    }
}
