package com.hexagonal.workflowlab;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * The only main() of the project. Component scanning starts here and reaches the use cases
 * ({@code @Service}), the domain collaborators ({@code @Component}) and every adapter.
 */
@SpringBootApplication
public class WorkflowLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowLabApplication.class, args);
    }

    /** {@code Clock} is a JDK class, so it cannot be annotated: it is the one bean that needs a factory method. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
