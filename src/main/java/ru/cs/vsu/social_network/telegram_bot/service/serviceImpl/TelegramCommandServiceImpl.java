package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.service.TelegramCommandService;
import ru.cs.vsu.social_network.telegram_bot.service.command.CommandFactory;
import ru.cs.vsu.social_network.telegram_bot.service.command.TelegramCommand;

import java.util.Map;

/**
 * Реализация сервиса обработки Telegram команд.
 * Делегирует выполнение командам из фабрики.
 */
@Slf4j
@Service
public class TelegramCommandServiceImpl implements TelegramCommandService {

    private static final String SERVICE_NAME = "TELEGRAM_COMMAND_SERVICE";
    private final CommandFactory commandFactory;

    /**
     * Конструктор сервиса команд.
     *
     * @param commandFactory фабрика команд
     */
    public TelegramCommandServiceImpl(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleStartCommand(Long telegramId, String username, String firstName, String lastName) {
        String input = username + "|" + (firstName != null ? firstName : "") + "|" + (lastName != null ? lastName : "");
        return executeCommand("start", telegramId, input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleInGymCommand(Long telegramId) {
        return executeCommand("ingym", telegramId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleDisplayNameInput(Long telegramId, String displayName) {
        return executeCommand("displaynameinput", telegramId, displayName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleDailyReportCommand(Long telegramId, String dateStr) {
        return executeCommand("dailyreport", telegramId, dateStr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handlePeriodReportCommand(Long telegramId, String startDateStr, String endDateStr) {
        String input = startDateStr + " " + endDateStr;
        return executeCommand("periodreport", telegramId, input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleTableCommand(Long telegramId, String input) {
        return executeCommand("table", telegramId, input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleUnknownCommand(Long telegramId) {
        return executeCommand("help", telegramId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleAdminMenuCommand(Long telegramId, String menuCommand) {
        return executeCommand("adminmenu", telegramId, menuCommand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleAdminDateInput(Long telegramId, String dateInput) {
        return executeCommand("admindateinput", telegramId, dateInput);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleHelpCommand(Long telegramId) {
        return executeCommand("help", telegramId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleChangeNameCommand(Long telegramId) {
        return executeCommand("changename", telegramId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleTrainingProgramCommand(Long telegramId) {
        return executeCommand("trainingprogram", telegramId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleBenchPressInput(Long telegramId, String benchPressInput) {
        return executeCommand("benchpressinput", telegramId, benchPressInput);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleFormatSelection(Long telegramId, String formatChoice) {
        return executeCommand("formatselection", telegramId, formatChoice);
    }

    /**
     * Выполняет команду через фабрику.
     *
     * @param commandName имя команды
     * @param telegramId идентификатор пользователя
     * @param input входные данные
     * @return результат выполнения команды
     */
    private String executeCommand(String commandName, Long telegramId, String input) {
        try {
            TelegramCommand command = commandFactory.getCommand(commandName);
            if (command != null) {
                return command.execute(telegramId, input);
            } else {
                log.error("{}_COMMAND_NOT_FOUND: команда '{}' не найдена",
                        SERVICE_NAME, commandName);
                return commandFactory.getCommand("unknown").execute(telegramId, null);
            }
        } catch (Exception e) {
            log.error("{}_COMMAND_EXECUTION_ERROR: ошибка при выполнении команды '{}' для {}: {}",
                    SERVICE_NAME, commandName, telegramId, e.getMessage(), e);
            return "Произошла ошибка при обработке команды.\n\n" +
                    "Пожалуйста, попробуйте позже или обратитесь к администратору.";
        }
    }
}