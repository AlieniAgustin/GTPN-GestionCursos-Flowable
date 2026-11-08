package com.flowable;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.App;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:course_limit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class CourseLimitExceededTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private CourseProcessFixture fixture;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("6. Course limit: with 5 courses in preparation, the sixth is not viable")
    void testCourseNotViableWhenLimitExceeded() {
        // The DMN evaluates as viable when there are fewer than 5 active courses
        for (int i = 0; i < 5; i++) {
            fixture.startCourseInPreparation();
        }

        ProcessInstance processInstance = fixture.startProcessInstance();
        assertNotNull(processInstance, "Process instance should have started");

        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertNull(activeInstance, "Process instance should have ended");
    }
}
