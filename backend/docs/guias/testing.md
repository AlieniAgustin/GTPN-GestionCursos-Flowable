# Pruebas y Testing

El backend implementa una estrategia de pruebas balanceada y orientada a verificar comportamiento real: se prueban las reglas de negocio y restricciones de persistencia sobre bases de datos embebidas reales, mientras que la frontera externa con Flowable REST se aísla mediante mocks y dobles de prueba.

---

## Filosofía de Pruebas

> [!NOTE]
> **Persistencia Real vs. Antipatrón de Mockear Repositorios:**
> En lugar de mockear `CourseRepository` o `CompanyRepository` con Mockito (lo cual oculta errores de SQL, constraints nulas, mappings JPA y transaccionalidad), las pruebas de servicios utilizan `@DataJpaTest`. Los repositorios corren contra una base de datos real en memoria (H2 configurada para emular PostgreSQL), validando que las consultas y mutaciones sean genuinas.

---

## Estructura de Suites de Prueba

| Capa | Anotación / Estrategia | Qué se Verifica | Ejemplo |
|---|---|---|---|
| **Servicios de Negocio** | `@DataJpaTest` + Mocks en fronteras externas | Reglas de negocio, persistencia JPA real, mapeo de DTOs y llamadas salientes a Flowable. | `CourseServiceTest`, `AuthenticationServiceTest` |
| **Controladores REST** | `@WebMvcTest` | Serialización JSON, Bean Validation (`@Valid`), códigos HTTP, roles de seguridad. | `CourseControllerTest` |
| **Sitio Web / Thymeleaf** | `@WebMvcTest` + MockMvc | Resolución de vistas HTML, modelo de datos Thymeleaf, redirecciones de formularios. | `PublicSiteControllerTest`, `TaskSiteControllerTest` |
| **Filtros de Seguridad** | Tests unitarios y de integración | Validación de headers (`X-API-KEY`), parsing de payload JSON en login. | `JsonAuthenticationFilterTest` |

---

## Ejecución de Pruebas

### Ejecutar toda la suite del backend
```bash
cd backend
mvn test
```

### Ejecutar una suite específica
```bash
# Probar exclusivamente la capa de cursos
mvn test -Dtest=CourseServiceTest

# Probar la seguridad y autenticación
mvn test -Dtest=AuthenticationServiceTest,JsonAuthenticationFilterTest
```

---

## Ejemplo de Prueba con `@DataJpaTest`

```java
@DataJpaTest
class CourseServiceTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private FlowableClient flowableClient;
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        flowableClient = Mockito.mock(FlowableClient.class);
        courseService = new CourseService(courseRepository, companyRepository, flowableClient);
    }

    @Test
    @DisplayName("Debe persistir el curso y vincular la instancia de proceso")
    void shouldPersistCourseAndLinkProcessInstance() {
        // Arrange
        when(flowableClient.startCourseProcess(any()))
                .thenReturn(new CourseProcessStartResponse("proc-1234", "OK"));

        // Act
        CourseResponseDTO result = courseService.processCourse(validRequest);

        // Assert
        assertNotNull(result.id());
        assertEquals("proc-1234", result.processInstanceId());
        // Comprobación real en base de datos:
        Course saved = courseRepository.findById(result.id()).orElseThrow();
        assertEquals("proc-1234", saved.getProcessInstanceId());
    }
}
```

---

## Próximo Paso

- Conoce cómo extender el sistema y agregar nuevas tareas en [Convenciones y Extensión](convenciones.md).
