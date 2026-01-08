package com.system.batch.section4;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
public class Report {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    private String reportType;      // SPAM, ABUSE, ILLEGAL, FAKE_NEW ...
    private int reporterLevel;      // 신고자 신뢰도 (1~5)
    private String evidenceData;    // 증거 데이터 (URL 등)
    private LocalDateTime reportedAt;
}
