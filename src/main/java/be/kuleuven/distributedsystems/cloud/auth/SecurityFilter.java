package be.kuleuven.distributedsystems.cloud.auth;

import be.kuleuven.distributedsystems.cloud.entities.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.*;
import be.kuleuven.distributedsystems.cloud.auth.*;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // TODO: (level 1) decode Identity Token and assign correct email and role
        // get the openId token and initialise variables
        String auth = extractTokenFromRequest(request);
        DecodedJWT token = null;
        String email = null;
        // make every user have the "user" role by default
        String role = "user";

        // check to ensure authorization token is not null
        if (auth != null){
            // take out the "Bearer" at the start of the string and decode, extracting role and email
            String[] authParts = auth.split(" ");
            token = JWT.decode(authParts[1]);
            email = String.valueOf(token.getClaim("email"));
            email = email.replace("\"", "");
            role = String.valueOf(token.getClaim("roles"));
        }

        // removing the array characters from the string "role"
        role = role.replace("[", "").replace("]", "").replace("\"", "");
        var otheruser = new User(email, new String[]{role});

        // TODO: (level 2) verify Identity Token
        String projectId = "fos-jm-cloud-app";
        PublicKeyFetcher pubKeyFetcher = new PublicKeyFetcher();

        try {
            // get kid from token, get public keys from google endpoint
            var kid = token.getKeyId();
            Map<String, String> publicKeys = pubKeyFetcher.fetchPublicKeys();
            var pubKey = publicKeys.get(kid);
            RSAPublicKey pubKeyConverted = null;
            try {
                pubKeyConverted = PublicKeyFetcher.convertStringToRSAPublicKey(pubKey);
            } catch (Exception e) {
                throw new RuntimeException("Error converting X509 cert to RSA key");
            }

            Algorithm algo = Algorithm.RSA256(pubKeyConverted, null);
            DecodedJWT jwt = JWT.require(algo)
                    .withIssuer("https://securetoken.google.com/" + projectId)
                    .build()
                    .verify(token);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying token");
        }

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new FirebaseAuthentication(otheruser));
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/api");
    }

    public static User getUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private static class FirebaseAuthentication implements Authentication {
        private final User user;

        FirebaseAuthentication(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            //if the user is a manager assign them that ROLE
            if (user.isManager()){
                return Collections.singleton(new SimpleGrantedAuthority("ROLE_manager"));
            }
            else //if user is not a manager assign them the user ROLE
                return Collections.singleton(new SimpleGrantedAuthority("ROLE_user"));
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public User getPrincipal() {
            return this.user;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean b) throws IllegalArgumentException {
        }

        @Override
        public String getName() {
            return null;
        }
    }
}