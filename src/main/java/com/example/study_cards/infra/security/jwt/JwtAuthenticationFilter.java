package com.example.study_cards.infra.security.jwt;

import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.UserStatus;
import com.example.study_cards.domain.user.repository.UserRepository;
import com.example.study_cards.infra.security.exception.JwtErrorCode;
import com.example.study_cards.infra.security.exception.JwtException;
import com.example.study_cards.infra.redis.service.TokenBlacklistService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (token != null) {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    throw new JwtException(JwtErrorCode.BLACKLISTED_TOKEN);
                }

                jwtTokenProvider.validateToken(token);

                Long userId = jwtTokenProvider.getUserId(token);
                userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                        .orElseThrow(() -> new JwtException(JwtErrorCode.INVALID_TOKEN));
                String email = jwtTokenProvider.getEmail(token);
                Set<Role> roles = jwtTokenProvider.getRoles(token);

                CustomUserDetails userDetails = new CustomUserDetails(userId, email, roles);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            jwtAuthenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(e.getErrorCode().getMessage(), e)
            );
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
