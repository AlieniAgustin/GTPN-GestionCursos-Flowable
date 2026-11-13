package com.flowable;

import java.util.List;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.App;
import com.flowable.app.config.IdentitySeeder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The identity seed must also run on the {@code prod} profile, which is the one the
 * Docker deployment uses: a freshly created database has to come up with the five users
 * and the two groups of section 3.3 without any manual bootstrap step.
 */
@SpringBootTest(classes = App.class)
@ActiveProfiles("prod")
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:identity_seed;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class IdentitySeederTest {

    @Autowired
    private IdentityService identityService;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("Seed: on the prod profile the five users and the two groups are created")
    void testSeedRunsOnProdProfile() {
        assertNotNull(identityService.createGroupQuery().groupId(IdentitySeeder.GROUP_ADMINISTRACION).singleResult(),
                "Group 'administracion' should have been seeded");
        assertNotNull(identityService.createGroupQuery().groupId(IdentitySeeder.GROUP_DOCENTES).singleResult(),
                "Group 'docentes' should have been seeded");

        List<String> administrativos = List.of("jefeadministrativo", "administrativo1");
        List<String> docentes = List.of("jefedocente", "docente1", "docente2");

        administrativos.forEach(userId -> assertMember(userId, IdentitySeeder.GROUP_ADMINISTRACION));
        docentes.forEach(userId -> assertMember(userId, IdentitySeeder.GROUP_DOCENTES));

        // Section 3.3: exactly five business users, plus the technical account used by the BFF
        List<User> users = identityService.createUserQuery().list();
        assertEquals(administrativos.size() + docentes.size() + 1, users.size(),
                "Only the five business users and the technical account should exist");

        assertTrue(identityService.checkPassword("administrativo1", IdentitySeeder.DEFAULT_PASSWORD),
                "Business users should log in with the default password");
    }

    private void assertMember(String userId, String groupId) {
        assertNotNull(identityService.createUserQuery().userId(userId).singleResult(),
                "User '" + userId + "' should have been seeded");
        List<Group> groups = identityService.createGroupQuery().groupMember(userId).list();
        assertTrue(groups.stream().anyMatch(group -> groupId.equals(group.getId())),
                "User '" + userId + "' should belong to group '" + groupId + "'");
    }
}
