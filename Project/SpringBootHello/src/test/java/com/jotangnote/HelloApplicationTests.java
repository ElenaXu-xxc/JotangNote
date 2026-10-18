package com.jotangnote;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "mimo.api.key=test-only-key")
class HelloApplicationTests {

	@Test
	void contextLoads() {
	}

}
