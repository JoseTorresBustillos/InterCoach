package InterCoach.security;

import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.AppUser;
import InterCoach.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class InterCoachUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public InterCoachUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username
                ));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
    }
}
