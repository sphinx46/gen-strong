package ru.cs.vsu.social_network.telegram_bot.service.command;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Фабрика для создания команд Telegram бота.
 * Регистрирует все доступные команды и предоставляет их по запросу.
 */
@Slf4j
@Component
public class CommandFactory {

    private final Map<String, TelegramCommand> commands = new HashMap<>();
    private final List<TelegramCommand> commandList;

    /**
     * Конструктор фабрики команд.
     *
     * @param commandList список всех команд, зарегистрированных в Spring контексте
     */
    public CommandFactory(List<TelegramCommand> commandList) {
        this.commandList = commandList;
    }

    /**
     * Инициализирует фабрику команд.
     */
    @PostConstruct
    public void init() {
        registerCommands();
        log.info("COMMAND_FACTORY_ИНИЦИАЛИЗАЦИЯ: зарегистрировано {} команд", commands.size());
    }

    /**
     * Регистрирует все команды.
     */
    private void registerCommands() {
        for (TelegramCommand command : commandList) {
            if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.StartCommand) {
                commands.put("start", command);
                commands.put("старт", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.HelpCommand) {
                commands.put("help", command);
                commands.put("помощь", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.InGymCommand) {
                commands.put("ingym", command);
                commands.put("явзале", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.ChangeNameCommand) {
                commands.put("changename", command);
                commands.put("сменитьимя", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.TrainingProgramCommand) {
                commands.put("trainingprogram", command);
                commands.put("составитьпрограмму", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.TrainingCycleSelectionCommand) {
                commands.put("trainingcycleselection", command);
                commands.put("выборцикла", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.BenchPressInputCommand) {
                commands.put("benchpressinput", command);
                commands.put("жимлежа", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.FormatSelectionCommand) {
                commands.put("formatselection", command);
                commands.put("форматпрограммы", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.DisplayNameInputCommand) {
                commands.put("displaynameinput", command);
                commands.put("вводимени", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.AdminMenuCommand) {
                commands.put("adminmenu", command);
                commands.put("админменю", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.AdminDateInputCommand) {
                commands.put("admindateinput", command);
                commands.put("админдата", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.DailyReportCommand) {
                commands.put("dailyreport", command);
                commands.put("дневнойотчет", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.PeriodReportCommand) {
                commands.put("periodreport", command);
                commands.put("периодотчет", command);
            }
        }

        commands.put("unknown", commands.get("help"));
    }

    /**
     * Получает команду по имени.
     *
     * @param commandName имя команды
     * @return команда или null, если команда не найдена
     */
    public TelegramCommand getCommand(String commandName) {
        return commands.get(commandName.toLowerCase());
    }
}