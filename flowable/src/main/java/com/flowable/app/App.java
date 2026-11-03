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


    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Profile("dev")
    @Bean
    CommandLineRunner initGroups(IdentityService repository) {
        List<String> docentes = List.of("docente1", "docente2", "jefedocente");
        List<String> administrativos = List.of("jefeadministrativo", "administrativo1", "administrativo2");
        Group administracionGroup = repository.newGroup(ADMINISTRACION_ID);
        administracionGroup.setName("Administracion");
        repository.saveGroup(administracionGroup);

        Group docentesGroup = repository.newGroup(DOCENTES_ID);
        docentesGroup.setName("Docentes");
        repository.saveGroup(docentesGroup);
        return args -> {
            docentes.forEach(docente -> {
                User docenteUser = repository.newUser(docente);
                repository.saveUser(docenteUser);
                repository.createMembership(docente, DOCENTES_ID);
            });

            administrativos.forEach(administrativo -> {
                User administrativoUser = repository.newUser(administrativo);
                repository.saveUser(administrativoUser);
                repository.createMembership(administrativo, ADMINISTRACION_ID);
            });
        };
    }
}