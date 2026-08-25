package com.mikelam.taskapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndReadsBackATask() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Learn Docker\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Learn Docker"))
                .andExpect(jsonPath("$.done").value(false));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Learn Docker"));
    }

    @Test
    void rejectsABlankTitle() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForAnUnknownTask() throws Exception {
        mockMvc.perform(get("/api/tasks/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
