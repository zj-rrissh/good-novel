package com.ainovel.novel.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthorCapabilityDisabledTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldNotExposeAuthorNovelCreationEndpoint() throws Exception {
        String payload = """
                {
                  "title": "new novel",
                  "intro": "intro"
                }
                """;

        mockMvc.perform(post("/api/v1/novels")
                        .header("X-Client", "pc-web")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5000));
    }
}
