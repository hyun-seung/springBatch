package com.system.batch.section4;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VictimRecordConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    /*
       1. 테이블 생성
       CREATE TABLE victims (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(255),
            process_id VARCHAR(50),
            terminated_at TIMESTAMP,
            status VARCHAR(20)
        );

       2. 데이터 인입
       INSERT INTO victims (name, process_id, terminated_at, status) VALUES
        ('zombie_process', 'PID_12345', '2024-01-01 12:00:00', 'TERMINATED'),
        ('sleeping_thread', 'PID_45678', '2024-01-15 15:30:00', 'TERMINATED'),
        ('memory_leak', 'PID_98765', '2024-02-01 09:15:00', 'RUNNING'),
        ('infinite_loop', 'PID_24680', '2024-02-15 18:45:00', 'RUNNING');

       3. 실행
       ./gradlew bootRun --args='--spring.batch.job.name=victimRecordJob'
     */
    @Bean
    public Job processVictimJob() {
        return new JobBuilder("victimRecordJob", jobRepository)
                .start(processVictimStep())
                .build();
    }

    @Bean
    public Step processVictimStep() {
        return new StepBuilder("victimRecordStep", jobRepository)
                .<Victim, Victim>chunk(5, transactionManager)
                .reader(terminatedVictimReader())
                .writer(victimWriter())
                .build();
    }

//    @Bean
//    public JdbcCursorItemReader<Victim> terminatedVictimReader() {
//        return new JdbcCursorItemReaderBuilder<Victim>()
//                .name("terminatedVictimReader")
//                .dataSource(dataSource)
//                .sql("SELECT * FROM victims WHERE status = ? AND terminated_at <= ?")
//                .queryArguments(List.of("TERMINATED", LocalDateTime.now()))
////                .beanRowMapper(Victim.class)
//                .rowMapper(new DataClassRowMapper<>(Victim.class))
//                .build();
//    }

//    @Bean
//    public JdbcPagingItemReader<Victim> terminatedVictimReader() {
//        return new JdbcPagingItemReaderBuilder<Victim>()
//                .name("terminatedVictimReader")
//                .dataSource(dataSource)
//                .pageSize(5)
//                .selectClause("SELECT id, name, process_id, terminated_at, status")
//                .fromClause("FROM victims")
//                .whereClause("WHERE status = :status AND terminated_at <= :terminatedAt")
//                .sortKeys(Map.of("id", Order.ASCENDING))
//                .parameterValues(Map.of(
//                        "status", "TERMINATED",
//                        "terminatedAt", LocalDateTime.now()
//                ))
////                .beanRowMapper(Victim.class)
//                .dataRowMapper(Victim.class)
//                .build();
//    }

//    @Bean
//    public JdbcPagingItemReader<Victim> terminatedVictimReader(DataSource dataSource) {
//        return new JdbcPagingItemReaderBuilder<Victim>()
//                .name("terminatedVictimReader")
//                .dataSource(dataSource)
//                .queryProvider(pagingQueryProvider(dataSource))
//                .parameterValues(Map.of(
//                        "status", "TERMINATED",
//                        "terminatedAt", LocalDateTime.now().minusDays(1)))
//                .pageSize(5)
//                .rowMapper(new BeanPropertyRowMapper<>(Victim.class))
//                .build();
//    }

    @Bean
    public JdbcPagingItemReader<Victim> terminatedVictimReader() {
        return new JdbcPagingItemReaderBuilder<Victim>()
                .name("terminatedVictimReader")
                .dataSource(dataSource)
                .pageSize(5)
                .queryProvider(pagingQueryProvider(dataSource))
                .parameterValues(Map.of(
                        "status", "TERMINATED",
                        "terminatedAt", LocalDateTime.now()))
                .rowMapper(new BeanPropertyRowMapper<>(Victim.class))
                .build();
    }

    private PagingQueryProvider pagingQueryProvider(DataSource dataSource) {
        SqlPagingQueryProviderFactoryBean queryProviderFactory = new SqlPagingQueryProviderFactoryBean();

        queryProviderFactory.setDataSource(dataSource);
        queryProviderFactory.setSelectClause("SELECT id, name, process_id, terminated_at, status");
        queryProviderFactory.setFromClause("FROM victims");
        queryProviderFactory.setWhereClause("WHERE status = :status AND terminated_at <= :terminatedAt");
        queryProviderFactory.setSortKeys(Map.of("id", Order.ASCENDING));
        try {
            return queryProviderFactory.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public ItemWriter<Victim> victimWriter() {
        return items -> {
            for (Victim victim : items) {
                log.info("{}", victim);
            }
        };
    }

    @NoArgsConstructor
    @Data
    public static class Victim {
        private Long id;
        private String name;
        private String processId;
        private LocalDateTime terminatedAt;
        private String status;
    }

//    public record Victim(Long id, String name, String processId, LocalDateTime terminatedAt, String status) {}
}
