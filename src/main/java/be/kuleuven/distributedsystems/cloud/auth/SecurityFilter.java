package be.kuleuven.distributedsystems.cloud.auth;

import be.kuleuven.distributedsystems.cloud.entities.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.protocol.ResponseContent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // TODO: (level 1) decode Identity Token and assign correct email and role
        //get the openId token and initialise variables
        String auth = extractTokenFromRequest(request);
        DecodedJWT token = null;
        String email = null;
        //make every user have the "user" role by default
        String role = "user";


        //check to ensure authorization token is not null
        if (auth != null){
            //take out the "Bearer" at the start of the string and decode
            String[] authParts = auth.split(" ");
            token = JWT.decode(authParts[1]);
            System.out.print(token);
            //extract the email and the
            email = String.valueOf(token.getClaim("email"));
            role = String.valueOf(token.getClaim("roles"));
        }
        //removing the array characters from the string "role"
        role = role.replace("[", "").replace("]", "").replace("\"", "");

        //create a new user with the "user" role or the "manager" role
        var otheruser = new User(email, new String[]{role});

        //given code : create the security context based on the user
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new FirebaseAuthentication(otheruser));
        filterChain.doFilter(request, response);


        // TODO: (level 2) verify Identity Token
        //var otheruser = new User("test@example.com", new String[]{});   //old code that was given
        //SecurityContext context = SecurityContextHolder.getContext();
        //context.setAuthentication(new FirebaseAuthentication(otheruser));

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
                System.out.println("here");
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

