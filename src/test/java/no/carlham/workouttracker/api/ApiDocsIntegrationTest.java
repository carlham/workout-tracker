package no.carlham.workouttracker.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import no.carlham.workouttracker.TestcontainersConfiguration;

/**
 * The API documentation is generated from the controllers at runtime, so check it actually builds
 * and is reachable without logging in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiDocsIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void openApiSpecDescribesEndpointsAndBearerAuth() {
        var json = assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatusOk()
                .bodyJson();

        json.extractingPath("$.paths").asMap()
                .containsKeys("/api/auth/login", "/api/exercises", "/api/templates", "/api/sessions/{id}/sets");
        json.extractingPath("$.components.securitySchemes.bearerAuth.scheme").isEqualTo("bearer");
        // Login doesn't require a token, so it overrides the global security requirement with an empty one
        json.extractingPath("$.paths['/api/auth/login'].post.security").asArray().isEmpty();
    }

    @Test
    void swaggerUiIsPublic() {
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }
}
