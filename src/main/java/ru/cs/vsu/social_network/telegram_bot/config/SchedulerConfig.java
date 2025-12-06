package ru.cs.vsu.social_network.telegram_bot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Конфигурация планировщика задач.
 * Включает поддержку аннотации @Scheduled и настраивает пул потоков для выполнения задач.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    /**
     * Создает и настраивает ThreadPoolTaskScheduler для выполнения запланированных задач.
     * Настройки пула позволяют эффективно управлять ресурсами при выполнении периодических операций.
     *
     * @return Настроенный экземпляр ThreadPoolTaskScheduler
     */
    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        log.info("SCHEDULER_CONFIG_НАСТРОЙКА_ПЛАНИРОВЩИКА_НАЧАЛО");

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("gym-bot-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);

        log.info("SCHEDULER_CONFIG_НАСТРОЙКА_ПЛАНИРОВЩИКА_УСПЕХ: poolSize={}, threadPrefix={}",
                scheduler.getPoolSize(), scheduler.getThreadNamePrefix());

        return scheduler;
    }
}