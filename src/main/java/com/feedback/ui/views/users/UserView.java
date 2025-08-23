package com.feedback.ui.views.users;

import com.feedback.model.Department;
import com.feedback.model.Role;
import com.feedback.model.User;
import com.feedback.service.UserService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users | Feedback System")
public class UserView extends VerticalLayout {

    private final UserService userService;
    private final Grid<User> grid = new Grid<>(User.class);
    private final TextField username = new TextField("Username");
    private final TextField firstName = new TextField("First Name");
    private final TextField lastName = new TextField("Last Name");
    private final EmailField email = new EmailField("Email");
    private final ComboBox<Role> role = new ComboBox<>("Role");
    private final ComboBox<Department> department = new ComboBox<>("Department");
    private final ComboBox<User> manager = new ComboBox<>("Manager");
    private final Binder<User> binder = new Binder<>(User.class);
    private User currentUser = null;

    public UserView(UserService userService) {
        this.userService = userService;
        addClassName("user-view");
        setSizeFull();

        configureGrid();
        configureForm();

        add(
                new H2("Users"),
                getToolbar(),
                grid
        );

        updateList();
    }

    private void configureGrid() {
        grid.addClassName("user-grid");
        grid.setSizeFull();
        grid.setColumns("username", "firstName", "lastName", "email");
        grid.addColumn(user -> user.getRole() != null ? user.getRole().getName() : "").setHeader("Role");
        grid.addColumn(user -> user.getDepartment() != null ? user.getDepartment().getName() : "").setHeader("Department");
        grid.addColumn(user -> user.getManager() != null ? user.getManager().getFullName() : "").setHeader("Manager");
        
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        
        grid.asSingleSelect().addValueChangeListener(e -> {
            openUserForm(e.getValue());
        });
    }

    private HorizontalLayout getToolbar() {
        Button addUserButton = new Button("Add User");
        addUserButton.addClickListener(e -> openUserForm(new User()));

        HorizontalLayout toolbar = new HorizontalLayout(addUserButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void configureForm() {
        role.setItems(userService.findAllRoles());
        role.setItemLabelGenerator(Role::getName);
        
        department.setItems(userService.findAllDepartments());
        department.setItemLabelGenerator(Department::getName);
        
        manager.setItems(userService.findAllUsers());
        manager.setItemLabelGenerator(User::getFullName);
        
        binder.bindInstanceFields(this);
    }

    private void openUserForm(User user) {
        if (user == null) {
            return;
        }
        
        currentUser = user;
        binder.setBean(user);
        
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(user.getId() == null ? "Add User" : "Edit User");
        
        FormLayout formLayout = new FormLayout();
        formLayout.add(username, firstName, lastName, email, role, department, manager);
        
        Button saveButton = new Button("Save");
        saveButton.addClickListener(e -> {
            if (binder.validate().isOk()) {
                userService.saveUser(currentUser);
                dialog.close();
                updateList();
                Notification.show("User saved successfully");
            }
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> dialog.close());
        
        Button deleteButton = new Button("Delete");
        deleteButton.setVisible(user.getId() != null);
        deleteButton.addClickListener(e -> {
            if (user.getId() != null) {
                userService.deleteUser(user.getId());
                dialog.close();
                updateList();
                Notification.show("User deleted");
            }
        });
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton, deleteButton);
        buttonLayout.setSpacing(true);
        
        dialog.add(formLayout, buttonLayout);
        dialog.open();
    }

    private void updateList() {
        grid.setItems(userService.findAllUsers());
    }
}