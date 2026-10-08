package no.carlham.workouttracker.musclegroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(MuscleGroupController.class)
class MuscleGroupControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private MuscleGroupRepository repository;

    @Test
    void createReturns201ForNewName() {
        when(repository.existsByNameIgnoreCase("Forearms")).thenReturn(false);
        when(repository.save(any(MuscleGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(mvc.post().uri("/api/muscle-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Forearms" }
                        """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.name").isEqualTo("Forearms");
    }

    @Test
    void createReturns409ForExistingName() {
        when(repository.existsByNameIgnoreCase("chest")).thenReturn(true);

        assertThat(mvc.post().uri("/api/muscle-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "chest" }
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Muscle group 'chest' already exists");
        verify(repository, never()).save(any());
    }
}
