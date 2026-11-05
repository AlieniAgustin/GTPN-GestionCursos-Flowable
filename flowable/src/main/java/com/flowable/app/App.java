package com.flowable.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import java.util.List;

import org.flowable.engine.IdentityService;

@SpringBootApplication
public class App {
    private static final String DOCENTES_ID = "docentes";
    private static final String ADMINISTRACION_ID = "administracion";
    public static final String DEFAULT_PASSWORD = "password";

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Profile("dev")
    @Bean
    CommandLineRunner initGroups(IdentityService repository) {
        return args -> {
            if (repository.createGroupQuery().groupId(ADMINISTRACION_ID).count() == 0) {
                Group administracionGroup = repository.newGroup(ADMINISTRACION_ID);
                administracionGroup.setName("Administracion");
                repository.saveGroup(administracionGroup);
            }

            if (repository.createGroupQuery().groupId(DOCENTES_ID).count() == 0) {
                Group docentesGroup = repository.newGroup(DOCENTES_ID);
                docentesGroup.setName("Docentes");
                repository.saveGroup(docentesGroup);
            }

            List<String> docentes = List.of("docente1", "docente2", "jefedocente");
            List<String> administrativos = List.of("jefeadministrativo", "administrativo1", "administrativo2");

            if (repository.createUserQuery().userId("admin").count() == 0) {
                User adminUser = repository.newUser("admin");
                adminUser.setPassword("test");
                adminUser.setFirstName("Admin");
                adminUser.setLastName("System");
                repository.saveUser(adminUser);
            }

            docentes.forEach(docente -> {
                if (repository.createUserQuery().userId(docente).count() == 0) {
                    User docenteUser = repository.newUser(docente);
                    docenteUser.setPassword(DEFAULT_PASSWORD);
                    repository.saveUser(docenteUser);
                    repository.createMembership(docente, DOCENTES_ID);
                }
            });

            administrativos.forEach(administrativo -> {
                if (repository.createUserQuery().userId(administrativo).count() == 0) {
                    User administrativoUser = repository.newUser(administrativo);
                    administrativoUser.setPassword(DEFAULT_PASSWORD);
                    repository.saveUser(administrativoUser);
                    repository.createMembership(administrativo, ADMINISTRACION_ID);
                }
            });
        };
    }
}
