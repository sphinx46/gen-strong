package ru.cs.vsu.social_network.telegram_bot.exception;

public class VisitNotFoundException extends RuntimeException {
    public VisitNotFoundException(String message) {
        super(message);
    }
}
