package com.feedback.ui.views.users;

import com.feedback.model.Department;
import com.feedback.model.Role;
import com.feedback.model.User;
import com.feedback.service.AuthenticationService;
import com.feedback.service.UserService;
import com.feedback.ui.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users | Feedback System")
@RolesAllowed({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
public class UserView extends VerticalLayout {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final Grid<User> grid = new Grid<>(User.class);

    // Form fields for user creation/editing
    private final TextField username = new TextField("Username");
    private final TextField firstName = new TextField("First Name");
    private final TextField lastName = new TextField("Last Name");
    private final EmailField email = new EmailField("Email");
    private final PasswordField password = new PasswordField("Password");
    private final ComboBox<Role> role = new ComboBox<>("Role");
    private final ComboBox<Department> department = new ComboBox<>("Department");
    private final ComboBox<User> manager = new ComboBox<>("Manager");
    private final Binder<User> binder = new Binder<>(User.class);

    private User currentEditingUser = null;

    public UserView(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;

        System.out.println("UserView: Constructor started");

        addClassName("user-view");
        setSizeFull();

        // Diagnostic: Print current user and authorities
        try {
            User user = authenticationService.getCurrentUser();
            System.out.println("Current User: " + (user != null ? user.getUsername() : "null"));
            // You can also log session/security context if needed
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
        }

        try {
            configureGrid();
            H2 title = new H2("User Management");
            add(title, getManagementToolbar(), grid);
            updateList();
            System.out.println("UserView: Successfully initialized");
        } catch (Exception e) {
            System.err.println("Error initializing UserView: " + e.getMessage());
            e.printStackTrace();
            add(new Span("Error loading user management. Please try again."));
        }
    }

    private void configureGrid() {
        grid.addClassName("user-grid");
        grid.setSizeFull();

        grid.removeAllColumns();
        grid.addColumn(User::getUsername).setHeader("Username").setAutoWidth(true);
        grid.addColumn(User::getFullName).setHeader("Full Name").setAutoWidth(true);
        grid.addColumn(User::getEmail).setHeader("Email").setAutoWidth(true);
        grid.addColumn(user -> user.getRole() != null ? user.getRole().getName() : "").setHeader("Role").setAutoWidth(true);
        grid.addColumn(user -> user.getDepartment() != null ? user.getDepartment().getName() : "").setHeader("Department").setAutoWidth(true);
        grid.addColumn(user -> user.getManager() != null ? user.getManager().getFullName() : "").setHeader("Manager").setAutoWidth(true);
        grid.addColumn(user -> user.isActive() ? "Active" : "Inactive").setHeader("Status").setAutoWidth(true);
        grid.addColumn(user -> user.getCreatedAt() != null ?
                user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "").setHeader("Created").setAutoWidth(true);
        grid.addColumn(user -> user.getLastLogin() != null ?
                user.getLastLogin().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Never").setHeader("Last Login").setAutoWidth(true);

        grid.addComponentColumn(user -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button editButton = new Button("Edit");
            editButton.addClickListener(e -> openUserForm(user));
            Button toggleButton = new Button(user.isActive() ? "Deactivate" : "Activate");
            toggleButton.addClickListener(e -> toggleUserStatus(user));
            Button resetPasswordButton = new Button("Reset Password");
            resetPasswordButton.addClickListener(e -> openResetPasswordDialog(user));
            actions.add(editButton, toggleButton, resetPasswordButton);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private HorizontalLayout getManagementToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        Button addUserButton = new Button("Add User");
        addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addUserButton.addClickListener(e -> openUserForm(new User()));
        toolbar.add(addUserButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void openUserForm(User user) {
        boolean isNewUser = user.getId() == null;
        currentEditingUser = user;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNewUser ? "Add User" : "Edit User: " + (user.getFullName() != null ? user.getFullName() : user.getEmail()));
        dialog.setWidth("600px");
        configureForm(isNewUser);

        if (!isNewUser) {
            binder.setBean(user);
            password.setVisible(false);
        } else {
            binder.setBean(new User());
            password.setVisible(true);
            password.setRequired(true);
        }

        FormLayout formLayout = new FormLayout();
        formLayout.add(username, firstName, lastName, email, role, department, manager);
        if (isNewUser) {
            formLayout.add(password);
        }

        HorizontalLayout buttonLayout = createFormButtons(dialog, isNewUser);
        dialog.add(formLayout, buttonLayout);
        dialog.open();
    }

    private void configureForm(boolean isNewUser) {
        username.setRequired(true);
        firstName.setRequired(true);
        lastName.setRequired(true);
        email.setRequired(true);

        role.setItems(userService.findAllRoles());
        role.setItemLabelGenerator(Role::getName);
        role.setRequired(true);

        department.setItems(userService.findAllDepartments());
        department.setItemLabelGenerator(Department::getName);
        department.setRequired(true);

        manager.setItems(userService.findActiveUsers());
        manager.setItemLabelGenerator(User::getFullName);

        if (isNewUser) {
            password.setRequired(true);
            password.setMinLength(6);
            password.setHelperText("Minimum 6 characters");
        }

        binder.forField(username)
                .withValidator(value -> !value.isEmpty(), "Username is required")
                .bind(User::getUsername, User::setUsername);
        binder.forField(firstName)
                .withValidator(value -> !value.isEmpty(), "First name is required")
                .bind(User::getFirstName, User::setFirstName);
        binder.forField(lastName)
                .withValidator(value -> !value.isEmpty(), "Last name is required")
                .bind(User::getLastName, User::setLastName);
        binder.forField(email)
                .withValidator(value -> !value.isEmpty(), "Email is required")
                .bind(User::getEmail, User::setEmail);
        binder.forField(role)
                .withValidator(value -> value != null, "Role is required")
                .bind(User::getRole, User::setRole);
        binder.forField(department)
                .withValidator(value -> value != null, "Department is required")
                .bind(User::getDepartment, User::setDepartment);
        binder.bind(manager, User::getManager, User::setManager);
    }

    private HorizontalLayout createFormButtons(Dialog dialog, boolean isNewUser) {
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveUser(dialog, isNewUser));
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> dialog.close());
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);

        if (!isNewUser) {
            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> deleteUser(dialog, currentEditingUser));
            buttonLayout.add(deleteButton);
        }

        buttonLayout.setSpacing(true);
        return buttonLayout;
    }

    private void saveUser(Dialog dialog, boolean isNewUser) {
        try {
            if (isNewUser) {
                if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
                        email.isEmpty() || password.isEmpty() || role.isEmpty() || department.isEmpty()) {
                    showError("Please fill in all required fields");
                    return;
                }
                if (!userService.isEmailAvailable(email.getValue())) {
                    showError("User with this email already exists");
                    return;
                }
                if (!userService.isUsernameAvailable(username.getValue())) {
                    showError("User with this username already exists");
                    return;
                }
                User newUser = userService.createUser(
                        username.getValue(),
                        firstName.getValue(),
                        lastName.getValue(),
                        email.getValue(),
                        password.getValue(),
                        role.getValue(),
                        department.getValue(),
                        manager.getValue()
                );
                showSuccess("User created successfully");
            } else {
                binder.writeBean(currentEditingUser);
                userService.updateUser(currentEditingUser);
                showSuccess("User updated successfully");
            }

            dialog.close();
            updateList();
            clearForm();
        } catch (ValidationException ex) {
            showError("Please check the form for errors");
        } catch (Exception ex) {
            showError("Error saving user: " + ex.getMessage());
        }
    }

    private void deleteUser(Dialog dialog, User user) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Confirm Delete");
        VerticalLayout content = new VerticalLayout();
        content.add(new Span("Are you sure you want to delete user: " + user.getFullName() + "?"));
        content.add(new Span("This action cannot be undone."));
        HorizontalLayout buttons = new HorizontalLayout();
        Button confirmButton = new Button("Delete");
        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirmButton.addClickListener(e -> {
            userService.deleteUser(user.getId());
            confirmDialog.close();
            dialog.close();
            updateList();
            showSuccess("User deleted successfully");
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> confirmDialog.close());
        buttons.add(confirmButton, cancelButton);
        content.add(buttons);
        confirmDialog.add(content);
        confirmDialog.open();
    }

    private void toggleUserStatus(User user) {
        if (user.isActive()) {
            userService.deactivateUser(user.getId());
            showSuccess("User deactivated successfully");
        } else {
            userService.reactivateUser(user.getId());
            showSuccess("User reactivated successfully");
        }
        updateList();
    }

    private void openResetPasswordDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reset Password for: " + user.getFullName());
        PasswordField newPassword = new PasswordField("New Password");
        newPassword.setRequired(true);
        newPassword.setMinLength(6);
        newPassword.setWidthFull();

        PasswordField confirmPassword = new PasswordField("Confirm Password");
        confirmPassword.setRequired(true);
        confirmPassword.setWidthFull();

        Button saveButton = new Button("Reset Password");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            if (newPassword.getValue().isEmpty() || confirmPassword.getValue().isEmpty()) {
                showError("Please fill in both password fields");
                return;
            }
            if (!newPassword.getValue().equals(confirmPassword.getValue())) {
                showError("Passwords do not match");
                return;
            }
            if (newPassword.getValue().length() < 6) {
                showError("Password must be at least 6 characters");
                return;
            }
            userService.changeUserPassword(user, newPassword.getValue());
            dialog.close();
            showSuccess("Password reset successfully");
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> dialog.close());

        VerticalLayout content = new VerticalLayout(newPassword, confirmPassword);
        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        dialog.add(content, buttons);
        dialog.open();
    }

    private void updateList() {
        grid.setItems(userService.findAllUsers());
    }

    private void clearForm() {
        username.clear();
        firstName.clear();
        lastName.clear();
        email.clear();
        password.clear();
        role.clear();
        department.clear();
        manager.clear();
        currentEditingUser = null;
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
