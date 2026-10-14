package com.web.doss;

import com.web.entity.User;
import com.web.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCK_TIME_DURATION = 2; // phút

    @Transactional
    public void increaseFailedAttempts(User user) {
        int newFailAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(newFailAttempts);
        if (newFailAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountNonLocked(false);
            user.setLockTime(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        user.setFailedAttempts(0);
        user.setLockTime(null);
        user.setAccountNonLocked(true);
        userRepository.save(user);
    }

    @Transactional
    public boolean unlockWhenTimeExpired(User user) {
        if (user.getLockTime() == null) return false;

        long minutes = Duration.between(user.getLockTime(), LocalDateTime.now()).toMinutes();
        if (minutes >= LOCK_TIME_DURATION) {
            resetFailedAttempts(user);
            return true;
        }
        return false;
    }
}
