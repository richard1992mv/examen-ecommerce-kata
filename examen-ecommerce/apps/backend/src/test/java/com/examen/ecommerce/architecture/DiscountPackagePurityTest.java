package com.examen.ecommerce.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Reglas de capas verificables: service/discount es Java puro; los controllers no usan repositorios. */
class DiscountPackagePurityTest {

    private static final Path MAIN = Path.of("src/main/java/com/examen/ecommerce");

    @Test
    void discountPackageDoesNotImportSpringJpaOrEntities() throws IOException {
        List<String> offending = importsMatching(MAIN.resolve("service/discount"),
                "import org.springframework", "import jakarta.persistence", "import com.examen.ecommerce.entity",
                "import org.hibernate");
        assertThat(offending).isEmpty();
    }

    @Test
    void controllersDoNotAccessRepositories() throws IOException {
        assertThat(importsMatching(MAIN.resolve("controller"), "import com.examen.ecommerce.repository")).isEmpty();
    }

    @Test
    void noFieldInjection() throws IOException {
        assertThat(importsMatching(MAIN, "@Autowired")).isEmpty();
    }

    private static List<String> importsMatching(Path dir, String... forbidden) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(DiscountPackagePurityTest::lines)
                    .filter(line -> Stream.of(forbidden).anyMatch(line::contains))
                    .toList();
        }
    }

    private static Stream<String> lines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8).stream().map(line -> file.getFileName() + ": " + line);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
