package com.example.study_cards.domain.user.service;

import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.entity.UserStatus;
import com.example.study_cards.domain.user.exception.UserErrorCode;
import com.example.study_cards.domain.user.exception.UserException;
import com.example.study_cards.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserDomainService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new UserException(UserErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .build();

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmailAndStatus(email, UserStatus.ACTIVE)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    public User findById(Long userId) {
        return userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    public User findByIdIncludingWithdrawn(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    public void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new UserException(UserErrorCode.INVALID_PASSWORD);
        }
    }

    public void changePassword(User user, String newPassword) {
        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndStatus(email, UserStatus.ACTIVE);
    }

    public void withdraw(User user) {
        if (user.isWithdrawn()) {
            throw new UserException(UserErrorCode.USER_ALREADY_WITHDRAWN);
        }
        if (user.isBanned()) {
            throw new UserException(UserErrorCode.USER_ALREADY_BANNED);
        }
        user.withdraw();
    }

    public void ban(User user) {
        if (user.isBanned()) {
            throw new UserException(UserErrorCode.USER_ALREADY_BANNED);
        }
        if (user.isWithdrawn()) {
            throw new UserException(UserErrorCode.USER_ALREADY_WITHDRAWN);
        }
        user.ban();
    }

    public Page<User> findUsers(UserStatus status, Pageable pageable) {
        if (status == null) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findAllByStatus(status, pageable);
    }

    public List<User> findAllPushEnabledUsersWithToken() {
        return userRepository.findAllByPushEnabledTrueAndFcmTokenIsNotNull();
    }
}
