package ru.cs.vsu.social_network.telegram_bot.provider.providerImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.exception.UserNotFoundException;
import ru.cs.vsu.social_network.telegram_bot.provider.AbstractEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.provider.UserEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.UserRepository;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.util.Optional;


/**
 * Реализация провайдера для получения сущности User.
 * Обеспечивает доступ к данным пользователей с обработкой исключительных ситуаций.
 */
@Slf4j
@Component
public final class UserEntityProviderImpl extends AbstractEntityProvider<User>
        implements UserEntityProvider {
    private static final String ENTITY_NAME = "ПОЛЬЗОВАТЕЛЬ";
    private final UserRepository userRepository;

    public UserEntityProviderImpl(UserRepository userRepository) {
        super(userRepository, ENTITY_NAME, () ->
                new UserNotFoundException(MessageConstants.USER_NOT_FOUND_FAILURE));
        this.userRepository = userRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> findByTelegramId(Long telegramId) {
        log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_TELEGRAM_ID_НАЧАЛО: " +
                        "поиск пользователя с Telegram ID: {}",
                ENTITY_NAME, telegramId);

        final Optional<User> user = userRepository.findByTelegramId(telegramId);

        log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_TELEGRAM_ID_УСПЕХ: " +
                        "пользователь {} с Telegram ID: {}",
                ENTITY_NAME, user.isPresent() ? "найден" : "не найден", telegramId);

        return user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByTelegramId(Long telegramId) {
        log.info("{}_ПРОВАЙДЕР_ПРОВЕРКА_СУЩЕСТВОВАНИЯ_ПО_TELEGRAM_ID_НАЧАЛО: " +
                        "проверка существования пользователя с Telegram ID: {}",
                ENTITY_NAME, telegramId);

        final boolean exists = userRepository.existsByTelegramId(telegramId);

        log.info("{}_ПРОВАЙДЕР_ПРОВЕРКА_СУЩЕСТВОВАНИЯ_ПО_TELEGRAM_ID_УСПЕХ: " +
                        "пользователь {} с Telegram ID: {}",
                ENTITY_NAME, exists ? "существует" : "не существует", telegramId);

        return exists;
    }
}