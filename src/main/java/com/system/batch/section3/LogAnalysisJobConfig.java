package com.system.batch.section3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.RegexLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LogAnalysisJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /*
        1. 파일 생성
        echo -e "[WARNING][Thread-156][CPU: 78%] Thread pool saturation detected - 45/50 threads in use...\n[ERROR][Thread-157][CPU: 92%] Thread deadlock detected between Thread-157 and Thread-159\n[FATAL][Thread-159][CPU: 95%] Thread dump initiated - system unresponsive for 30s" > log.txt

        2. bootRun 실행
        ./gradlew bootRun --args='--spring.batch.job.name=logAnalysisJob inputFile=/Users/hyunseung/IdeaProjects/kill-batch-system-boot/log.txt'
     */
    @Bean
    public Job logAnalysisJob(Step logAnalysisStep) {
        return new JobBuilder("logAnalysisJob", jobRepository)
                .start(logAnalysisStep)
                .build();
    }

    @Bean
    public Step logAnalysisStep(
            FlatFileItemReader<LogEntry> logItemReader,
            ItemWriter<LogEntry> logItemWriter
    ) {
        return new StepBuilder("logAnalysisStep", jobRepository)
                .<LogEntry, LogEntry>chunk(10, transactionManager)
                .reader(logItemReader)
                .writer(logItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<LogEntry> logItemReader(
            @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
        RegexLineTokenizer tokenizer = new RegexLineTokenizer();
        tokenizer.setRegex("\\[\\w+\\]\\[Thread-(\\d+)\\]\\[CPU: \\d+%\\] (.+)");

        return new FlatFileItemReaderBuilder<LogEntry>()
                .name("logItemReader")
                .resource(new FileSystemResource(inputFile))
                .lineTokenizer(tokenizer)
                .fieldSetMapper(fieldSet -> new LogEntry(fieldSet.readString(0), fieldSet.readString(1)))
                .build();
    }

    @Bean
    public ItemWriter<LogEntry> logItemWriter() {
        return items -> {
            for (LogEntry logEntry : items) {
                log.info(String.format("THD-%s: %s", logEntry.getThreadNum(), logEntry.getMessage()));
            }
        };
    }

    @Data
    @AllArgsConstructor
    public static class LogEntry {
        private String threadNum;
        private String message;
    }
}
