package com.connectors.pos.security;

import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepo;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

Users user = userRepo.findByEmail(email)
        .orElseThrow(()->new UsernameNotFoundException("email wasn't found"));
return new UserPrincipal(user);

    }
}
