package com.projeto.gestao.config;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.projeto.gestao.service.AdminBootstrapService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapCommand implements ApplicationRunner {
    static final String BOOTSTRAP_OPTION = "admin-bootstrap";
    static final String EMAIL_OPTION = "admin-bootstrap-email";
    static final String PASSWORD_ENV = "APP_ADMIN_BOOTSTRAP_PASSWORD";

    private final AdminBootstrapService bootstrapService;
    private final Environment environment;
    private final Supplier<Console> consoleSupplier;

    @Autowired
    public AdminBootstrapCommand(AdminBootstrapService bootstrapService, Environment environment) {
        this(bootstrapService, environment, System::console);
    }

    AdminBootstrapCommand(
            AdminBootstrapService bootstrapService, Environment environment,
            Supplier<Console> consoleSupplier) {
        this.bootstrapService = bootstrapService;
        this.environment = environment;
        this.consoleSupplier = consoleSupplier;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!arguments.containsOption(BOOTSTRAP_OPTION)) {
            return;
        }

        String email = singleOption(arguments, EMAIL_OPTION);
        String password = environment.getProperty(PASSWORD_ENV);
        if (password != null) {
            bootstrapService.bootstrap(email, password);
            return;
        }

        Console console = consoleSupplier.get();
        if (console == null) {
            throw new IllegalStateException(
                    "Bootstrap administrativo exige console interativo ou APP_ADMIN_BOOTSTRAP_PASSWORD.");
        }
        char[] passwordChars = console.readPassword("Senha do administrador: ");
        if (passwordChars == null) {
            throw new IllegalStateException("A senha de bootstrap não foi fornecida.");
        }
        try {
            bootstrapService.bootstrap(email, new String(passwordChars));
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    private String singleOption(ApplicationArguments arguments, String option) {
        List<String> values = arguments.getOptionValues(option);
        if (values == null || values.size() != 1 || values.get(0) == null || values.get(0).isBlank()) {
            throw new IllegalArgumentException("Informe exatamente um e-mail para o bootstrap administrativo.");
        }
        return values.get(0);
    }
}
