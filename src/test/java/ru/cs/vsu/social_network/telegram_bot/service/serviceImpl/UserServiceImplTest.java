package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import ru.cs.vsu.social_network.telegram_bot.testUtils.TestDataFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.UserFactory;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final Long TELEGRAM_ID = 123456789L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserEntityProvider userEntityProvider;
    @Mock
    private UserFactory userFactory;
    @Mock
    private UserValidator userValidator;
    @Mock
    private EntityMapper entityMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Регистрация пользователя - новый пользователь")
    void registerUser_whenNewUser_shouldCreateUser() {
        final UserCreateRequest createRequest = TestDataFactory.createUserCreateRequest(
                TELEGRAM_ID, "testuser", "Иван", "Иванов", "Иван");
        final User newUser = new User();
        newUser.setId(USER_ID);
        final User savedUser = new User();
        savedUser.setId(USER_ID);
        final UserInfoResponse expectedResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "testuser", "Иван", ROLE.USER);

        when(userEntityProvider.existsByTelegramId(TELEGRAM_ID)).thenReturn(false);
        when(userFactory.create(createRequest)).thenReturn(newUser);
        when(userRepository.save(newUser)).thenReturn(savedUser);
        when(entityMapper.map(savedUser, UserInfoResponse.class)).thenReturn(expectedResponse);

        final UserInfoResponse result = userService.registerUser(createRequest);

        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
        assertEquals(TELEGRAM_ID, result.getTelegramId());
        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("Регистрация пользователя - уже существует")
    void registerUser_whenUserExists_shouldReturnExistingUser() {
        final UserCreateRequest createRequest = TestDataFactory.createUserCreateRequest(
                TELEGRAM_ID, "testuser", "Иван", "Иванов", "Иван");
        final User existingUser = new User();
        existingUser.setId(USER_ID);
        final UserInfoResponse expectedResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "testuser", "Иван", ROLE.USER);

        when(userEntityProvider.existsByTelegramId(TELEGRAM_ID)).thenReturn(true);
        when(userEntityProvider.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(existingUser));
        when(entityMapper.map(existingUser, UserInfoResponse.class)).thenReturn(expectedResponse);

        final UserInfoResponse result = userService.registerUser(createRequest);

        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление пользователя - успешно")
    void updateUser_whenValidData_shouldUpdateUser() {
        final UserUpdateRequest updateRequest = TestDataFactory.createUserUpdateRequest(
                "НовоеИмя", ROLE.ADMIN);
        final User existingUser = new User();
        existingUser.setId(USER_ID);
        final User updatedUser = new User();
        updatedUser.setId(USER_ID);
        updatedUser.setDisplayName("НовоеИмя");
        updatedUser.setRole(ROLE.ADMIN);
        final UserInfoResponse expectedResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "testuser", "НовоеИмя", ROLE.ADMIN);

        when(userEntityProvider.getById(USER_ID)).thenReturn(existingUser);
        when(userRepository.save(existingUser)).thenReturn(updatedUser);
        when(entityMapper.map(updatedUser, UserInfoResponse.class)).thenReturn(expectedResponse);

        final UserInfoResponse result = userService.updateUser(USER_ID, updateRequest);

        assertNotNull(result);
        assertEquals("НовоеИмя", result.getDisplayName());
        assertEquals(ROLE.ADMIN, result.getRole());
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Получение пользователя по ID - успешно")
    void getUserById_whenUserExists_shouldReturnUser() {
        final User user = new User();
        user.setId(USER_ID);
        final UserInfoResponse expectedResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "testuser", "Иван", ROLE.USER);

        when(userEntityProvider.getById(USER_ID)).thenReturn(user);
        when(entityMapper.map(user, UserInfoResponse.class)).thenReturn(expectedResponse);

        final UserInfoResponse result = userService.getUserById(USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
        verify(userEntityProvider).getById(USER_ID);
    }

    @Test
    @DisplayName("Получение пользователя по Telegram ID - успешно")
    void getUserByTelegramId_whenUserExists_shouldReturnUser() {
        final User user = new User();
        user.setId(USER_ID);
        final UserInfoResponse expectedResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "testuser", "Иван", ROLE.USER);

        when(userEntityProvider.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));
        when(entityMapper.map(user, UserInfoResponse.class)).thenReturn(expectedResponse);

        final UserInfoResponse result = userService.getUserByTelegramId(TELEGRAM_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
        verify(userEntityProvider).findByTelegramId(TELEGRAM_ID);
    }

    @Test
    @DisplayName("Получение пользователей по роли - успешно")
    void getUsersByRole_whenUsersExist_shouldReturnPage() {
        final ROLE role = ROLE.USER;
        final PageRequest pageRequest =
                PageRequest.builder()
                        .pageNumber(0)
                        .size(10)
                        .sortBy("createdAt")
                        .direction(Sort.Direction.DESC)
                        .build();

        final List<User> users = List.of(new User(), new User());
        final Page<User> usersPage = new PageImpl<>(users);

        final Page<UserInfoResponse> expectedResponsePage = usersPage.map(user -> {
            return TestDataFactory.createUserInfoResponse(
                    user.getId() != null ? user.getId() : UUID.randomUUID(),
                    111L, "testuser", "Иван", ROLE.USER);
        });

        when(userRepository.findAllByRole(eq(role), any(Pageable.class))).thenReturn(usersPage);


        final PageResponse<UserInfoResponse> result = userService.getUsersByRole(role, pageRequest);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(userRepository).findAllByRole(eq(role), any(Pageable.class));
    }


    @Test
    @DisplayName("Изменение роли пользователя - успешно")
    void changeUserRole_whenAdmin_shouldChangeRole() {
        final ROLE newRole = ROLE.ADMIN;
        final User targetUser = new User();
        targetUser.setId(USER_ID);
        targetUser.setRole(ROLE.USER);
        final User updatedUser = new User();
        updatedUser.setId(USER_ID);
        updatedUser.setRole(ROLE.ADMIN);
        final UserInfoResponse expectedResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "testuser", "Иван", ROLE.ADMIN);

        doNothing().when(userValidator).validateAdminAccessById(ADMIN_ID);
        when(userEntityProvider.getById(USER_ID)).thenReturn(targetUser);
        when(userRepository.save(targetUser)).thenReturn(updatedUser);
        when(entityMapper.map(updatedUser, UserInfoResponse.class)).thenReturn(expectedResponse);

        final UserInfoResponse result = userService.changeUserRole(ADMIN_ID, USER_ID, newRole);

        assertNotNull(result);
        assertEquals(ROLE.ADMIN, result.getRole());
        verify(userValidator).validateAdminAccessById(ADMIN_ID);
        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("Обновление отображаемого имени - успешно")
    void updateDisplayName_whenValidName_shouldUpdateName() {
        final String displayName = "НовоеИмя";
        final User user = new User();
        user.setId(USER_ID);
        user.setDisplayName("СтароеИмя");
        final User updatedUser = new User();
        updatedUser.setId(USER_ID);
        updatedUser.setDisplayName("НовоеИмя");
        final UserInfoResponse expectedResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "testuser", "НовоеИмя", ROLE.USER);

        when(userEntityProvider.getById(USER_ID)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(updatedUser);
        when(entityMapper.map(updatedUser, UserInfoResponse.class)).thenReturn(expectedResponse);

        final UserInfoResponse result = userService.updateDisplayName(USER_ID, displayName);

        assertNotNull(result);
        assertEquals("НовоеИмя", result.getDisplayName());
        verify(userRepository).save(user);
    }
}