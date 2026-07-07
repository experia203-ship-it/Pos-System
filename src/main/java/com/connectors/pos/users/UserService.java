package com.connectors.pos.users;

import com.connectors.pos.exceptions.RoleNotFoundException;
import com.connectors.pos.exceptions.UserEmailAlreadyExistsException;
import com.connectors.pos.exceptions.UserNameAlreadyExistsException;
import com.connectors.pos.users.userdtos.CreateUserDto;
import com.connectors.pos.users.userdtos.UserMapper;
import com.connectors.pos.users.userdtos.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepo;
    private final UserMapper userMapper;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    /**
     * this service should contain 2 methods
     * 1 for create a new user
     * 2 add role to existing user
     * 3 passwords must be hashed
     */

@Transactional
    public UserResponseDto createUser(CreateUserDto create){

        if(userRepo.existsByEmail(create.email())){

     throw new UserEmailAlreadyExistsException("email already exists");
        }

        if(userRepo.existsByName(create.name())){

            throw new UserNameAlreadyExistsException("name already exists");
        }

Users user = userMapper.toEntity(create);


    if(user.getRoles()==null||user.getRoles().isEmpty()){
Roles role = roleRepo.findByName("ROLE_USER")
                .orElseThrow(()->new RoleNotFoundException("role not found"));
        user.getRoles().add(role);
    }
user.setPassword(encoder.encode(create.password()));

    Users saved = userRepo.save(user);

    return userMapper.toResponse(saved);
    }



    @Transactional
    public void assignRoleToUser(Long userId,Long roleId){
    Users user = userRepo.findById(userId).
            orElseThrow(()->new UsernameNotFoundException("user wasn't found"));

            Roles role = roleRepo.findById(roleId)
                    .orElseThrow(()-> new RoleNotFoundException("role wasn't found"));


  Set<Roles> roles =user.getRoles();
  roles.add(role);
    }

}
