package com.system.batch.section4;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.MongoCursorItemReader;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.MongoPagingItemReader;
import org.springframework.batch.item.data.builder.MongoCursorItemReaderBuilder;
import org.springframework.batch.item.data.builder.MongoItemReaderBuilder;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.data.builder.MongoPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class HackerPatternDetectionJob {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MongoTemplate mongoTemplate;

    public HackerPatternDetectionJob(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            MongoTemplate mongoTemplate
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.mongoTemplate = mongoTemplate;
    }

    @Bean
    public Job detectHackerPatternJob(Step detectHackerPatternStep) {
        return new JobBuilder("detectHackerPatternJob", jobRepository)
                .start(detectHackerPatternStep)
                .build();
    }

    @Bean
    public Step detectHackerPatternStep(
            MongoCursorItemReader<SecurityLog> securityLogReader,
//            MongoPagingItemReader<SecurityLog> securityLogReader,
            ItemProcessor<SecurityLog, SecurityLog> hackerPatternProcessor,
//            ItemWriter<SecurityLog> securityLogWriter
            MongoItemWriter<SecurityLog> securityLogWriter
    ) {
        return new StepBuilder("detectHackerPatternStep", jobRepository)
                .<SecurityLog, SecurityLog>chunk(10, transactionManager)
                .reader(securityLogReader)
                .processor(hackerPatternProcessor)
                .writer(securityLogWriter)
                .build();
    }

//    @Bean
//    @StepScope
//    public MongoCursorItemReader<SecurityLog> securityLogReader(
//            @Value("#{jobParameters['searchDate']}") LocalDate searchDate
//    ) {
//        Date startOfDay = Date.from(searchDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
//        Date endOfDay = Date.from(searchDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
//
//        return new MongoCursorItemReaderBuilder<SecurityLog>()
//                .name("securityLogReader")
//                .template(mongoTemplate)
//                .collection("security_logs")
//                .jsonQuery("""
//                        {
//                            "label": "PENDING_ANALYSIS",
//                            "timestamp": {
//                                "$gte": ?0,
//                                "$lt": ?1
//                            }
//                        }
//                    """)
//                .parameterValues(List.of(startOfDay, endOfDay))
//                .sorts(Map.of("timestamp", Sort.Direction.ASC))
//                .targetType(SecurityLog.class)
//                .batchSize(10)
//                .build();
//    }

    @Bean
    @StepScope
    public MongoCursorItemReader<SecurityLog> securityLogReader(
            @Value("#{jobParameters['searchDate']}") LocalDate searchDate
    ) {
        Query query = new Query()
                .addCriteria(Criteria.where("label").is("PENDING_ANALYSIS"))
                .addCriteria(Criteria.where("timestamp")
                        .gte(Date.from(searchDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                        .lt(Date.from(searchDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())))
                .with(Sort.by(Sort.Direction.ASC, "timestamp"))
                .cursorBatchSize(10);

        return new MongoCursorItemReaderBuilder<SecurityLog>()
                .name("securityLogReader")
                .template(mongoTemplate)
                .collection("security_logs")
                .query(query)
                .sorts(Map.of("timestamp", Sort.Direction.ASC))
                .targetType(SecurityLog.class)
                .build();
    }

//    @Bean
//    @StepScope
//    public MongoPagingItemReader<SecurityLog> securityLogReader(
//            @Value("#{jobParameters['searchDate']}") LocalDate searchDate
//    ) {
//        Query query = new Query()
//                .addCriteria(Criteria.where("label").is("PENDING_ANALYSIS"))
//                .addCriteria(Criteria.where("timestamp")
//                        .gte(Date.from(searchDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
//                        .lt(Date.from(searchDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())))
//                .with(Sort.by(Sort.Direction.ASC, "timestamp"));
//
//        return new MongoPagingItemReaderBuilder<SecurityLog>()
//                .name("securityLogReader")
//                .template(mongoTemplate)
//                .collection("security_logs")
//                .query(query)
//                .sorts(Map.of("timestamp", Sort.Direction.ASC))
//                .targetType(SecurityLog.class)
//                .pageSize(10)
//                .build();
//    }

    @Bean
    public ItemProcessor<SecurityLog, SecurityLog> hackerPatternProcessor() {
        return log -> {
            String detectedPattern = analyzeAttackPattern(log);
            log.setLabel(detectedPattern);
            return log;
        };
    }

    private String analyzeAttackPattern(SecurityLog securityLog) {
        String command = securityLog.getCommand().toLowerCase();

        if (command.contains("ssh") || command.contains("telnet")) {
            return "Lateral_Movement";
        }

        if (command.contains("sudo") || command.contains("su ")) {
            return "Privilege_Escalation";
        }

        if (command.contains("history -c") || command.contains("rm /var/log") || command.contains("killall rsyslog")) {
            return "Defense_Evasion";
        }

        return "UNKNOWN";
    }

//    @Bean
//    public ItemWriter<SecurityLog> securityLogItemWriter() {
//        return items -> items.forEach(securityLog ->
//            log.info("[패턴 탐지] {}: {}", securityLog.getLabel(), securityLog)
//        );
//    }

    @Bean
    public MongoItemWriter<SecurityLog> securityLogItemWriter() {
        return new MongoItemWriterBuilder<SecurityLog>()
                .template(mongoTemplate)
                .collection("security_logs")
                .mode(MongoItemWriter.Mode.UPSERT)
                .build();
    }

    @Data
    @Document
    public static class SecurityLog {
        @Id
        private String id;
        private String attackerId;
        private String command;
        private LocalDateTime timestamp;
        private String label;
    }
}
