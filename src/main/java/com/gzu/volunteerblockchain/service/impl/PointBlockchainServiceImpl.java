package com.gzu.volunteerblockchain.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.service.PointBlockchainService;
import java.io.IOException;
import java.math.BigInteger;
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
import org.springframework.stereotype.Service;

@Service
public class PointBlockchainServiceImpl implements PointBlockchainService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TYPE_EARNED = "earned";
    private static final String TYPE_SPENT = "spent";
    private static final String TYPE_REFUND = "refund";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${webase.front-url:}")
    private String webaseFrontUrl;

    @Value("${webase.group-id:group0}")
    private String groupId;

    @Value("${webase.user-address:}")
    private String userAddress;

    @Value("${webase.sign-user-id:}")
    private String signUserId;

    @Value("${webase.points-contract-address:}")
    private String pointsContractAddress;

    @Value("${webase.points-contract-name:VolunteerPointsLedger}")
    private String pointsContractName;

    @Value("${webase.points-contract-abi:classpath:blockchain/VolunteerPointsLedger.abi.json}")
    private Resource pointsContractAbiResource;

    public PointBlockchainServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PointChainResult creditPoints(PointChainRequest request) {
        return writePoints("creditPoints", TYPE_EARNED, request);
    }

    @Override
    public PointChainResult debitPoints(PointChainRequest request) {
        return writePoints("debitPoints", TYPE_SPENT, request);
    }

    @Override
    public PointChainResult refundPoints(PointChainRequest request) {
        return writePoints("refundPoints", TYPE_REFUND, request);
    }

    @Override
    public Integer getBalance(Integer userId) {
        validateUserId(userId);
        Object data = invokeContract("getBalance", List.of(userId), false);
        Integer balance = extractInteger(data);
        if (balance == null) {
            throw new BusinessException("Unable to parse chain points balance");
        }
        return balance;
    }

    @Override
    public PointChainTransaction getTransaction(String bizKey) {
        String normalizedBizKey = trimRequired(bizKey, "bizKey is required");
        Object data = invokeContract("getTransaction", List.of(normalizedBizKey), false);
        List<Object> values = flattenValues(data);
        if (values.size() < 4) {
            return new PointChainTransaction(false, normalizedBizKey, null, null, null);
        }
        Boolean exists = parseBoolean(values.get(3));
        if (!Boolean.TRUE.equals(exists)) {
            return new PointChainTransaction(false, normalizedBizKey, null, null, null);
        }
        return new PointChainTransaction(
            true,
            normalizedBizKey,
            parseInteger(values.get(0)),
            parseInteger(values.get(1)),
            parseInteger(values.get(2))
        );
    }

    private PointChainResult writePoints(String functionName, String transactionType, PointChainRequest request) {
        validateRequest(request);
        LocalDateTime now = LocalDateTime.now();
        String digest = buildDigest(request, transactionType, now);
        Object data = invokeContract(functionName, List.of(
            request.bizKey(),
            request.userId(),
            request.organizationId() == null ? 0 : request.organizationId(),
            request.points(),
            safe(request.source()),
            safe(request.referenceType()),
            request.referenceId() == null ? "" : String.valueOf(request.referenceId()),
            digest,
            format(now)
        ), true);
        String txHash = findString(data, "transactionHash", "txHash", "hash");
        String blockNumber = findString(data, "blockNumber", "blockNum", "blockHeight");
        Integer balanceAfter = getBalance(request.userId());
        return new PointChainResult(
            request.bizKey(),
            transactionType,
            digest,
            txHash,
            blockNumber,
            pointsContractName,
            balanceAfter,
            LocalDateTime.now()
        );
    }

    private Object invokeContract(String functionName, List<Object> functionParams, boolean writeTransaction) {
        requireChainConfig();
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("groupId", groupId);
            requestBody.put("contractAddress", pointsContractAddress);
            requestBody.put("contractAbi", readContractAbi());
            requestBody.put("funcName", functionName);
            requestBody.put("funcParam", functionParams);
            if (writeTransaction && !isBlank(signUserId)) {
                requestBody.put("signUserId", signUserId);
            } else {
                requestBody.put("user", userAddress);
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(webaseFrontUrl) + (writeTransaction && !isBlank(signUserId)
                    ? "/WeBASE-Front/trans/handleWithSign"
                    : "/WeBASE-Front/trans/handle")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Object payload = objectMapper.readValue(response.body(), Object.class);
            if (payload instanceof Map<?, ?> map) {
                Object code = map.get("code");
                if (!isSuccessCode(code)) {
                    Object message = map.containsKey("message") ? map.get("message") : map.get("errorMessage");
                    throw new BusinessException(String.valueOf(message == null ? "WeBASE points contract call failed" : message));
                }
                return map.containsKey("data") ? map.get("data") : map;
            }
            return payload;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException("Points chain call failed: " + ex.getMessage());
        }
    }

    private String buildDigest(PointChainRequest request, String transactionType, LocalDateTime createdAt) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("bizKey", request.bizKey());
            payload.put("userId", request.userId());
            payload.put("organizationId", request.organizationId() == null ? 0 : request.organizationId());
            payload.put("points", request.points());
            payload.put("transactionType", transactionType);
            payload.put("source", safe(request.source()));
            payload.put("referenceType", safe(request.referenceType()));
            payload.put("referenceId", request.referenceId());
            payload.put("createdAt", format(createdAt));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException("Unable to build points transaction digest: " + ex.getMessage());
        }
    }

    private void validateRequest(PointChainRequest request) {
        if (request == null) {
            throw new BusinessException("Points chain request is required");
        }
        trimRequired(request.bizKey(), "bizKey is required");
        validateUserId(request.userId());
        if (request.points() == null || request.points() <= 0) {
            throw new BusinessException("points must be positive");
        }
    }

    private void validateUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("userId is required");
        }
    }

    private void requireChainConfig() {
        if (isBlank(webaseFrontUrl) || isBlank(userAddress) || isBlank(pointsContractAddress)) {
            throw new BusinessException("WeBASE points contract config is incomplete");
        }
    }

    private Object readContractAbi() throws IOException {
        String abiJson = new String(pointsContractAbiResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return objectMapper.readValue(abiJson, Object.class);
    }

    private String findString(Object value, String... keys) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            for (String key : keys) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (key.equalsIgnoreCase(String.valueOf(entry.getKey())) && entry.getValue() != null) {
                        return valueAsString(entry.getValue());
                    }
                }
            }
            for (Object child : map.values()) {
                String found = findString(child, keys);
                if (found != null) {
                    return found;
                }
            }
        }
        if (value instanceof List<?> list) {
            for (Object child : list) {
                String found = findString(child, keys);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Integer extractInteger(Object value) {
        if (value == null) {
            return null;
        }
        Integer parsed = parseInteger(value);
        if (parsed != null) {
            return parsed;
        }
        if (value instanceof Map<?, ?> map) {
            for (String key : List.of("value", "result", "output", "data")) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (key.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                        Integer found = extractInteger(entry.getValue());
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }
            for (Object child : map.values()) {
                Integer found = extractInteger(child);
                if (found != null) {
                    return found;
                }
            }
        }
        if (value instanceof List<?> list) {
            for (Object child : list) {
                Integer found = extractInteger(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private List<Object> flattenValues(Object value) {
        List<Object> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            result.addAll(list);
            return result;
        }
        if (value instanceof Map<?, ?> map) {
            Object data = map.get("data");
            if (data != null) {
                return flattenValues(data);
            }
            Object resultValue = map.get("result");
            if (resultValue != null) {
                return flattenValues(resultValue);
            }
            result.addAll(map.values());
            return result;
        }
        if (value != null) {
            result.add(value);
        }
        return result;
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                    return new BigInteger(trimmed.substring(2), 16).intValue();
                }
                return new BigInteger(trimmed).intValue();
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Boolean parseBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            if ("true".equalsIgnoreCase(trimmed) || "1".equals(trimmed)) {
                return true;
            }
            if ("false".equalsIgnoreCase(trimmed) || "0".equals(trimmed)) {
                return false;
            }
        }
        return null;
    }

    private boolean isSuccessCode(Object code) {
        if (code == null) {
            return true;
        }
        String codeText = String.valueOf(code);
        return "0".equals(codeText) || "200".equals(codeText);
    }

    private String trimRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME_FORMATTER);
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }
}
