package ru.cs.vsu.social_network.telegram_bot.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.service.command.TelegramCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Конфигурация для инициализации состояний команд
 */
@Slf4j
@Configuration
public class CommandStatesConfig {

    /**
     * -- GETTER --
     *  Получает карту состояний пользователей
     *
     * @return карта состояний пользователей
     */
    @Getter
    private final Map<Long, String> userStates = new HashMap<>();
    /**
     * -- GETTER --
     *  Получает карту состояний администраторов
     *
     * @return карта состояний администраторов
     */
    @Getter
    private final Map<Long, String> adminStates = new HashMap<>();
    /**
     * -- GETTER --
     *  Получает карту значений жима лежа
     *
     * @return карта значений жима лежа
     */
    @Getter
    private final Map<Long, Double> pendingBenchPressValues = new HashMap<>();
    /**
     * -- GETTER --
     *  Получает карту выбранных тренировочных циклов
     *
     * @return карта тренировочных циклов
     */
    @Getter
    private final Map<Long, String> pendingTrainingCycles = new HashMap<>();
    /**
     * -- GETTER --
     *  Получает карту выбранных форматов
     *
     * @return карта форматов
     */
    @Getter
    private final Map<Long, String> pendingFormatSelections = new HashMap<>();

    private final List<TelegramCommand> commands;

    public CommandStatesConfig(List<TelegramCommand> commands) {
        this.commands = commands;
    }

    @PostConstruct
    public void initCommandStates() {
        log.info("COMMAND_STATES_CONFIG_ИНИЦИАЛИЗАЦИЯ: начало инициализации состояний для {} команд", commands.size());

        for (TelegramCommand command : commands) {
            if (command instanceof BaseTelegramCommand baseCommand) {
                baseCommand.setUserStates(userStates);
                baseCommand.setAdminStates(adminStates);
                baseCommand.setPendingBenchPressValues(pendingBenchPressValues);
                baseCommand.setPendingTrainingCycles(pendingTrainingCycles);
                baseCommand.setPendingFormatSelections(pendingFormatSelections);

                log.debug("COMMAND_STATES_CONFIG: состояния инициализированы для команды {}",
                        command.getClass().getSimpleName());
            }
        }

        log.info("COMMAND_STATES_CONFIG_ИНИЦИАЛИЗАЦИЯ_УСПЕХ: состояния инициализированы для всех команд");
    }
}