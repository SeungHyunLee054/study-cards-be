package com.example.study_cards.domain.user.repository;

import com.example.study_cards.domain.user.entity.OAuthProvider;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    boolean existsByEmail(String email);

    boolean existsByEmailAndStatus(String email, UserStatus status);

    Optional<User> findByProviderAndProviderIdAndStatus(OAuthProvider provider, String providerId, UserStatus status);

    Optional<User> findByIdAndStatus(Long id, UserStatus status);

    Page<User> findAllByStatus(UserStatus status, Pageable pageable);

    List<User> findAllByPushEnabledTrueAndFcmTokenIsNotNull();
}
