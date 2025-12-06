package ru.cs.vsu.social_network.telegram_bot.exception;

public class VisitRepeatException extends RuntimeException {
    public VisitRepeatException(String message) {
        super(message);
    }
}
