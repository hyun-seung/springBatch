package com.system.batch.section3;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SystemFailureJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /*
        1. 파일 준비
        echo -e "에러ID,발생시각,심각도,프로세스ID,에러메시지\nERR001,2024-01-19 10:15:23,CRITICAL,1234,SYSTEM_CRASH\nERR002,2024-01-19 10:15:25,FATAL,1235,MEMORY_OVERFLOW" > system-failures.csv

        2. bootRun 실행
        ./gradlew bootRun --args='--spring.batch.job.name=systemFailureJob inputFile=/Users/hyunseung/IdeaProjects/kill-batch-system-boot/system-failures.csv'

        --

        1. 파일 준비
        echo -e "ERR001  2024-01-19 10:15:23  CRITICAL  1234  SYSTEM  CRASH DETECT \nERR002  2024-01-19 10:15:25  FATAL     1235  MEMORY  OVERFLOW FAIL" > system-failures.txt

        2. bootRun 실행
        ./gradlew bootRun --args='--spring.batch.job.name=systemFailureJob inputFile=/Users/hyunseung/IdeaProjects/kill-batch-system-boot/system-failures.txt'
     */
    @Bean
    public Job systemFailureJob(Step systemFailureStep) {
        return new JobBuilder("systemFailureJob", jobRepository)
                .start(systemFailureStep)
                .build();
    }

//    @Bean
//    public Step systemFailureStep(
//            FlatFileItemReader<SystemFailure> systemFailureItemReader,
//            SystemFailureStdoutItemWriter systemFailureStdoutItemWriter
//    ) {
//        return new StepBuilder("systemFailureStep", jobRepository)
//                .<SystemFailure, SystemFailure>chunk(10, transactionManager)
//                .reader(systemFailureItemReader)
//                .writer(systemFailureStdoutItemWriter)
//                .build();
//    }

//    @Bean
//    @StepScope
//    public FlatFileItemReader<SystemFailure> systemFailureItemReader(
//            @Value("#{jobParameters['inputFile']}") String inputFile
//    ) {
//        return new FlatFileItemReaderBuilder<SystemFailure>()
//                .name("systemFailureItemReader")
//                .resource(new FileSystemResource(inputFile))    // 읽어 들일 Resource 지정
//                .delimited()    // 구분자로 분리된 형식임을 알리는 설정, DelimitedLineTokenizer 가 지정
//                .delimiter(",") // 구분자 지정
//                .names("errorId", "errorDateTime", "severity", "processId", "errorMessage")
//                .targetType(SystemFailure.class)    // 매핑 대상 클래스 지정
//                .linesToSkip(1) // 헤더 처리 -> 첫 줄 건너띄고 데이터 처리
//                .build();
//    }

    @Bean
    @StepScope
    public FlatFileItemReader<SystemFailure> systemFailureItemReader(
            @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
        return new FlatFileItemReaderBuilder<SystemFailure>()
                .name("systemFailureItemReader")
                .resource(new FileSystemResource(inputFile))
                .fixedLength()  // 고정 길이 형식
                .columns(new Range[] {
                        new Range(1, 8),
                        new Range(9, 29),
                        new Range(30, 39),
                        new Range(40, 45),
                        new Range(46, 66),
                })
                .names("errorId", "errorDateTime", "severity", "processId", "errorMessage")
                .targetType(SystemFailure.class)
                .customEditors(Map.of(LocalDateTime.class, dateTimeEditor()))
                .build();
    }

    private PropertyEditor dateTimeEditor() {
        return new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                setValue(LocalDateTime.parse(text, formatter));
            }
        };
    }

    @Bean
    public SystemFailureStdoutItemWriter systemFailureStdoutItemWriter() {
        return new SystemFailureStdoutItemWriter();
    }

    public static class SystemFailureStdoutItemWriter implements ItemWriter<SystemFailure> {
        @Override
        public void write(Chunk<? extends SystemFailure> chunk) throws Exception {
            for (SystemFailure failure : chunk) {
                log.info("Processing system failure: {}", failure);
            }
        }
    }

    @Data
    public static class SystemFailure {
        private String errorId;
        private String errorDateTime;
//        private LocalDateTime errorDateTime;
        private String severity;
        private Integer processId;
        private String errorMessage;
    }

    /*
        1. 파일 생성
        echo -e "에러ID,발생시각,심각도,프로세스ID,에러메시지\nERR001,2024-01-19 10:15:23,CRITICAL,1234,SYSTEM_CRASH\nERR002,2024-01-19 10:15:25,FATAL,1235,MEMORY_OVERFLOW\nERR003,2024-01-19 10:16:10,CRITICAL,1236,DATABASE_CORRUPTION" > critical-failures.csv
        echo -e "에러ID,발생시각,심각도,프로세스ID,에러메시지\nERR101,2024-01-19 10:20:30,WARN,2001,HIGH_CPU_USAGE\nERR102,2024-01-19 10:21:15,INFO,2002,CACHE_MISS\nERR103,2024-01-19 10:22:45,WARN,2003,SLOW_QUERY_DETECTED" > normal-failures.csv

        2. 실행
        ./gradlew bootRun --args='--spring.batch.job.name=systemFailureJob inputFilePath=./'
     */
    @Bean
    public Step systemFailureStep(
            MultiResourceItemReader<SystemFailure> multiSystemFailureItemReader,
            SystemFailureStdoutItemWriter systemFailureStdoutItemWriter
    ) {
        return new StepBuilder("systemFailureStep", jobRepository)
                .<SystemFailure, SystemFailure>chunk(10, transactionManager)
                .reader(multiSystemFailureItemReader)
                .writer(systemFailureStdoutItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<SystemFailure> multiSystemFailureItemReader(
            @Value("#{jobParameters['inputFilePath']}") String inputFilePath
    ) {
        return new MultiResourceItemReaderBuilder<SystemFailure>()
                .name("multiSystemFailureItemReader")
                .resources(new Resource[] {
                        new FileSystemResource(inputFilePath + "/critical-failures.csv"),
                        new FileSystemResource(inputFilePath + "/normal-failures.csv")
                })
                .delegate(systemFailureFileReader())
                .build();
    }

    @Bean
    public FlatFileItemReader<SystemFailure> systemFailureFileReader() {
        return new FlatFileItemReaderBuilder<SystemFailure>()
                .name("systemFailureFileReader")
                .delimited()
                .delimiter(",")
                .names("errorId", "errorDateTime", "severity", "processId", "errorMessage")
                .targetType(SystemFailure.class)
                .linesToSkip(1)
                .build();
    }
}
