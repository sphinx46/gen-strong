package ru.cs.vsu.social_network.telegram_bot.exception;

public class UserTrainingNotFoundException extends RuntimeException {
    public UserTrainingNotFoundException(String message) {
        super(message);
    }
}
