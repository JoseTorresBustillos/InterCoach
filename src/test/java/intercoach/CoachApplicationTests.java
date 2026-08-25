package intercoach;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class CoachApplicationTests {

    @Test
    void applicationDefinesEntrypoint() {
        assertThat(CoachApplication.class)
                .hasAnnotation(SpringBootApplication.class);
    }
}
