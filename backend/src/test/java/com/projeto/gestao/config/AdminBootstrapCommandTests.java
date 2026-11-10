package com.projeto.gestao.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Console;
import java.util.Arrays;

import com.projeto.gestao.service.AdminBootstrapService;

import org.springframework.boot.ApplicationArguments;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class AdminBootstrapCommandTests {
    @Test
    void normalApplicationStartDoesNotRunBootstrap() {
        AdminBootstrapService service = mock(AdminBootstrapService.class);
        AdminBootstrapCommand command = new AdminBootstrapCommand(
                service, new MockEnvironment(), () -> null);

        command.run(new DefaultApplicationArguments());

        verifyNoInteractions(service);
    }

    @Test
    void automatedBootstrapReadsOnlyTheExplicitPasswordEnvironmentVariable() {
        AdminBootstrapService service = mock(AdminBootstrapService.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty(AdminBootstrapCommand.PASSWORD_ENV, "SenhaAdmin1!");
        AdminBootstrapCommand command = new AdminBootstrapCommand(
                service, environment, () -> null);

        command.run(new DefaultApplicationArguments(
                "--admin-bootstrap", "--admin-bootstrap-email=ADMIN@Example.COM"));

        verify(service).bootstrap("ADMIN@Example.COM", "SenhaAdmin1!");
    }

    @Test
    void bootstrapWithoutEnvironmentOrInteractiveConsoleFailsWithoutCallingService() {
        AdminBootstrapService service = mock(AdminBootstrapService.class);
        AdminBootstrapCommand command = new AdminBootstrapCommand(
                service, new MockEnvironment(), () -> null);

        assertThatThrownBy(() -> command.run(new DefaultApplicationArguments(
                "--admin-bootstrap", "--admin-bootstrap-email=admin@example.com")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Bootstrap administrativo exige console interativo ou APP_ADMIN_BOOTSTRAP_PASSWORD.");
        verifyNoInteractions(service);
    }

    @Test
    void interactiveBootstrapReadsAndPassesTheConsolePassword() {
        AdminBootstrapService service = mock(AdminBootstrapService.class);
        Console console = mock(Console.class);
        when(console.readPassword("Senha do administrador: "))
                .thenReturn("SenhaAdmin1!".toCharArray());
        AdminBootstrapCommand command = new AdminBootstrapCommand(
                service, new MockEnvironment(), () -> console);

        command.run(new DefaultApplicationArguments(
                "--admin-bootstrap", "--admin-bootstrap-email=admin@example.com"));

        verify(service).bootstrap("admin@example.com", "SenhaAdmin1!");
    }

    @Test
    void consoleBootstrapWithoutPasswordFailsWithoutCallingService() {
        AdminBootstrapService service = mock(AdminBootstrapService.class);
        Console console = mock(Console.class);
        when(console.readPassword("Senha do administrador: ")).thenReturn(null);
        AdminBootstrapCommand command = new AdminBootstrapCommand(
                service, new MockEnvironment(), () -> console);

        assertThatThrownBy(() -> command.run(new DefaultApplicationArguments(
                "--admin-bootstrap", "--admin-bootstrap-email=admin@example.com")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A senha de bootstrap não foi fornecida.");
        verifyNoInteractions(service);
    }

    @Test
    void invalidBootstrapEmailOptionsAreRejected() {
        AdminBootstrapService service = mock(AdminBootstrapService.class);
        AdminBootstrapCommand command = new AdminBootstrapCommand(
                service, new MockEnvironment(), () -> null);

        assertThatThrownBy(() -> command.run(new DefaultApplicationArguments("--admin-bootstrap")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> command.run(new DefaultApplicationArguments(
                "--admin-bootstrap", "--admin-bootstrap-email=one@example.com",
                "--admin-bootstrap-email=two@example.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> command.run(new DefaultApplicationArguments(
                "--admin-bootstrap", "--admin-bootstrap-email=")))
                .isInstanceOf(IllegalArgumentException.class);

        ApplicationArguments nullValueArguments = mock(ApplicationArguments.class);
        when(nullValueArguments.containsOption(AdminBootstrapCommand.BOOTSTRAP_OPTION)).thenReturn(true);
        when(nullValueArguments.getOptionValues(AdminBootstrapCommand.EMAIL_OPTION))
                .thenReturn(Arrays.asList((String) null));
        assertThatThrownBy(() -> command.run(nullValueArguments))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(service);
    }
}
