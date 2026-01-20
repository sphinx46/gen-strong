package ru.cs.vsu.social_network.telegram_bot.service.command;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CommandFactory {

    private final Map<String, TelegramCommand> commands = new HashMap<>();
    private final List<TelegramCommand> commandList;

    public CommandFactory(List<TelegramCommand> commandList) {
        this.commandList = commandList;
    }

    @PostConstruct
    public void init() {
        registerCommands();
        log.info("COMMAND_FACTORY_ИНИЦИАЛИЗАЦИЯ: зарегистрировано {} команд", commands.size());
    }

    private void registerCommands() {
        commandList.forEach(command -> {
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
                commands.put("составитьпрограммутренировок", command);
                commands.put("программа", command);
            } else if (command instanceof ru.cs.vsu.social_network.telegram_bot.service.command.impl.ContributionCommand) {
                commands.put("contribution", command);
                commands.put("внестивклад", command);
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
        });

        commands.put("unknown", commands.get("help"));
    }

    public TelegramCommand getCommand(String commandName) {
        return commands.get(commandName.toLowerCase());
    }
}