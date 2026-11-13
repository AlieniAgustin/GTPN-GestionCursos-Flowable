package com.flowable.app.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Creates the institution's users and groups required by the assignment (section 3.3)
 * directly in Flowable's IDM tables.
 *
 * <p>Runs on every profile, including {@code prod}, so a freshly created database is
 * usable right after {@code docker compose up} without any manual bootstrap step. The
 * seeder is idempotent: it only creates what is missing, both for users/groups and for
 * memberships, so restarting the app (or running it against an already seeded database)
 * is harmless.
 *
 * <p>Set {@code institucion.seed.enabled=false} to skip it.
 */
@Component
@Order(0)
@ConditionalOnProperty(name = "institucion.seed.enabled", havingValue = "true", matchIfMissing = true)
public class IdentitySeeder implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(IdentitySeeder.class);

    public static final String GROUP_ADMINISTRACION = "administracion";
    public static final String GROUP_DOCENTES = "docentes";

    /** Password shared by the five business users. Demo only. */
    public static final String DEFAULT_PASSWORD = "password";

    /** Technical account used by the BFF to call the engine's REST API. */
    private static final String TECHNICAL_USER = "admin";
    private static final String TECHNICAL_PASSWORD = "test";

    /**
     * Section 3.3: Administracion has 2 people (one of them is the administrative chief)
     * and Docentes has 3 (one of them is the team chief). Five users in total.
     */
    private static final List<String> ADMINISTRATIVOS = List.of("jefeadministrativo", "administrativo1");
    private static final List<String> DOCENTES = List.of("jefedocente", "docente1", "docente2");

    private final IdentityService identityService;

    public IdentitySeeder(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void run(String... args) {
        Map<String, String> groups = new LinkedHashMap<>();
        groups.put(GROUP_ADMINISTRACION, "Administracion");
        groups.put(GROUP_DOCENTES, "Docentes");
        groups.forEach(this::ensureGroup);

        ensureUser(TECHNICAL_USER, TECHNICAL_PASSWORD, "Admin", "System");

        ADMINISTRATIVOS.forEach(userId -> {
            ensureUser(userId, DEFAULT_PASSWORD, null, null);
            ensureMembership(userId, GROUP_ADMINISTRACION);
        });

        DOCENTES.forEach(userId -> {
            ensureUser(userId, DEFAULT_PASSWORD, null, null);
            ensureMembership(userId, GROUP_DOCENTES);
        });

        LOG.info("Identity seed finished: groups {} and {} users available",
                groups.keySet(), ADMINISTRATIVOS.size() + DOCENTES.size() + 1);
    }

    private void ensureGroup(String groupId, String groupName) {
        if (identityService.createGroupQuery().groupId(groupId).count() > 0) {
            return;
        }
        Group group = identityService.newGroup(groupId);
        group.setName(groupName);
        identityService.saveGroup(group);
        LOG.info("Seeded group '{}'", groupId);
    }

    private void ensureUser(String userId, String password, String firstName, String lastName) {
        if (identityService.createUserQuery().userId(userId).count() > 0) {
            return;
        }
        User user = identityService.newUser(userId);
        user.setPassword(password);
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        identityService.saveUser(user);
        LOG.info("Seeded user '{}'", userId);
    }

    private void ensureMembership(String userId, String groupId) {
        boolean alreadyMember = identityService.createGroupQuery()
                .groupMember(userId)
                .groupId(groupId)
                .count() > 0;
        if (alreadyMember) {
            return;
        }
        identityService.createMembership(userId, groupId);
        LOG.info("Seeded membership '{}' -> '{}'", userId, groupId);
    }
}
