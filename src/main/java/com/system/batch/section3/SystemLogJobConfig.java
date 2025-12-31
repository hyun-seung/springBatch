package com.system.batch.section3;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SystemLogJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /*
        1. 시스템 로그 생성
        echo -e "ERROR,mysql-prod,OOM,2024-01-24T09:30:00,heap space killing spree,85%,/var/log/mysql\nABORT,spring-batch,MemoryLeak,2024-01-24T10:15:30,forced termination,-1,/usr/apps/batch,TERMINATED\nCOLLECT,heap-dump,PID-9012,2024-01-24T11:00:15,/tmp/heapdump\nERROR,redis-cache,SocketTimeout,2024-01-24T13:45:00,connection timeout,92%,/var/log/redis\nABORT,zombie-process,Deadlock,2024-01-24T13:46:20,kill -9 executed,-1,/proc/dead,TERMINATED" > system-events.log

        2. bootRun 실행
        ./gradlew bootRun --args='--spring.batch.job.name=systemLogJob inputFile=./system-events.log'
     */
    @Bean
    public Job systemLogJob(Step systemLogStep) {
        return new JobBuilder("systemLogJob", jobRepository)
                .start(systemLogStep)
                .build();
    }

    @Bean
    public Step systemLogStep(
            FlatFileItemReader<SystemLog> systemLogReader,
            ItemWriter<SystemLog> systemLogWriter
    ) {
        return new StepBuilder("systemLogStep", jobRepository)
                .<SystemLog, SystemLog>chunk(10, transactionManager)
                .reader(systemLogReader)
                .writer(systemLogWriter)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<SystemLog> systemLogReader(
            @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
        return new FlatFileItemReaderBuilder<SystemLog>()
                .name("systemLogReader")
                .resource(new FileSystemResource(inputFile))
                .lineMapper(systemLogLineMapper())
                .build();
    }

    @Bean
    public PatternMatchingCompositeLineMapper<SystemLog> systemLogLineMapper() {
        PatternMatchingCompositeLineMapper<SystemLog> lineMapper = new PatternMatchingCompositeLineMapper<>();

        Map<String, LineTokenizer> tokenizers = new HashMap<>();
        tokenizers.put("ERROR*", errorLineTokenizer());
        tokenizers.put("ABORT*", abortLineTokenizer());
        tokenizers.put("COLLECT*", collectLineTokenizer());
        lineMapper.setTokenizers(tokenizers);

        Map<String, FieldSetMapper<SystemLog>> mappers = new HashMap<>();
        mappers.put("ERROR*", new ErrorFieldSetMapper());
        mappers.put("ABORT*", new AbortFieldSetMapper());
        mappers.put("COLLECT*", new CollectFieldSetMapper());
        lineMapper.setFieldSetMappers(mappers);

        return lineMapper;
    }

    @Bean
    public DelimitedLineTokenizer errorLineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames("type", "application", "errorType", "timestamp", "message", "resourceUsage", "logPath");
        return tokenizer;
    }

    @Bean
    public DelimitedLineTokenizer abortLineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames("type", "application", "errorType", "timestamp", "message", "exitCode", "processPath", "status");
        return tokenizer;
    }

    @Bean
    public DelimitedLineTokenizer collectLineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setNames("type", "dumpType", "processId", "timestamp", "dumpPath");
        return tokenizer;
    }

    @Bean
    public ItemWriter<SystemLog> systemLogWriter() {
        return items -> {
            for (SystemLog item : items) {
                log.info("{}", item);
            }
        };
    }

    @Data
    public static class SystemLog {
        private String type;
        private String timestamp;
    }

    @Data
    @ToString(callSuper = true)
    public static class ErrorLog extends SystemLog {
        private String application;
        private String errorType;
        private String message;
        private String resourceUsage;
        private String logPath;
    }

    @Data
    @ToString(callSuper = true)
    public static class AbortLog extends SystemLog {
        private String application;
        private String errorType;
        private String message;
        private String exitCode;
        private String processPath;
        private String status;
    }

    @Data
    @ToString(callSuper = true)
    public static class CollectLog extends SystemLog {
        private String dumpType;
        private String processId;
        private String dumpPath;
    }

    public static class ErrorFieldSetMapper implements FieldSetMapper<SystemLog> {
        @Override
        public SystemLog mapFieldSet(FieldSet fs) throws BindException {
            ErrorLog errorLog = new ErrorLog();
            errorLog.setType(fs.readString("type"));
            errorLog.setApplication(fs.readString("application"));
            errorLog.setErrorType(fs.readString("errorType"));
            errorLog.setTimestamp(fs.readString("timestamp"));
            errorLog.setMessage(fs.readString("message"));
            errorLog.setResourceUsage(fs.readString("resourceUsage"));
            errorLog.setLogPath(fs.readString("logPath"));
            return errorLog;
        }
    }

    public static class AbortFieldSetMapper implements FieldSetMapper<SystemLog> {
        @Override
        public SystemLog mapFieldSet(FieldSet fs) throws BindException {
            AbortLog abortLog = new AbortLog();
            abortLog.setType(fs.readString("type"));
            abortLog.setApplication(fs.readString("application"));
            abortLog.setErrorType(fs.readString("errorType"));
            abortLog.setTimestamp(fs.readString("timestamp"));
            abortLog.setMessage(fs.readString("message"));
            abortLog.setExitCode(fs.readString("exitCode"));
            abortLog.setProcessPath(fs.readString("processPath"));
            abortLog.setStatus(fs.readString("status"));
            return abortLog;
        }
    }


    public static class CollectFieldSetMapper implements FieldSetMapper<SystemLog> {
        @Override
        public SystemLog mapFieldSet(FieldSet fs) throws BindException {
            CollectLog collectLog = new CollectLog();
            collectLog.setType(fs.readString("type"));
            collectLog.setDumpType(fs.readString("dumpType"));
            collectLog.setProcessId(fs.readString("processId"));
            collectLog.setTimestamp(fs.readString("timestamp"));
            collectLog.setDumpPath(fs.readString("dumpPath"));
            return collectLog;
        }
    }

    public record SystemDeath(String command, int cpu, String status) {}

//    @Bean
//    @StepScope
//    public FlatFileItemReader<SystemDeath> systemDeathReader(
//            @Value("#{jobParameters['inputFile']}") String inputFile
//    ) {
//        return new FlatFileItemReaderBuilder<SystemDeath>()
//                .name("systemKillReader")
//                .resource(new FileSystemResource(inputFile))
//                .delimited()
//                .names("command", "cpu", "status")
//                .targetType(SystemDeath.class)
//                .linesToSkip(1)
//                .build();
//    }
}
