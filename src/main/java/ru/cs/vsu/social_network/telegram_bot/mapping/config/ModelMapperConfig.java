package ru.cs.vsu.social_network.telegram_bot.mapping.config;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.cs.vsu.social_network.telegram_bot.dto.request.AddVisitorRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserUpdateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.*;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.entity.VisitorLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Конфигурационный класс для настройки ModelMapper.
 * Определяет правила маппинга между сущностями и DTO в контексте Telegram бота для тренажерного зала.
 * Обеспечивает корректное преобразование данных между слоями приложения.
 */
@Slf4j
@Configuration
public class ModelMapperConfig {

    /**
     * Создает и настраивает ModelMapper для преобразования сущностей в DTO.
     * Включает сопоставление полей, пропуск null значений и доступ к приватным полям.
     *
     * @return настроенный экземпляр ModelMapper с определенными правилами маппинга
     */
    @Bean
    public ModelMapper modelMapper() {
        log.info("MODEL_MAPPER_КОНФИГУРАЦИЯ_НАЧАЛО");

        ModelMapper modelMapper = new ModelMapper();

        configureConverters(modelMapper);
        configureUserMappings(modelMapper);
        configureVisitMappings(modelMapper);
        configureVisitorLogMappings(modelMapper);

        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true)
                .setFieldAccessLevel(
                        org.modelmapper.config.Configuration
                                .AccessLevel.PRIVATE);

        log.info("MODEL_MAPPER_КОНФИГУРАЦИЯ_УСПЕХ");
        return modelMapper;
    }

    /**
     * Настраивает конвертеры для преобразования типов.
     * Решает проблему маппинга final классов, таких как LocalDateTime.
     *
     * @param modelMapper экземпляр ModelMapper для настройки
     */
    private void configureConverters(ModelMapper modelMapper) {
        modelMapper.addConverter(new AbstractConverter<LocalDateTime, LocalDateTime>() {
            @Override
            protected LocalDateTime convert(LocalDateTime source) {
                return source;
            }
        });

        modelMapper.addConverter(new AbstractConverter<LocalDateTime, LocalDate>() {
            @Override
            protected LocalDate convert(LocalDateTime source) {
                return source != null ? source.toLocalDate() : null;
            }
        });

        modelMapper.addConverter(new AbstractConverter<LocalDate, LocalDate>() {
            @Override
            protected LocalDate convert(LocalDate source) {
                return source;
            }
        });
    }

    /**
     * Настраивает маппинг для сущности User и связанных DTO.
     * Определяет правила преобразования между User, UserCreateRequest, UserUpdateRequest и UserInfoResponse.
     *
     * @param modelMapper экземпляр ModelMapper для настройки
     */
    private void configureUserMappings(ModelMapper modelMapper) {
        modelMapper.addMappings(new PropertyMap<User, UserInfoResponse>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setTelegramId(source.getTelegramId());
                map().setUsername(source.getUsername());
                map().setFirstName(source.getFirstName());
                map().setLastName(source.getLastName());
                map().setDisplayName(source.getDisplayName());
                map().setRole(source.getRole());
                map().setRegisteredAt(source.getCreatedAt());
            }
        });

        modelMapper.addMappings(new PropertyMap<UserCreateRequest, User>() {
            @Override
            protected void configure() {
                map().setTelegramId(source.getTelegramId());
                map().setUsername(source.getUsername());
                map().setFirstName(source.getFirstName());
                map().setLastName(source.getLastName());
                map().setDisplayName(source.getDisplayName());
                map().setRole(source.getRole());
            }
        });

        modelMapper.addMappings(new PropertyMap<UserUpdateRequest, User>() {
            @Override
            protected void configure() {
                map().setDisplayName(source.getDisplayName());
                map().setRole(source.getRole());
            }
        });

        modelMapper.addMappings(new PropertyMap<UserInfoResponse, User>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setTelegramId(source.getTelegramId());
                map().setUsername(source.getUsername());
                map().setFirstName(source.getFirstName());
                map().setLastName(source.getLastName());
                map().setDisplayName(source.getDisplayName());
                map().setRole(source.getRole());
            }
        });
    }

    /**
     * Настраивает маппинг для сущности Visit и связанных DTO.
     * Определяет правила преобразования между Visit и VisitResponse.
     *
     * @param modelMapper экземпляр ModelMapper для настройки
     */
    private void configureVisitMappings(ModelMapper modelMapper) {
        modelMapper.addMappings(new PropertyMap<Visit, VisitResponse>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setUserId(source.getUser().getId());
                map().setUserDisplayName(source.getUser().getDisplayName());
                using(ctx -> ((LocalDateTime) ctx.getSource()).toLocalDate())
                        .map(source.getVisitDate(), destination.getVisitDate());
                map().setCreatedAt(source.getCreatedAt());
            }
        });
    }

    /**
     * Настраивает маппинг для сущности VisitorLog и связанных DTO.
     * Определяет правила преобразования между VisitorLog, AddVisitorRequest и VisitorLogResponse.
     *
     * @param modelMapper экземпляр ModelMapper для настройки
     */
    private void configureVisitorLogMappings(ModelMapper modelMapper) {
        modelMapper.addMappings(new PropertyMap<VisitorLog, VisitorLogResponse>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setVisitorsCount(source.getVisitorCount());
                map().setLogDate(source.getLogDate());
                map().setCreatedAt(source.getCreatedAt());
                map().setUpdatedAt(source.getUpdatedAt());
            }
        });

        modelMapper.addMappings(new PropertyMap<AddVisitorRequest, VisitorLog>() {
            @Override
            protected void configure() {
                skip(destination.getLogDate());
                skip(destination.getVisitorCount());
                skip(destination.getRawData());
            }
        });
    }
}