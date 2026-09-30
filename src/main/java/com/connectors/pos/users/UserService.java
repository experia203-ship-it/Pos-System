package com.connectors.pos.users;

import com.connectors.pos.audit.AuditLogService;
import com.connectors.pos.exceptions.RoleNotFoundException;
import com.connectors.pos.exceptions.UserEmailAlreadyExistsException;
import com.connectors.pos.exceptions.UserManagementException;
import com.connectors.pos.exceptions.UserNameAlreadyExistsException;
import com.connectors.pos.exceptions.UserNotFoundException;
import com.connectors.pos.users.userdtos.CreateUserDto;
import com.connectors.pos.users.userdtos.UserListDto;
import com.connectors.pos.users.userdtos.UserMapper;
import com.connectors.pos.users.userdtos.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepo;
    private final UserMapper userMapper;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final AuditLogService auditLogService;
    /**
     * this service should contain 2 methods
     * 1 for create a new user
     * 2 add role to existing user
     * 3 passwords must be hashed
     */

@Transactional
    public UserResponseDto createUser(CreateUserDto create){

    if (roleRepo.lockInitialAdministratorRegistration() != 1) {
        throw new RoleNotFoundException("role not found: ROLE_ADMIN");
    }

    if(userRepo.existsByEmail(create.email())){

     throw new UserEmailAlreadyExistsException("email already exists");
        }

        if(userRepo.existsByName(create.name())){

            throw new UserNameAlreadyExistsException("name already exists");
        }

Users user = userMapper.toEntity(create);


    long existingUsers = userRepo.count();
    if (existingUsers > 0 && !currentUserIsAdmin()) {
        throw new AccessDeniedException("Only an administrator may register additional users.");
    }
    String roleName = existingUsers == 0 ? "ROLE_ADMIN" : "ROLE_USER";
    Roles role = roleRepo.findByName(roleName)
            .orElseThrow(() -> new RoleNotFoundException("role not found: " + roleName));
    user.setRoles(new java.util.HashSet<>(Set.of(role)));
user.setPassword(encoder.encode(create.password()));

    Users saved = userRepo.save(user);

    return userMapper.toResponse(saved);
    }

    private static boolean currentUserIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
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

    @Transactional(readOnly = true)
    public List<UserListDto> getAllUsers() {
        return userRepo.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(u -> new UserListDto(u.getId(), u.getName(), u.getEmail(), isAdmin(u)))
                .toList();
    }

    @Transactional
    public void promoteToAdmin(Long userId) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("user wasn't found"));

        if (isAdmin(user)) {
            return;
        }

        Roles adminRole = roleRepo.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RoleNotFoundException("role not found: ROLE_ADMIN"));

        user.getRoles().add(adminRole);
        userRepo.save(user);
        auditLogService.log("USER_PROMOTE", "USER", userId,
                "Promoted " + user.getEmail() + " to administrator.");
    }

    @Transactional
    public void demoteFromAdmin(Long userId, Long requesterId) {
        if (userId.equals(requesterId)) {
            throw new UserManagementException("You cannot remove your own administrator access.");
        }

        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("user wasn't found"));

        if (!isAdmin(user)) {
            return;
        }

        if (userRepo.countByRoleName("ROLE_ADMIN") <= 1) {
            throw new UserManagementException("At least one administrator must remain.");
        }

        user.getRoles().removeIf(role -> "ROLE_ADMIN".equals(role.getName()));
        userRepo.save(user);
        auditLogService.log("USER_DEMOTE", "USER", userId,
                "Demoted " + user.getEmail() + " to cashier.");
    }

    @Transactional
    public void resetPasswordByAdmin(Long userId, String newPassword) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("user wasn't found"));

        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);
        auditLogService.log("PASSWORD_RESET_ADMIN", "USER", userId,
                "Administrator reset the password for " + user.getEmail() + ".");
    }

    @Transactional
    public void changeOwnPassword(Long userId, String currentPassword, String newPassword) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("user wasn't found"));

        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new UserManagementException("Current password is incorrect.");
        }

        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);
        auditLogService.log("PASSWORD_CHANGE_SELF", "USER", userId,
                user.getEmail() + " changed their own password.");
    }

    @Transactional(readOnly = true)
    public UserListDto getUserSummary(Long userId) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("user wasn't found"));
        return new UserListDto(user.getId(), user.getName(), user.getEmail(), isAdmin(user));
    }

    private static boolean isAdmin(Users user) {
        return user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

}
