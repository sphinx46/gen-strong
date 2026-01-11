package ru.cs.vsu.social_network.telegram_bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.cs.vsu.social_network.telegram_bot.service.command.CommandFactory;
import ru.cs.vsu.social_network.telegram_bot.service.command.impl.*;

/**
 * Конфигурация для регистрации всех команд в фабрике.
 */
@Configuration
public class CommandConfig {

    /**
     * Настраивает фабрику команд с регистрацией всех команд.
     *
     * @param startCommand команда /start
     * @param inGymCommand команда "Я в зале"
     * @param helpCommand команда /help
     * @param changeNameCommand команда смены имени
     * @param displayNameInputCommand команда ввода имени
     * @param trainingProgramCommand команда программы тренировок
     * @param benchPressInputCommand команда ввода жима лежа
     * @param formatSelectionCommand команда выбора формата
     * @param dailyReportCommand команда дневного отчета
     * @param periodReportCommand команда отчета за период
     * @param tableCommand команда таблицы
     * @param adminMenuCommand команда меню администратора
     * @param adminDateInputCommand команда ввода даты администратором
     * @return настроенная фабрика команд
     */
    @Bean
    public CommandFactory commandFactory(
            StartCommand startCommand,
            InGymCommand inGymCommand,
            HelpCommand helpCommand,
            ChangeNameCommand changeNameCommand,
            DisplayNameInputCommand displayNameInputCommand,
            TrainingProgramCommand trainingProgramCommand,
            BenchPressInputCommand benchPressInputCommand,
            FormatSelectionCommand formatSelectionCommand,
            DailyReportCommand dailyReportCommand,
            PeriodReportCommand periodReportCommand,
            TableCommand tableCommand,
            AdminMenuCommand adminMenuCommand,
            AdminDateInputCommand adminDateInputCommand) {

        CommandFactory factory = new CommandFactory();

        factory.registerCommand("start", startCommand);
        factory.registerCommand("ingym", inGymCommand);
        factory.registerCommand("help", helpCommand);
        factory.registerCommand("changename", changeNameCommand);
        factory.registerCommand("displaynameinput", displayNameInputCommand);
        factory.registerCommand("trainingprogram", trainingProgramCommand);
        factory.registerCommand("benchpressinput", benchPressInputCommand);
        factory.registerCommand("formatselection", formatSelectionCommand);
        factory.registerCommand("dailyreport", dailyReportCommand);
        factory.registerCommand("periodreport", periodReportCommand);
        factory.registerCommand("table", tableCommand);
        factory.registerCommand("adminmenu", adminMenuCommand);
        factory.registerCommand("admindateinput", adminDateInputCommand);

        return factory;
    }
}