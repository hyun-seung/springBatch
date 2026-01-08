package com.system.batch.section4;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@NamedQuery(
        name = "Post.findByReportsReportedAtBetween",
        query = "SELECT p FROM Post p JOIN FETCH p.reports r WHERE r.reportedAt >= :startDateTime AND r.reportedAt < :endDateTime"
)
@Getter
@Setter
public class Post {

    @Id
    private Long id;
    private String title;       // 게시물 제목
    private String content;     // 게시물 내용
    private String writer;      // 작성자

    @OneToMany(mappedBy = "post", fetch = FetchType.EAGER)
    @BatchSize(size = 5)
    private List<Report> reports = new ArrayList<>();

    private LocalDateTime blockedAt;
}
