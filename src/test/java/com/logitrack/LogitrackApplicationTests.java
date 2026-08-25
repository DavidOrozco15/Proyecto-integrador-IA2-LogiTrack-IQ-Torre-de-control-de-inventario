package com.logitrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;   // ← agregar

@SpringBootTest
@ActiveProfiles("test")   // ← agregar
class LogitrackApplicationTests {

    @Test
    void contextLoads() {
    }

}
