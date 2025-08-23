package com.feedback.service;

import com.feedback.model.Department;
import com.feedback.model.Role;
import com.feedback.model.User;
import com.feedback.repository.DepartmentRepository;
import com.feedback.repository.RoleRepository;
import com.feedback.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    
    @Autowired
    public UserService(UserRepository userRepository, 
                      RoleRepository roleRepository,
                      DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
    }
    
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public List<User> findSubordinates(Long managerId) {
        return userRepository.findByManagerId(managerId);
    }
    
    public List<User> findUsersByDepartment(Long departmentId) {
        return userRepository.findByDepartmentId(departmentId);
    }
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }
    
    public List<Department> findAllDepartments() {
        return departmentRepository.findAll();
    }
    
    // Initialize some default data for testing
    public void initializeDefaultData() {
        // Create default roles if they don't exist
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setName(Role.ADMIN);
            adminRole.setDescription("Administrator with full access");
            roleRepository.save(adminRole);
            
            Role managerRole = new Role();
            managerRole.setName(Role.MANAGER);
            managerRole.setDescription("Manager with team management access");
            roleRepository.save(managerRole);
            
            Role employeeRole = new Role();
            employeeRole.setName(Role.EMPLOYEE);
            employeeRole.setDescription("Regular employee");
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
        
        // Create a default admin user if no users exist
        if (userRepository.count() == 0) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setFirstName("System");
            adminUser.setLastName("Administrator");
            adminUser.setEmail("admin@example.com");
            adminUser.setRole(roleRepository.findByName(Role.ADMIN).orElse(null));
            adminUser.setDepartment(departmentRepository.findAll().get(0));
            userRepository.save(adminUser);
        }
    }
}
