package com.jotangnote.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteMapperQueryTest {

    @Test
    void listQueryIsScopedToUserAndActiveNotes() throws Exception {
        String sql = selectSql(
                "findByUserId",
                Long.class,
                long.class,
                int.class
        );

        assertTrue(sql.contains("user_id = #{userId}"));
        assertTrue(sql.contains("is_deleted = 0"));
        assertTrue(sql.contains("LIMIT #{size} OFFSET #{offset}"));
    }

    @Test
    void countQueryUsesSameUserAndSoftDeleteScope() throws Exception {
        String sql = selectSql("countByUserId", Long.class);

        assertTrue(sql.contains("user_id = #{userId}"));
        assertTrue(sql.contains("is_deleted = 0"));
    }

    private String selectSql(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = NoteMapper.class.getMethod(methodName, parameterTypes);
        return String.join(" ", method.getAnnotation(Select.class).value());
    }
}
