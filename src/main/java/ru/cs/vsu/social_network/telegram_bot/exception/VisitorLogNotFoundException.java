package ru.cs.vsu.social_network.telegram_bot.exception;

public class VisitorLogNotFoundException extends RuntimeException {
    public VisitorLogNotFoundException(String message) {
        super(message);
    }
}
