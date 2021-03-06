package com.rcsoyer.servicosjuridicos.service;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.rcsoyer.servicosjuridicos.config.Constants;
import com.rcsoyer.servicosjuridicos.domain.Authority;
import com.rcsoyer.servicosjuridicos.domain.User;
import com.rcsoyer.servicosjuridicos.repository.AuthorityRepository;
import com.rcsoyer.servicosjuridicos.repository.UserRepository;
import com.rcsoyer.servicosjuridicos.security.AuthoritiesConstants;
import com.rcsoyer.servicosjuridicos.security.SecurityUtils;
import com.rcsoyer.servicosjuridicos.service.dto.UserDTO;
import com.rcsoyer.servicosjuridicos.service.util.RandomUtil;
import com.rcsoyer.servicosjuridicos.web.rest.errors.EmailAlreadyUsedException;
import com.rcsoyer.servicosjuridicos.web.rest.errors.InvalidPasswordException;
import com.rcsoyer.servicosjuridicos.web.rest.errors.LoginAlreadyUsedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing users.
 */
@Slf4j
@Service
@Transactional
public class UserService {
    
    private final CacheManager cacheManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;
    
    public UserService(final UserRepository userRepository, final PasswordEncoder passwordEncoder,
                       final AuthorityRepository authorityRepository, final CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.cacheManager = cacheManager;
    }
    
    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository.findOneByActivationKey(key)
                             .map(user -> {
                                 // activate given user for the registration key.
                                 user.setActivated(true);
                                 user.setActivationKey(null);
                                 this.clearUserCaches(user);
                                 log.debug("Activated user: {}", user);
                                 return user;
                             });
    }
    
    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);
        return userRepository.findOneByResetKey(key)
                             .filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400)))
                             .map(user -> {
                                 user.setPassword(passwordEncoder.encode(newPassword));
                                 user.setResetKey(null);
                                 user.setResetDate(null);
                                 this.clearUserCaches(user);
                                 return user;
                             });
    }
    
    public Optional<User> requestPasswordReset(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail)
                             .filter(User::getActivated)
                             .map(user -> {
                                 user.setResetKey(RandomUtil.generateResetKey());
                                 user.setResetDate(Instant.now());
                                 this.clearUserCaches(user);
                                 return user;
                             });
    }
    
    public User registerUser(UserDTO userDTO, String password) {
        userRepository.findOneByLogin(userDTO.getLogin().toLowerCase()).ifPresent(existingUser -> {
            boolean removed = removeNonActivatedUser(existingUser);
            if (!removed) {
                throw new LoginAlreadyUsedException();
            }
        });
        userRepository.findOneByEmailIgnoreCase(userDTO.getEmail()).ifPresent(existingUser -> {
            boolean removed = removeNonActivatedUser(existingUser);
            if (!removed) {
                throw new EmailAlreadyUsedException();
            }
        });
        User newUser = new User();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(userDTO.getLogin().toLowerCase());
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userDTO.getFirstName());
        newUser.setLastName(userDTO.getLastName());
        newUser.setEmail(userDTO.getEmail().toLowerCase());
        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);
        this.clearUserCaches(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }
    
    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.getActivated()) {
            return false;
        }
        userRepository.delete(existingUser);
        userRepository.flush();
        this.clearUserCaches(existingUser);
        return true;
    }
    
    public User createUser(final UserDTO dto) {
        final User user = new User()
                              .setLogin(dto.getLogin().toLowerCase())
                              .setFirstName(dto.getFirstName())
                              .setLastName(dto.getLastName())
                              .setEmail(dto.getEmail().toLowerCase())
                              .setImageUrl(dto.getImageUrl());
        
        if (dto.getLangKey() == null) {
            user.setLangKey(Constants.DEFAULT_LANGUAGE);
        } else {
            user.setLangKey(dto.getLangKey());
        }
        
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        user.setActivated(true);
        
        if (isNotEmpty(dto.getAuthorities())) {
            final Set<Authority> authorities = authorityRepository.findByNameIn(dto.getAuthorities());
            user.setAuthorities(authorities);
        }
        
        userRepository.save(user);
        this.clearUserCaches(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }
    
    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param dataUpdate user to update
     * @return updated user
     */
    public Optional<UserDTO> updateUser(final UserDTO dataUpdate) {
        return userRepository.findById(dataUpdate.getId())
                             .map(user -> {
                                 clearUserCaches(user);
                                 user.setLogin(dataUpdate.getLogin().toLowerCase());
                                 user.setFirstName(dataUpdate.getFirstName());
                                 user.setLastName(dataUpdate.getLastName());
                                 user.setEmail(dataUpdate.getEmail().toLowerCase());
                                 user.setImageUrl(dataUpdate.getImageUrl());
                                 user.setActivated(dataUpdate.isActivated());
                                 user.setLangKey(dataUpdate.getLangKey());
                                 final Set<Authority> authorities =
                                     authorityRepository.findByNameIn(dataUpdate.getAuthorities());
                                 user.setAuthorities(authorities);
                                 this.clearUserCaches(user);
                                 log.debug("Changed Information for User: {}", user);
                                 return user;
                             })
                             .map(UserDTO::new);
    }
    
    public void deleteUser(String login) {
        userRepository.findOneByLogin(login).ifPresent(user -> {
            userRepository.delete(user);
            this.clearUserCaches(user);
            log.debug("Deleted User: {}", user);
        });
    }
    
    public void changePassword(String currentClearTextPassword, String newPassword) {
        SecurityUtils.getCurrentUserLogin()
                     .flatMap(userRepository::findOneByLogin)
                     .ifPresent(user -> {
                         String currentEncryptedPassword = user.getPassword();
                         if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                             throw new InvalidPasswordException();
                         }
                         String encryptedPassword = passwordEncoder.encode(newPassword);
                         user.setPassword(encryptedPassword);
                         this.clearUserCaches(user);
                         log.debug("Changed password for User: {}", user);
                     });
    }
    
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAllByLoginNot(pageable, Constants.ANONYMOUS_USER).map(UserDTO::new);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities(Long id) {
        return userRepository.findOneWithAuthoritiesById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
    }
    
    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userRepository
            .findAllByActivatedIsFalseAndCreatedDateBefore(Instant.now().minus(3, ChronoUnit.DAYS))
            .forEach(user -> {
                log.debug("Deleting not activated user {}", user.getLogin());
                userRepository.delete(user);
                this.clearUserCaches(user);
            });
    }
    
    /**
     * @return a list of all the authorities
     */
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).collect(Collectors.toList());
    }
    
    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evict(user.getLogin());
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evict(user.getEmail());
    }
}
