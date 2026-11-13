package com.projeto.gestao.config;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.projeto.gestao.service.AdminPasswordResetService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminPasswordResetCommand implements ApplicationRunner {
    static final String RESET_OPTION = "admin-password-reset";
    static final String EMAIL_OPTION = "admin-password-reset-email";

    private final Supplier<AdminPasswordResetService> resetServiceSupplier;
    private final Supplier<Console> consoleSupplier;

    @Autowired
    public AdminPasswordResetCommand(ObjectProvider<AdminPasswordResetService> resetServiceProvider) {
        this(resetServiceProvider::getIfAvailable, System::console);
    }

    AdminPasswordResetCommand(
            AdminPasswordResetService resetService, Supplier<Console> consoleSupplier) {
        this(() -> resetService, consoleSupplier);
    }

    AdminPasswordResetCommand(
            Supplier<AdminPasswordResetService> resetServiceSupplier,
            Supplier<Console> consoleSupplier) {
        this.resetServiceSupplier = resetServiceSupplier;
        this.consoleSupplier = consoleSupplier;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!arguments.containsOption(RESET_OPTION)) {
            return;
        }

        String email = singleOption(arguments, EMAIL_OPTION);
        AdminPasswordResetService resetService = resetServiceSupplier.get();
        if (resetService == null) {
            throw new IllegalStateException(
                    "Redefinição administrativa exige APP_ADMIN_JWT_ENABLED=true.");
        }

        Console console = consoleSupplier.get();
        if (console == null) {
            throw new IllegalStateException(
                    "Redefinição administrativa exige console interativo.");
        }

        char[] passwordChars = null;
        try {
            passwordChars = console.readPassword("Nova senha do administrador: ");
            if (passwordChars == null) {
                throw new IllegalStateException(
                        "A nova senha administrativa não foi fornecida.");
            }
            resetService.reset(email, passwordChars);
        } finally {
            clear(passwordChars);
        }
    }

    private String singleOption(ApplicationArguments arguments, String option) {
        List<String> values = arguments.getOptionValues(option);
        if (values == null || values.size() != 1 || values.get(0) == null
                || values.get(0).isBlank()) {
            throw new IllegalArgumentException(
                    "Informe exatamente um e-mail para a redefinição administrativa.");
        }
        return values.get(0);
    }

    private static void clear(char[] passwordChars) {
        if (passwordChars != null) {
            Arrays.fill(passwordChars, '\0');
        }
    }
}
