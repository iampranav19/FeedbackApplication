package com.feedback.service;

import com.feedback.model.Department;
import com.feedback.model.Role;
import com.feedback.model.User;
import com.feedback.repository.DepartmentRepository;
import com.feedback.repository.RoleRepository;
import com.feedback.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(UserRepository userRepository, 
                      RoleRepository roleRepository,
                      DepartmentRepository departmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> findActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }
    
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> findSubordinates(Long managerId) {
        return userRepository.findByManagerId(managerId);
    }
    
    public List<User> findUsersByDepartment(Long departmentId) {
        return userRepository.findByDepartmentId(departmentId);
    }
    
    /**
     * Create a new user with encrypted password (only for super admin)
     */
    public User createUser(String username, String firstName, String lastName, String email, 
                          String plainPassword, Role role, Department department, User manager) {
        
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with this email already exists");
        }
        
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("User with this username already exists");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setRole(role);
        user.setDepartment(department);
        user.setManager(manager);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    /**
     * Update existing user (without changing password)
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Change user password
     */
    public boolean changeUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
    
    /**
     * Deactivate user instead of deleting
     */
    public void deactivateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });
    }
    
    /**
     * Reactivate user
     */
    public void reactivateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(true);
            userRepository.save(user);
        });
    }
    
    /**
     * Hard delete user (use with caution)
     */
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    /**
     * Check if email is available
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
    
    /**
     * Check if username is available
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }
    
    public List<Department> findAllDepartments() {
        return departmentRepository.findAll();
    }
    
    public Optional<Role> findRoleByName(String name) {
        return roleRepository.findByName(name);
    }
    
    /**
     * Initialize default data and super admin user
     */
    public void initializeDefaultData() {
        // Create default roles if they don't exist
        if (roleRepository.count() == 0) {
            Role superAdminRole = new Role(Role.SUPER_ADMIN, "Super Administrator with full system access");
            roleRepository.save(superAdminRole);
            
            Role adminRole = new Role(Role.ADMIN, "Administrator with administrative access");
            roleRepository.save(adminRole);
            
            Role managerRole = new Role(Role.MANAGER, "Manager with team management access");
            roleRepository.save(managerRole);
            
            Role employeeRole = new Role(Role.EMPLOYEE, "Regular employee");
            roleRepository.save(employeeRole);
        }
        
        // Create default departments if they don't exist
        if (departmentRepository.count() == 0) {
            Department itDept = new Department();
            itDept.setName("IT");
            itDept.setDescription("Information Technology Department");
            departmentRepository.save(itDept);
            
            Department hrDept = new Department();
            hrDept.setName("HR");
            hrDept.setDescription("Human Resources Department");
            departmentRepository.save(hrDept);
            
            Department salesDept = new Department();
            salesDept.setName("Sales");
            salesDept.setDescription("Sales and Marketing Department");
            departmentRepository.save(salesDept);
        }
        
        // Create super admin user (Pranav) if no users exist
        if (userRepository.count() == 0) {
            Role superAdminRole = roleRepository.findByName(Role.SUPER_ADMIN).orElse(null);
            Department itDept = departmentRepository.findAll().get(0); // First department
            
            if (superAdminRole != null) {
                User superAdmin = new User();
                superAdmin.setUsername("pranav");
                superAdmin.setFirstName("Pranav");
                superAdmin.setLastName("Super Admin");
                superAdmin.setEmail("pranav@company.com");
                superAdmin.setPassword(passwordEncoder.encode("test123"));
                superAdmin.setRole(superAdminRole);
                superAdmin.setDepartment(itDept);
                superAdmin.setActive(true);
                superAdmin.setCreatedAt(LocalDateTime.now());
                
                userRepository.save(superAdmin);
            }
        }
    }
    
    /**
     * Get users that current user can give feedback to (exclude self)
     */
    public List<User> getUsersForFeedback(User currentUser) {
        return userRepository.findByIsActiveTrue().stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .toList();
    }
}