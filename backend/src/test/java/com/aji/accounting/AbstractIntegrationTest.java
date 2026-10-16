package com.aji.accounting;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Basis integration test memakai MySQL asli via Testcontainers.
 * Container singleton (dipakai bersama semua kelas test) agar context Spring yang di-cache tetap valid.
 * Otomatis di-skip bila Docker tidak tersedia.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("accounting_test")
            .withCommand("--default-time-zone=+07:00");

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            MYSQL.start();
        }
    }
}
