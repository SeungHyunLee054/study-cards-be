package com.example.study_cards.domain.user.entity;

import com.example.study_cards.domain.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    private String providerId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private Integer streak;

    @Column(nullable = false)
    private Double masteryRate;

    private LocalDate lastStudyDate;

    private String fcmToken;

    @Column(nullable = false)
    private Boolean pushEnabled = true;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Version
    private Long version;

    @Builder
    public User(String email, String password, String nickname, Set<Role> roles,
                OAuthProvider provider, String providerId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.roles = roles != null && !roles.isEmpty() ? new HashSet<>(roles) : new HashSet<>(Set.of(Role.ROLE_USER));
        this.provider = provider != null ? provider : OAuthProvider.LOCAL;
        this.providerId = providerId;
        this.streak = 0;
        this.masteryRate = 0.0;
        this.pushEnabled = true;
        this.emailVerified = false;
    }

    public boolean isOAuthUser() {
        return this.provider != OAuthProvider.LOCAL;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    public void updateStreak(LocalDate today) {
        if (lastStudyDate == null) {
            this.streak = 1;
        } else if (lastStudyDate.equals(today)) {
            return;
        } else if (lastStudyDate.equals(today.minusDays(1))) {
            this.streak++;
        } else {
            this.streak = 1;
        }
        this.lastStudyDate = today;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void removeFcmToken() {
        this.fcmToken = null;
    }

    public void updatePushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

}
