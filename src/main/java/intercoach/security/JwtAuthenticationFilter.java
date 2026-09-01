package intercoach.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final InterCoachUserDetailsService userDetailsService;
    private final SecurityErrorHandler securityErrorHandler;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            InterCoachUserDetailsService userDetailsService,
            SecurityErrorHandler securityErrorHandler
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        try {
            JwtClaims claims = jwtService.validateToken(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(claims.username());

                if (!userDetails.isEnabled()) {
                    throw new DisabledException("Account is inactive.");
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            securityErrorHandler.writeError(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired token.",
                    request.getRequestURI()
            );
        }
    }
}
