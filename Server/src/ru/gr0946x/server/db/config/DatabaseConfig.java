package ru.gr0946x.server.db.config;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Конфигурационный класс Spring для настройки уровня доступа
 * к данным (JPA/Hibernate).
 * <p>
 * Отвечает за инициализацию инфраструктуры работы с базой данных:
 * <ul>
 *   <li>Загружает параметры подключения и настройки ORM из
 *   {@code application.properties}</li>
 *   <li>Создаёт и настраивает {@link DataSource}
 *   для управления JDBC-соединениями</li>
 *   <li>Инициализирует {@link EntityManagerFactory}
 *   с адаптером Hibernate</li>
 *   <li>Настраивает менеджер транзакций для поддержки
 *   {@code @Transactional}</li>
 *   <li>Автоматически регистрирует репозитории и сервисы
 *   в пакете {@code ru.smak.db}</li>
 * </ul>
 * <p>
 * Класс помечен как корневая конфигурация и используется при запуске приложения
 *
 * @author Маклецов С. В.
 */
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "ru.gr0946x.server.db")
@EnableJpaRepositories(basePackages = "ru.gr0946x.server.db.repository")
@PropertySource("classpath:application.properties")
public class DatabaseConfig {

    /** JDBC-адрес подключения к базе данных */
    @Value("${db.url}")
    private String url;

    /** Полное имя класса JDBC-драйвера */
    @Value("${db.driver}")
    private String driver;

    /** Имя пользователя БД */
    @Value("${db.user}")
    private String user;

    /** Пароль пользователя БД */
    @Value("${db.pass}")
    private String password;

    /** Стратегия управления схемой БД: update, create, validate, none */
    @Value("${jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    /** Флаг вывода SQL-запросов в консоль */
    @Value("${jpa.show-sql}")
    private boolean showSql;

    /** Флаг форматирования SQL-запросов для удобного чтения */
    @Value("${jpa.format-sql}")
    private boolean formatSql;

    /**
     * Создаёт и настраивает источник данных (DataSource) для JDBC-соединений.
     * <p>
     * Используется {@link DriverManagerDataSource},
     * который создаёт новое соединение
     * при каждом запросе. Подходит для учебных и тестовых проектов.
     *
     * @return настроенный экземпляр {@link DataSource}
     */
    @Bean
    public DataSource dataSource() {
        var ds = new DriverManagerDataSource(url, user, password);
        ds.setDriverClassName(driver);
        return ds;
    }

    /**
     * Инициализирует фабрику менеджеров сущностей JPA (EntityManagerFactory).
     * <p>
     * Является центральным компонентом JPA:
     * управляет жизненным циклом {@code EntityManager},
     * кэширует метаданные сущностей и применяет настройки Hibernate.
     * <p>
     * Метод принимает {@link DataSource} через параметр,
     * что позволяет Spring внедрить уже созданный бин,
     * избегая прямого вызова методов внутри {@code @Configuration}.
     *
     * @param dataSource предварительно созданный источник данных
     * @return настроенная фабрика {@link LocalContainerEntityManagerFactoryBean}
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean
    entityManagerFactory(DataSource dataSource) {
        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);

        // Указывает пакет, где Spring будет искать классы, помеченные @Entity
        em.setPackagesToScan("ru.gr0946x.server.db.entity");

        // Подключает адаптер Hibernate для работы с JPA
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        var props = new Properties();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.show_sql", showSql);
        props.put("hibernate.format_sql", formatSql);

        em.setJpaProperties(props);
        return em;
    }

    /**
     * Создаёт менеджер транзакций для JPA.
     * <p>
     * Обеспечивает поддержку аннотации {@code @Transactional},
     * управление границами транзакций,
     * commit/rollback и интеграцию с Spring AOP.
     * <p>
     * Зависит от {@link EntityManagerFactory},
     * который внедряется автоматически по типу.
     *
     * @param emf фабрика менеджеров сущностей
     * @return менеджер транзакций {@link PlatformTransactionManager}
     */
    @Bean
    public PlatformTransactionManager transactionManager(
            EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
