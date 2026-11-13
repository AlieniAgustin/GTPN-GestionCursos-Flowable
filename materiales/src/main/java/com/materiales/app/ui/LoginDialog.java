package com.materiales.app.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.materiales.app.service.AuthenticationFailedException;
import com.materiales.app.service.StaffAuthenticator;
import com.materiales.app.service.StaffUser;

/**
 * Identificación del administrativo con su usuario y contraseña de Flowable.
 */
public class LoginDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final StaffAuthenticator authenticator;
    private final JTextField userField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JLabel errorLabel = new JLabel(" ", SwingConstants.CENTER);

    private StaffUser authenticatedUser;

    public LoginDialog(Window owner, StaffAuthenticator authenticator) {
        super(owner, "Preparación de materiales — Ingresar", ModalityType.APPLICATION_MODAL);
        this.authenticator = authenticator;

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.LINE_END;

        constraints.gridx = 0;
        constraints.gridy = 0;
        form.add(new JLabel("Usuario:"), constraints);
        constraints.gridy = 1;
        form.add(new JLabel("Contraseña:"), constraints);

        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.gridx = 1;
        constraints.gridy = 0;
        form.add(userField, constraints);
        constraints.gridy = 1;
        form.add(passwordField, constraints);

        errorLabel.setForeground(Color.RED.darker());

        JButton loginButton = new JButton("Ingresar");
        loginButton.addActionListener(event -> attemptLogin());
        JButton cancelButton = new JButton("Salir");
        cancelButton.addActionListener(event -> {
            authenticatedUser = null;
            dispose();
        });

        JPanel buttons = new JPanel();
        buttons.add(loginButton);
        buttons.add(cancelButton);

        JPanel south = new JPanel(new BorderLayout());
        south.add(errorLabel, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginButton);
        setMinimumSize(new Dimension(360, 180));
        pack();
        setLocationRelativeTo(owner);
    }

    private void attemptLogin() {
        try {
            authenticatedUser = authenticator.login(userField.getText(),
                    new String(passwordField.getPassword()));
            dispose();
        } catch (AuthenticationFailedException e) {
            authenticatedUser = null;
            errorLabel.setText(e.getMessage());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    /**
     * Shows the dialog and returns the authenticated user, or null if it was cancelled.
     */
    public StaffUser showAndGetUser() {
        userField.requestFocusInWindow();
        setVisible(true);
        return authenticatedUser;
    }
}
