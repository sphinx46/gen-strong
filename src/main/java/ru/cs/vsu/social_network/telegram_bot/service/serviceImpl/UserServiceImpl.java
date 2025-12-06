package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserUpdateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.request.pageable.PageRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.pageable.PageResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.mapping.EntityMapper;
import ru.cs.vsu.social_network.telegram_bot.provider.UserEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.UserRepository;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.UserFactory;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.util.UUID;

/**
 * Реализация сервиса для управления пользователями.
 * Обеспечивает бизнес-логику регистрации, обновления и управления пользователями бота.
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final String SERVICE_NAME = "ПОЛЬЗОВАТЕЛЬ_СЕРВИС";

    private final UserRepository userRepository;
    private final UserEntityProvider userEntityProvider;
    private final UserFactory userFactory;
    private final UserValidator userValidator;
    private final EntityMapper entityMapper;

    public UserServiceImpl(final UserRepository userRepository,
                           final UserEntityProvider userEntityProvider,
                           final UserFactory userFactory,
                           final UserValidator userValidator,
                           final EntityMapper entityMapper) {
        this.userRepository = userRepository;
        this.userEntityProvider = userEntityProvider;
        this.userFactory = userFactory;
        this.userValidator = userValidator;
        this.entityMapper = entityMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserInfoResponse registerUser(final UserCreateRequest createRequest) {
        log.info("{}_РЕГИСТРАЦИЯ_НАЧАЛО: регистрация пользователя с Telegram ID: {}",
                SERVICE_NAME, createRequest.getTelegramId());

        if (userEntityProvider.existsByTelegramId(createRequest.getTelegramId())) {
            final User existingUser = userEntityProvider.findByTelegramId(createRequest.getTelegramId())
                    .orElseThrow(() -> new IllegalStateException("Пользователь должен существовать"));

            log.info("{}_РЕГИСТРАЦИЯ_УЖЕ_СУЩЕСТВУЕТ: пользователь с Telegram ID {} уже зарегистрирован",
                    SERVICE_NAME, createRequest.getTelegramId());

            return entityMapper.map(existingUser, UserInfoResponse.class);
        }

        final User user = userFactory.create(createRequest);
        final User savedUser = userRepository.save(user);

        log.info("{}_РЕГИСТРАЦИЯ_УСПЕХ: пользователь успешно зарегистрирован с ID: {}",
                SERVICE_NAME, savedUser.getId());

        return entityMapper.map(savedUser, UserInfoResponse.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserInfoResponse updateUser(final UUID userId, final UserUpdateRequest updateRequest) {
        log.info("{}_ОБНОВЛЕНИЕ_НАЧАЛО: обновление пользователя с ID: {}",
                SERVICE_NAME, userId);

        final User user = userEntityProvider.getById(userId);

        if (updateRequest.getDisplayName() != null) {
            user.setDisplayName(updateRequest.getDisplayName());
        }

        if (updateRequest.getRole() != null) {
            user.setRole(updateRequest.getRole());
        }

        final User updatedUser = userRepository.save(user);

        log.info("{}_ОБНОВЛЕНИЕ_УСПЕХ: пользователь с ID: {} успешно обновлен",
                SERVICE_NAME, userId);

        return entityMapper.map(updatedUser, UserInfoResponse.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInfoResponse getUserById(final UUID userId) {
        log.info("{}_ПОЛУЧЕНИЕ_ПО_ID_НАЧАЛО: запрос пользователя с ID: {}",
                SERVICE_NAME, userId);

        final User user = userEntityProvider.getById(userId);
        final UserInfoResponse response = entityMapper.map(user, UserInfoResponse.class);

        log.info("{}_ПОЛУЧЕНИЕ_ПО_ID_УСПЕХ: пользователь с ID: {} найден",
                SERVICE_NAME, userId);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInfoResponse getUserByTelegramId(final Long telegramId) {
        log.info("{}_ПОЛУЧЕНИЕ_ПО_TELEGRAM_ID_НАЧАЛО: запрос пользователя с Telegram ID: {}",
                SERVICE_NAME, telegramId);

        final User user = userEntityProvider.findByTelegramId(telegramId)
                .orElseThrow(() -> {
                    log.error("{}_ПОЛУЧЕНИЕ_ПО_TELEGRAM_ID_ОШИБКА: " +
                            "пользователь с Telegram ID {} не найден", SERVICE_NAME, telegramId);
                    return new RuntimeException("Пользователь не найден");
                });

        final UserInfoResponse response = entityMapper.map(user, UserInfoResponse.class);

        log.info("{}_ПОЛУЧЕНИЕ_ПО_TELEGRAM_ID_УСПЕХ: пользователь с Telegram ID: {} найден",
                SERVICE_NAME, telegramId);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<UserInfoResponse> getUsersByRole(final ROLE role, final PageRequest pageRequest) {
        log.info("{}_ПОЛУЧЕНИЕ_ПО_РОЛИ_НАЧАЛО: запрос пользователей с ролью: {}, страница: {}",
                SERVICE_NAME, role, pageRequest.getPageNumber());

        final Pageable pageable = pageRequest.toPageable();
        final Page<User> usersPage = userRepository.findAllByRole(role, pageable);
        final Page<UserInfoResponse> responsePage = usersPage.map(user ->
                entityMapper.map(user, UserInfoResponse.class));

        log.info("{}_ПОЛУЧЕНИЕ_ПО_РОЛИ_УСПЕХ: найдено {} пользователей с ролью: {}",
                SERVICE_NAME, usersPage.getTotalElements(), role);

        return PageResponse.of(responsePage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserInfoResponse changeUserRole(final UUID adminUserId, final UUID targetUserId, final ROLE newRole) {
        log.info("{}_ИЗМЕНЕНИЕ_РОЛИ_НАЧАЛО: администратор {} изменяет роль пользователя {} на {}",
                SERVICE_NAME, adminUserId, targetUserId, newRole);

        userValidator.validateAdminAccessById(adminUserId);

        final User targetUser = userEntityProvider.getById(targetUserId);
        targetUser.setRole(newRole);

        final User updatedUser = userRepository.save(targetUser);

        log.info("{}_ИЗМЕНЕНИЕ_РОЛИ_УСПЕХ: роль пользователя {} успешно изменена на {}",
                SERVICE_NAME, targetUserId, newRole);

        return entityMapper.map(updatedUser, UserInfoResponse.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserInfoResponse updateDisplayName(final UUID userId, final String displayName) {
        log.info("{}_ОБНОВЛЕНИЕ_ИМЕНИ_НАЧАЛО: обновление отображаемого имени для пользователя {} на '{}'",
                SERVICE_NAME, userId, displayName);

        final User user = userEntityProvider.getById(userId);
        user.setDisplayName(displayName);

        final User updatedUser = userRepository.save(user);

        log.info("{}_ОБНОВЛЕНИЕ_ИМЕНИ_УСПЕХ: отображаемое имя пользователя {} успешно обновлено",
                SERVICE_NAME, userId);

        return entityMapper.map(updatedUser, UserInfoResponse.class);
    }
}