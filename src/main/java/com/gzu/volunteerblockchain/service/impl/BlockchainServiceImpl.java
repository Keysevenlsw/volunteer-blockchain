package com.gzu.volunteerblockchain.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzu.volunteerblockchain.entity.Activity;
import com.gzu.volunteerblockchain.entity.ActivityCompletion;
import com.gzu.volunteerblockchain.entity.BlockchainEvidence;
import com.gzu.volunteerblockchain.entity.User;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.mapper.BlockchainEvidenceMapper;
import com.gzu.volunteerblockchain.service.BlockchainService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BlockchainServiceImpl implements BlockchainService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BlockchainEvidenceMapper blockchainEvidenceMapper;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${webase.front-url:}")
    private String webaseFrontUrl;

    @Value("${webase.group-id:group0}")
    private String groupId;

    @Value("${webase.user-address:}")
    private String userAddress;

    @Value("${webase.contract-address:}")
    private String contractAddress;

    @Value("${webase.contract-name:VolunteerEvidenceRegistry}")
    private String contractName;

    @Value("${webase.contract-abi:classpath:blockchain/VolunteerEvidenceRegistry.abi.json}")
    private Resource contractAbiResource;

    public BlockchainServiceImpl(BlockchainEvidenceMapper blockchainEvidenceMapper, ObjectMapper objectMapper) {
        this.blockchainEvidenceMapper = blockchainEvidenceMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String buildDigest(ActivityCompletion completion, Activity activity, User volunteer) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("completionId", completion.getCompletionId());
            payload.put("activityId", activity.getActivityId());
            payload.put("activityName", activity.getActivityName());
            payload.put("organizationId", activity.getOrganizationId());
            payload.put("volunteerId", volunteer.getUserId());
            payload.put("volunteerName", volunteer.getUsername());
            payload.put("serviceLocation", completion.getServiceLocation());
            payload.put("serviceStartTime", format(completion.getServiceStartTime()));
            payload.put("serviceEndTime", format(completion.getServiceEndTime()));
            payload.put("reportText", completion.getReportText());
            payload.put("contributionDetails", completion.getContributionDetails());
            payload.put("attachmentPaths", normalizeAttachmentPaths(completion.getAttachmentPaths()));
            String json = objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception ex) {
            throw new BusinessException("生成链上摘要失败: " + ex.getMessage());
        }
    }

    @Override
    @Async
    public void submitEvidenceAsync(Integer evidenceId) {
        retryEvidence(evidenceId);
    }

    @Override
    public BlockchainEvidence retryEvidence(Integer evidenceId) {
        BlockchainEvidence evidence = blockchainEvidenceMapper.selectById(evidenceId);
        if (evidence == null) {
            throw new BusinessException("链上存证记录不存在");
        }

        if (isBlank(webaseFrontUrl) || isBlank(userAddress) || isBlank(contractAddress)) {
            markFailed(evidence, "WeBASE 配置不完整，无法提交链上存证");
            return evidence;
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("groupId", groupId);
            requestBody.put("user", userAddress);
            requestBody.put("contractAddress", contractAddress);
            requestBody.put("contractAbi", readContractAbi());
            requestBody.put("funcName", "saveEvidence");
            requestBody.put("funcParam", List.of(
                evidence.getBizType(),
                String.valueOf(evidence.getBizId()),
                evidence.getDigest(),
                safe(evidence.getReviewerName()),
                format(evidence.getReviewedAt()),
                safe(evidence.getStoragePath())
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(webaseFrontUrl) + "/WeBASE-Front/trans/handle"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Object payload = objectMapper.readValue(response.body(), Object.class);
            if (!(payload instanceof Map<?, ?> payloadMap)) {
                markFailed(evidence, "Unexpected WeBASE response");
                return evidence;
            }
            Object code = payloadMap.get("code");
            if (!isSuccessCode(code)) {
                Object message = payloadMap.containsKey("message") ? payloadMap.get("message") : payloadMap.get("errorMessage");
                markFailed(evidence, String.valueOf(message == null ? "WeBASE call failed" : message));
                return evidence;
            }

            Map<String, Object> data = null;
            Object dataObject = payloadMap.get("data");
            if (dataObject instanceof Map<?, ?> rawMap) {
                data = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    data.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }

            evidence.setOnchainStatus("success");
            evidence.setTxHash(readValue(data, "transactionHash", "txHash"));
            evidence.setBlockNumber(readValue(data, "blockNumber", "blockNum"));
            evidence.setErrorMessage(null);
            evidence.setOnchainAt(LocalDateTime.now());
            blockchainEvidenceMapper.updateById(evidence);
            return evidence;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            markFailed(evidence, "链上存证失败: " + ex.getMessage());
            return evidence;
        }
    }

    private List<String> normalizeAttachmentPaths(String rawAttachmentPaths) throws IOException {
        if (isBlank(rawAttachmentPaths)) {
            return List.of();
        }
        List<String> paths = objectMapper.readValue(rawAttachmentPaths, new TypeReference<List<String>>() {
        });
        List<String> normalized = new ArrayList<>(paths);
        normalized.sort(String::compareTo);
        return normalized;
    }

    private void markFailed(BlockchainEvidence evidence, String errorMessage) {
        evidence.setOnchainStatus("failed");
        evidence.setErrorMessage(errorMessage);
        blockchainEvidenceMapper.updateById(evidence);
    }

    private Object readContractAbi() throws IOException {
        String abiJson = new String(contractAbiResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return objectMapper.readValue(abiJson, Object.class);
    }

    private String readValue(Map<String, Object> data, String primaryKey, String fallbackKey) {
        if (data == null) {
            return null;
        }
        Object primary = data.get(primaryKey);
        if (primary != null) {
            return primary.toString();
        }
        Object fallback = data.get(fallbackKey);
        return fallback == null ? null : fallback.toString();
    }

    private boolean isSuccessCode(Object code) {
        if (code == null) {
            return true;
        }
        String codeText = String.valueOf(code);
        return "0".equals(codeText) || "200".equals(codeText);
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME_FORMATTER);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }
}
