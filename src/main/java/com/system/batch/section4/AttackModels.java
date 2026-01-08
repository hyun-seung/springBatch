package com.system.batch.section4;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

public class AttackModels {

    @Data
    public static class AttackLog {
        private long id;
        private LocalDateTime timestamp;
        private String targetIp;
        private AttackType attackType;
        private String payload;

        public void setAttackType(String attackTypeStr) {
            this.attackType = AttackType.fromString(attackTypeStr);
        }
    }

    @RequiredArgsConstructor
    public enum AttackType {
        SQL_INJECTION("SQL Injection"),
        XSS("Cross-Site Scripting (XSS)"),
        DDOS("DDoS"),
        BRUTE_FORCE("Brute Force"),
        UNKNOWN("Unknown");

        private final String displayName;

        @JsonValue
        public String getDisplayName() {
            return displayName;
        }

        public static AttackType fromString(String attackType) {
            if (attackType == null) return UNKNOWN;

            return switch (attackType.toLowerCase()) {
                case "sql injections" -> SQL_INJECTION;
                case "cross-site scripting (xss)", "xss" -> XSS;
                case "ddos" -> DDOS;
                case "brute force" -> BRUTE_FORCE;
                default -> UNKNOWN;
            };
        }
    }

    @Builder
    @Getter
    public static class AttackAnalysisResult {
        private static final ObjectMapper PRETTY_MAPPER = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        private int totalAttacks;
        private Map<AttackType, Integer> attackTypeCount;
        private Map<AttackType, String> attackTypePercentage;
        private Map<String, Integer> ipAttackCount;
        private Map<String, Integer> timeSlotCount;
        private String mostDangerousIp;
        private String peakHour;
        private String threatLevel;

        @Override
        public String toString() {
            try {
                return """
                        ======= 공격 분석 결과 =======
                        %s
                        =========================== 
                        """.formatted(PRETTY_MAPPER.writeValueAsString(this));
            } catch (Exception e) {
                return super.toString();
            }
        }
    }
}
