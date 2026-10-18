package com.jotangnote.controller;

import com.jotangnote.entity.User;
import com.jotangnote.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private StubUserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = new StubUserService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .build();
    }

    @Test
    void returnsCurrentUserWithoutPassword() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setPassword("$2a$10$not-returned");

        userService.addUser(user);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 7L);

        mockMvc.perform(get("/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertEquals(7L, userService.lastRequestedId);
    }

    @Test
    void rejectsRequestWithoutLoggedInUserId() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));

        assertNull(userService.lastRequestedId);
    }

    @Test
    void rejectsSessionWhoseUserNoLongerExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 99L);

        mockMvc.perform(get("/users/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("登录状态已失效"));

        assertEquals(99L, userService.lastRequestedId);
    }

    private static final class StubUserService extends UserService {

        private final Map<Long, User> users = new HashMap<>();
        private Long lastRequestedId;

        private StubUserService() {
            super(null);
        }

        private void addUser(User user) {
            users.put(user.getId(), user);
        }

        @Override
        public User findById(Long id) {
            lastRequestedId = id;
            return users.get(id);
        }
    }
}
