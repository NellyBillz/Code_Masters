package za.codemaster.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionChecker implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            // Run a lightweight test query against Supabase
            String result = jdbcTemplate.queryForObject("SELECT version();", String.class);
            System.out.println("=================================================");
            System.out.println(">>> SUCCESS: CONNECTED TO SUPABASE POSTGRESQL! <<<");
            System.out.println(">>> Version: " + result);
            System.out.println("=================================================");
        } catch (Exception e) {
            System.err.println("=================================================");
            System.err.println(">>> FAILED TO CONNECT TO SUPABASE <<<");
            System.err.println("Error: " + e.getMessage());
            System.err.println("=================================================");
        }
    }
}