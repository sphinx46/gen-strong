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
import ru.cs.vsu.social_network.telegram_bot.dto.request.pageable.PageRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.pageable.PageResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.mapping.EntityMapper;
import ru.cs.vsu.social_network.telegram_bot.provider.UserEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.VisitRepository;
import ru.cs.vsu.social_network.telegram_bot.testUtils.TestDataFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.VisitFactory;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final Long TELEGRAM_ID = 123456789L;

    @Mock
    private VisitRepository visitRepository;
    @Mock
    private VisitEntityProvider visitEntityProvider;
    @Mock
    private UserEntityProvider userEntityProvider;
    @Mock
    private VisitFactory visitFactory;
    @Mock
    private VisitValidator visitValidator;
    @Mock
    private EntityMapper entityMapper;

    @InjectMocks
    private VisitServiceImpl visitService;

    @Test
    @DisplayName("Создание посещения - успешно")
    void createVisit_whenValidUser_shouldCreateVisit() {
        final Visit newVisit = new Visit();
        newVisit.setId(VISIT_ID);
        final Visit savedVisit = new Visit();
        savedVisit.setId(VISIT_ID);
        final VisitResponse expectedResponse = TestDataFactory.createVisitResponse(
                VISIT_ID, USER_ID, LocalDate.now());

        doNothing().when(visitValidator).validateRepeatVisit(USER_ID);
        when(visitFactory.create(USER_ID, null)).thenReturn(newVisit);
        when(visitRepository.save(newVisit)).thenReturn(savedVisit);
        when(entityMapper.map(savedVisit, VisitResponse.class)).thenReturn(expectedResponse);

        final VisitResponse result = visitService.createVisit(USER_ID);

        assertNotNull(result);
        assertEquals(VISIT_ID, result.getId());
        assertEquals(USER_ID, result.getUserId());
        verify(visitValidator).validateRepeatVisit(USER_ID);
        verify(visitRepository).save(newVisit);
    }

    @Test
    @DisplayName("Создание посещения по Telegram ID - успешно")
    void createVisitByTelegramId_whenValidUser_shouldCreateVisit() {
        final User user = new User();
        user.setId(USER_ID);
        final Visit newVisit = new Visit();
        newVisit.setId(VISIT_ID);
        final Visit savedVisit = new Visit();
        savedVisit.setId(VISIT_ID);
        final VisitResponse expectedResponse = TestDataFactory.createVisitResponse(
                VISIT_ID, USER_ID, LocalDate.now());

        when(userEntityProvider.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));
        doNothing().when(visitValidator).validateRepeatVisit(USER_ID);
        when(visitFactory.createForUser(user)).thenReturn(newVisit);
        when(visitRepository.save(newVisit)).thenReturn(savedVisit);
        when(entityMapper.map(savedVisit, VisitResponse.class)).thenReturn(expectedResponse);

        final VisitResponse result = visitService.createVisitByTelegramId(TELEGRAM_ID);

        assertNotNull(result);
        assertEquals(VISIT_ID, result.getId());
        verify(visitValidator).validateRepeatVisit(USER_ID);
        verify(visitRepository).save(newVisit);
    }

    @Test
    @DisplayName("Получение посещения по ID - успешно")
    void getVisitById_whenVisitExists_shouldReturnVisit() {
        final Visit visit = new Visit();
        visit.setId(VISIT_ID);
        final VisitResponse expectedResponse = TestDataFactory.createVisitResponse(
                VISIT_ID, USER_ID, LocalDate.now());

        when(visitEntityProvider.getById(VISIT_ID)).thenReturn(visit);
        when(entityMapper.map(visit, VisitResponse.class)).thenReturn(expectedResponse);

        final VisitResponse result = visitService.getVisitById(VISIT_ID);

        assertNotNull(result);
        assertEquals(VISIT_ID, result.getId());
        verify(visitEntityProvider).getById(VISIT_ID);
    }

    @Test
    @DisplayName("Проверка посещения сегодня - пользователь посещал")
    void hasUserVisitedToday_whenVisited_shouldReturnTrue() {
        final LocalDate today = LocalDate.now();

        when(visitEntityProvider.existsByUserIdAndDate(USER_ID, today)).thenReturn(true);

        final boolean result = visitService.hasUserVisitedToday(USER_ID);

        assertTrue(result);
        verify(visitEntityProvider).existsByUserIdAndDate(USER_ID, today);
    }

    @Test
    @DisplayName("Проверка посещения сегодня - пользователь не посещал")
    void hasUserVisitedToday_whenNotVisited_shouldReturnFalse() {
        final LocalDate today = LocalDate.now();

        when(visitEntityProvider.existsByUserIdAndDate(USER_ID, today)).thenReturn(false);

        final boolean result = visitService.hasUserVisitedToday(USER_ID);

        assertFalse(result);
        verify(visitEntityProvider).existsByUserIdAndDate(USER_ID, today);
    }

    @Test
    @DisplayName("Подсчет посещений по дате - успешно")
    void getVisitCountByDate_shouldReturnCount() {
        final LocalDate date = LocalDate.of(2025, 12, 6);

        when(visitEntityProvider.countByDate(date)).thenReturn(15L);

        final long result = visitService.getVisitCountByDate(date);

        assertEquals(15L, result);
        verify(visitEntityProvider).countByDate(date);
    }

    @Test
    @DisplayName("Подсчет посещений по пользователю - успешно")
    void getVisitCountByUser_shouldReturnCount() {
        final User user = new User();
        user.setId(USER_ID);
        final List<Visit> visits = List.of(new Visit(), new Visit(), new Visit());
        final Page<Visit> visitsPage = new PageImpl<>(visits);

        when(userEntityProvider.getById(USER_ID)).thenReturn(user);
        when(visitRepository.findAllByUser(eq(user), any(Pageable.class))).thenReturn(visitsPage);

        final long result = visitService.getVisitCountByUser(USER_ID);

        assertEquals(3L, result);
        verify(visitRepository).findAllByUser(eq(user), any(Pageable.class));
    }

    @Test
    @DisplayName("Получение посещений по дате - успешно")
    void getVisitsByDate_whenVisitsExist_shouldReturnPage() {
        final LocalDate date = LocalDate.of(2025, 12, 6);
        final PageRequest pageRequest = PageRequest.builder()
                .pageNumber(0)
                .size(10)
                .sortBy("visitDate")
                .direction(Sort.Direction.DESC)
                .build();

        final List<Visit> visits = List.of(new Visit(), new Visit());
        final Page<Visit> visitsPage = new PageImpl<>(visits);

        when(visitRepository.findAllByDate(eq(date), any(Pageable.class))).thenReturn(visitsPage);

        final PageResponse<VisitResponse> result = visitService.getVisitsByDate(date, pageRequest);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(visitRepository).findAllByDate(eq(date), any(Pageable.class));
    }

    @Test
    @DisplayName("Получение посещений по пользователю - успешно")
    void getVisitsByUser_whenVisitsExist_shouldReturnPage() {
        final PageRequest pageRequest = PageRequest.builder()
                .pageNumber(0)
                .size(10)
                .sortBy("visitDate")
                .direction(Sort.Direction.DESC)
                .build();

        final User user = new User();
        user.setId(USER_ID);

        final List<Visit> visits = List.of(new Visit(), new Visit());
        final Page<Visit> visitsPage = new PageImpl<>(visits);

        when(userEntityProvider.getById(USER_ID)).thenReturn(user);
        when(visitRepository.findAllByUser(eq(user), any(Pageable.class))).thenReturn(visitsPage);

        final PageResponse<VisitResponse> result = visitService.getVisitsByUser(USER_ID, pageRequest);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(visitRepository).findAllByUser(eq(user), any(Pageable.class));
    }
}