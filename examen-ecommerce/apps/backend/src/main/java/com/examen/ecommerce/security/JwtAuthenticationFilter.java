package com.examen.ecommerce.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee "Authorization: Bearer &lt;token&gt;" y, si es válido, autentica la petición.
 * No responde errores: si el token falta o es inválido deja la petición sin autenticar
 * y el {@link JwtAuthenticationEntryPoint} responde 401 donde se requiera autenticación.
 * No se registra como @Component para que solo corra dentro de la cadena de Spring Security.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String BEARER_PREFIX = "Bearer ";
    /** Atributo de request con el motivo del rechazo, usado por el entry point. */
    static final String JWT_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".error";

    enum TokenError { INVALID, EXPIRED }

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            String username = jwtService.validateAndGetUsername(token);
            UserDetails user = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (ExpiredJwtException ex) {
            reject(request, TokenError.EXPIRED);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            reject(request, TokenError.INVALID);
        }

        chain.doFilter(request, response);
    }

    private static void reject(HttpServletRequest request, TokenError error) {
        SecurityContextHolder.clearContext();
        request.setAttribute(JWT_ERROR_ATTRIBUTE, error);
    }
}
