package za.codemaster.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!prod")
public class DatabaseConnectionChecker implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            // Query current database name, active user, and PostgreSQL version
            String dbName = jdbcTemplate.queryForObject("SELECT current_database();", String.class);
            String currentUser = jdbcTemplate.queryForObject("SELECT current_user;", String.class);
            String version = jdbcTemplate.queryForObject("SELECT version();", String.class);

            // Query existing public tables to verify Flyway migration
            List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;",
                String.class
            );

            System.out.println("=================================================");
            System.out.println(">>> SUCCESS: CONNECTED TO LOCAL POSTGRESQL! <<<");
            System.out.println(">>> Database : " + dbName);
            System.out.println(">>> User     : " + currentUser);
            System.out.println(">>> Version  : " + version);
            System.out.println(">>> Tables   : " + tables);
            System.out.println("=================================================");
        } catch (Exception e) {
            System.err.println("=================================================");
            System.err.println(">>> FAILED TO CONNECT TO LOCAL POSTGRESQL <<<");
            System.err.println("Error: " + e.getMessage());
            System.err.println("=================================================");
        }
    }
}