package ru.cs.vsu.social_network.telegram_bot.entity.enums;

public enum FITNESS_GOAL {
    MUSCLE_GAIN("Набор мышечной массы"),
    WEIGHT_LOSS("Похудение"),
    MAINTENANCE("Поддержание формы");

    private final String russianName;

    FITNESS_GOAL(String russianName) {
        this.russianName = russianName;
    }

    public String getRussianName() {
        return russianName;
    }
}