package com.connectors.pos.users;

import com.connectors.pos.audit.AuditLogService;
import com.connectors.pos.exceptions.RoleNotFoundException;
import com.connectors.pos.exceptions.UserEmailAlreadyExistsException;
import com.connectors.pos.exceptions.UserManagementException;
import com.connectors.pos.exceptions.UserNameAlreadyExistsException;
import com.connectors.pos.exceptions.UserNotFoundException;
import com.connectors.pos.i18n.Messages;
import com.connectors.pos.users.userdtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final ApplicationEventPublisher applicationEventPublisher;   // the injected instance
    private final Environment environment;

    /**
     * Seeded by DemoInitializer only when the "demo" profile is active (i.e. the
     * public Render demo deployment). This account must stay usable as a live demo
     * admin, so any mutation that could lock visitors out (demotion, forced password
     * reset, or self password change) is blocked while running under that profile.
     */
    private static final String PROTECTED_DEMO_EMAIL = "user123@gmail.com";

    private boolean isProtectedDemoAccount(Users user) {
        return user != null
                && user.getEmail() != null
                && user.getEmail().equalsIgnoreCase(PROTECTED_DEMO_EMAIL)
                && environment.acceptsProfiles(Profiles.of("demo"));
    }

    private void assertNotProtectedDemoAccount(Users user) {
        if (isProtectedDemoAccount(user)) {
            throw new UserManagementException(Messages.get("error.user.demoAccountProtected"));
        }
    }

@Transactional
    public UserResponseDto createUser(CreateUserDto create){

    if (roleRepo.lockInitialAdministratorRegistration() != 1) {
        throw new RoleNotFoundException(Messages.get("error.role.adminNotFound"));
    }

    if(userRepo.existsByEmail(create.email())){

     throw new UserEmailAlreadyExistsException(Messages.get("error.user.emailExists"));
        }

        if(userRepo.existsByName(create.name())){

            throw new UserNameAlreadyExistsException(Messages.get("error.user.nameExists"));
        }

Users user = userMapper.toEntity(create);


    long existingUsers = userRepo.count();
    if (existingUsers > 0 && !currentUserIsAdmin()) {
        throw new AccessDeniedException(Messages.get("error.auth.adminRequiredForRegistration"));
    }
    String roleName = existingUsers == 0 ? "ROLE_ADMIN" : "ROLE_USER";
    Roles role = roleRepo.findByName(roleName)
            .orElseThrow(() -> new RoleNotFoundException(Messages.get("error.role.notFoundNamed", roleName)));
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
            orElseThrow(()->new UsernameNotFoundException(Messages.get("error.user.notFound")));

            Roles role = roleRepo.findById(roleId)
                    .orElseThrow(()-> new RoleNotFoundException(Messages.get("error.role.notFound")));


  Set<Roles> roles =user.getRoles();
  roles.add(role);
    }

    @Transactional(readOnly = true)
    public List<UserListDto> getAllUsers() {
        return userRepo.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(u -> new UserListDto(u.getId(), u.getName(), u.getEmail(), isAdmin(u), isProtectedDemoAccount(u)))
                .toList();
    }


    @Transactional
    public void promoteToAdmin(Long userId) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(Messages.get("error.user.notFound")));

        if (isAdmin(user)) {
            return;
        }

        Roles adminRole = roleRepo.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RoleNotFoundException(Messages.get("error.role.adminNotFound")));

        user.getRoles().add(adminRole);
        userRepo.save(user);

        applicationEventPublisher.publishEvent(
                new AuditEvent("USER_PROMOTE", "USER", userId,
                        "Promoted " + user.getEmail() + " to administrator.",
                        null, ""));

    }


    @Transactional
    public void demoteFromAdmin(Long userId, Long requesterId) {
        if (userId.equals(requesterId)) {
            throw new UserManagementException(Messages.get("error.user.cannotRemoveOwnAdmin"));
        }

        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(Messages.get("error.user.notFound")));

        if (!isAdmin(user)) {
            return;
        }

        assertNotProtectedDemoAccount(user);

        if (userRepo.countByRoleName("ROLE_ADMIN") <= 1) {
            throw new UserManagementException(Messages.get("error.user.atLeastOneAdminRequired"));
        }

        user.getRoles().removeIf(role -> "ROLE_ADMIN".equals(role.getName()));
        userRepo.save(user);
        applicationEventPublisher.publishEvent(
                new AuditEvent("USER_DEMOTE", "USER", userId,
                        "Demoted " + user.getEmail() + " to cashier.",
                        null, ""));
    }

    @Transactional
    public void resetPasswordByAdmin(Long userId, String newPassword) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(Messages.get("error.user.notFound")));

        assertNotProtectedDemoAccount(user);

        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);
            //publish an audit event for the password reset action
            applicationEventPublisher.publishEvent(
                new AuditEvent("PASSWORD_RESET_ADMIN", "USER", userId,
                        "Administrator reset the password for " + user.getEmail() + ".",
                        null, ""));
    }

    @Transactional
    public void changeOwnPassword(Long userId, String currentPassword, String newPassword) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(Messages.get("error.user.notFound")));

        assertNotProtectedDemoAccount(user);

        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new UserManagementException(Messages.get("error.user.currentPasswordIncorrect"));
        }

        if (user.isMustChangePassword() && encoder.matches(newPassword, user.getPassword())) {
            throw new UserManagementException(Messages.get("error.user.passwordSameAsDefault"));
        }

        user.setPassword(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepo.save(user);
        applicationEventPublisher.publishEvent(
                new AuditEvent("PASSWORD_CHANGE_SELF", "USER", userId,
                        user.getEmail() + " changed their own password.",
                        null, ""));
    }

    @Transactional(readOnly = true)
    public boolean isDefaultAdminPending() {
        return userRepo.existsByEmailAndMustChangePasswordTrue(DefaultAdmin.EMAIL);
    }

    @Transactional(readOnly = true)
    public UserListDto getUserSummary(Long userId) {
        Users user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(Messages.get("error.user.notFound")));
        return new UserListDto(user.getId(), user.getName(), user.getEmail(), isAdmin(user), isProtectedDemoAccount(user));
    }

    private static boolean isAdmin(Users user) {
        return user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

}
