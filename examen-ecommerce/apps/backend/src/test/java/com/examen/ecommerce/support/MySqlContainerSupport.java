package com.examen.ecommerce.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Contenedor MySQL 5.7.44 compartido (singleton) por todas las pruebas con BD.
 * 01_schema.sql y 02_seed_data.sql (con DROP TABLE) SOLO se ejecutan aquí, nunca en dev.
 */
public abstract class MySqlContainerSupport {

    public static final String SCHEMA_SCRIPT = "classpath:db/01_schema.sql";
    public static final String SEED_SCRIPT = "classpath:db/02_seed_data.sql";

    private static final String INIT_DIR = "/docker-entrypoint-initdb.d/";

    @SuppressWarnings("resource")
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:5.7.44"))
            .withDatabaseName("kata_ecommerce")
            .withUsername("kata")
            .withPassword("kata")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci",
                    "--sql-mode=STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,"
                            + "NO_ENGINE_SUBSTITUTION")
            .withEnv("TZ", "America/Bogota")
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true")
            .withUrlParam("characterEncoding", "UTF-8")
            .withUrlParam("tinyInt1isBit", "false")
            .withUrlParam("connectionTimeZone", "America/Bogota")
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/01_schema.sql"),
                    INIT_DIR + "01_schema.sql")
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/02_seed_data.sql"),
                    INIT_DIR + "02_seed_data.sql");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
