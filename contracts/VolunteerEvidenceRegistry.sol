pragma solidity ^0.6.10;

contract VolunteerEvidenceRegistry {
    struct Evidence {
        string digest;
        string reviewer;
        string reviewedAt;
        string storagePath;
        bool exists;
    }

    mapping(bytes32 => Evidence) private evidences;

    function saveEvidence(
        string memory bizType,
        string memory bizId,
        string memory digest,
        string memory reviewer,
        string memory reviewedAt,
        string memory storagePath
    ) public {
        bytes32 key = keccak256(abi.encodePacked(bizType, ":", bizId));
        evidences[key] = Evidence(digest, reviewer, reviewedAt, storagePath, true);
    }

    function getEvidence(
        string memory bizType,
        string memory bizId
    )
        public
        view
        returns (
            string memory digest,
            string memory reviewer,
            string memory reviewedAt,
            string memory storagePath,
            string memory status
        )
    {
        bytes32 key = keccak256(abi.encodePacked(bizType, ":", bizId));
        Evidence memory evidence = evidences[key];
        if (!evidence.exists) {
            return ("", "", "", "", "missing");
        }
        return (evidence.digest, evidence.reviewer, evidence.reviewedAt, evidence.storagePath, "stored");
    }
}
