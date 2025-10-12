package com.system.batch.section2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlimService {

    public void send(String s) {
        log.info("AlimService - send call : {}", s);
    }
}
