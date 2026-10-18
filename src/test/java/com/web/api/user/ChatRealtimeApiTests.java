package com.web.api.user;

import com.web.entity.User;
import com.web.repository.ChatRepository;
import com.web.utils.UserUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatRealtimeApiTests {
    @Test void anonymousChatReturnsUnauthorizedWithoutDatabaseQuery() {
        ChatRealtimeApi api = new ChatRealtimeApi(mock(SimpMessagingTemplate.class));
        ChatRepository repository = mock(ChatRepository.class);
        ReflectionTestUtils.setField(api, "userUtils", mock(UserUtils.class));
        ReflectionTestUtils.setField(api, "chatRepository", repository);
        assertEquals(HttpStatus.UNAUTHORIZED, api.myChat().getStatusCode());
        verifyNoInteractions(repository);
    }
    @Test void chatHistoryUsesAuthenticatedUserId() {
        ChatRealtimeApi api = new ChatRealtimeApi(mock(SimpMessagingTemplate.class));
        UserUtils users = mock(UserUtils.class);
        User user = new User(); user.setId(42L);
        when(users.getUserWithAuthority()).thenReturn(user);
        ChatRepository repository = mock(ChatRepository.class);
        when(repository.findByUser(42L)).thenReturn(List.of());
        ReflectionTestUtils.setField(api, "userUtils", users);
        ReflectionTestUtils.setField(api, "chatRepository", repository);
        assertEquals(HttpStatus.OK, api.myChat().getStatusCode());
        verify(repository).findByUser(42L);
    }
}
