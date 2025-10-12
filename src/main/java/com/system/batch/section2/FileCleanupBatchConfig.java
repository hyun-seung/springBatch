package com.system.batch.section2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileCleanupBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public Tasklet deleteOldFilesTasklet() {
        // "temp" 디렉토리에서 30일 이상 지난 파일 삭제
        return new DeleteOldFilesTasklet("/path/to/temp", 30);
    }

    @Bean
    public Step deleteOldFilesStep() {
        return new StepBuilder("deleteOldFilesStep", jobRepository)
                .tasklet(deleteOldFilesTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Job deleteOldFilesJob() {
        return new JobBuilder("deleteOldFilesJob", jobRepository)
                .start(deleteOldFilesStep())
                .build();
    }

    @Bean
    public Step deleteOldRecordsStep() {
        // 단순한 작업이라면 별도의 Tasklet 구현 클래스를 만들지 않고,
        // 람다 표현식을 사용해 Step 구성 중에 바로 Tasklet 을 정의할 수 있다.
        // 복잡한 로직이 필요하지 않다면 이 방식이 훨씬 직관적이고 간편하다.
        return new StepBuilder("deleteOldRecordsStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int deleted = jdbcTemplate.update("DELETE FROM logs WHERE created < NOW() - INTERNAL 7 DAY");
                    log.info("🗑️ {}개의 오래된 레코드가 삭제되었습니다.", deleted);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job deleteOldRecordsJob() {
        return new JobBuilder("deleteOldRecordsJob", jobRepository)
                .start(deleteOldRecordsStep())
                .build();
    }
}
