package com.jotangnote.controller;

import com.jotangnote.entity.Note;
import com.jotangnote.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoteControllerTest {

    private StubNoteService noteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        noteService = new StubNoteService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NoteController(noteService, null))
                .build();
    }

    @Test
    void returnsCurrentUsersNotesWithPaginationMetadata() throws Exception {
        Note note = new Note();
        note.setId(11L);
        note.setUserId(7L);
        note.setTitle("第一篇笔记");
        note.setContent("正文");
        noteService.notes = List.of(note);
        noteService.total = 12;

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 7L);

        mockMvc.perform(get("/notes")
                        .param("page", "2")
                        .param("size", "5")
                        .param("userId", "99")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes[0].id").value(11))
                .andExpect(jsonPath("$.notes[0].title").value("第一篇笔记"))
                .andExpect(jsonPath("$.notes[0].userId").doesNotExist())
                .andExpect(jsonPath("$.notes[0].isDeleted").doesNotExist())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.totalPages").value(3));

        assertEquals(7L, noteService.requestedUserId);
        assertEquals(5L, noteService.requestedOffset);
        assertEquals(5, noteService.requestedSize);
    }

    @Test
    void normalizesInvalidPaginationParameters() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 7L);

        mockMvc.perform(get("/notes")
                        .param("page", "-3")
                        .param("size", "1000")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalPages").value(0));

        assertEquals(0L, noteService.requestedOffset);
        assertEquals(100, noteService.requestedSize);
    }

    @Test
    void rejectsRequestWithoutLoggedInUserId() throws Exception {
        mockMvc.perform(get("/notes"))
                .andExpect(status().isUnauthorized());

        assertNull(noteService.requestedUserId);
    }

    private static final class StubNoteService extends NoteService {

        private List<Note> notes = List.of();
        private long total;
        private Long requestedUserId;
        private long requestedOffset;
        private int requestedSize;

        private StubNoteService() {
            super(null, null, null);
        }

        @Override
        public List<Note> getNotesByUserId(Long userId, long offset, int size) {
            requestedUserId = userId;
            requestedOffset = offset;
            requestedSize = size;
            return notes;
        }

        @Override
        public long countNotesByUserId(Long userId) {
            requestedUserId = userId;
            return total;
        }
    }
}
