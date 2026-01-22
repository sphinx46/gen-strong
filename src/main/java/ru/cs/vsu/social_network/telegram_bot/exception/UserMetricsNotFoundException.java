package ru.cs.vsu.social_network.telegram_bot.exception;

public class UserMetricsNotFoundException extends RuntimeException {
    public UserMetricsNotFoundException(String message) {
        super(message);
    }
}
