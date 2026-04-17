package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.entity.Activity;
import com.gzu.volunteerblockchain.entity.ActivityCompletion;
import com.gzu.volunteerblockchain.entity.BlockchainEvidence;
import com.gzu.volunteerblockchain.entity.User;

public interface BlockchainService {

    String buildDigest(ActivityCompletion completion, Activity activity, User volunteer);

    void submitEvidenceAsync(Integer evidenceId);

    BlockchainEvidence retryEvidence(Integer evidenceId);
}
