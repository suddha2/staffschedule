package com.midco.rota;

import org.optaplanner.spring.boot.autoconfigure.OptaPlannerAutoConfiguration;
import org.optaplanner.spring.boot.autoconfigure.OptaPlannerBenchmarkAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// OptaPlanner 10's auto-configuration doesn't reliably build the solver beans in
// this project (and its benchmark auto-config demands a SolverConfig bean); the
// solver/score/manager beans are defined explicitly in OptaPlannerConfig instead.
@SpringBootApplication(exclude = { OptaPlannerAutoConfiguration.class, OptaPlannerBenchmarkAutoConfiguration.class })
@EnableScheduling
@EnableAsync
public class RotaServiceApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(RotaServiceApplication.class, args);
    }
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(RotaServiceApplication.class);
    }
}	