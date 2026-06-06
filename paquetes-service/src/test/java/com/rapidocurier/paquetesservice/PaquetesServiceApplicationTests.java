package com.rapidocurier.paquetesservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Disabled("Context test disabled until infrastructure layer is implemented")
class PaquetesServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
