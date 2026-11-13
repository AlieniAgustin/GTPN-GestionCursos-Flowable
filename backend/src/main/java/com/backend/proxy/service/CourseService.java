package com.backend.proxy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.CompanyContactDTO;
import com.backend.proxy.dto.CourseDetailsUpdateDTO;
import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.dto.FlowableProcessInstanceResponse;
import com.backend.proxy.dto.FlowableVariable;
import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.Course;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.repository.CourseRepository;
import com.backend.proxy.types.CourseStatus;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final String PROCESS_KEY = "Process_Curso";

    private final CourseRepository courseRepository;
    private final CompanyRepository companyRepository;
    private final FlowableClient flowableClient;

    /**
     * Registra la solicitud e inicia su instancia de proceso (enunciado 4.1.1).
     *
     * <p>Devuelve un DTO en vez de la entidad: el mapeo ocurre dentro de la transacción,
     * así la asociación lazy {@code company} ya está cargada cuando se serializa.
     */
    @Transactional
    public CourseResponseDTO processCourse(CourseRequestDTO request) {
        CompanyContactDTO contact = request.contact();
        Company company = getOrCreate(contact);
        Course course = new Course(company, request.name(), request.description(), request.possibleDates());
        course = courseRepository.save(course);

        FlowableProcessInstanceResponse response = flowableClient.startProcessInstance(
                PROCESS_KEY,
                String.valueOf(course.getId()),
                List.of(
                        new FlowableVariable("courseId", course.getId()),
                        new FlowableVariable("courseName", course.getName()),
                        new FlowableVariable("companyName", company.getName()),
                        new FlowableVariable("companyEmail", company.getEmail())
                )
        );

        if (response != null && response.id() != null) {
            course.setProcessInstanceId(response.id());
            course = courseRepository.save(course);
        }

        return new CourseResponseDTO(
                course.getId(),
                course.getProcessInstanceId(),
                course.getName(),
                course.getDescription(),
                course.getStatus(),
                company.getName()
        );
    }

    public void updateCourse(String processId, CourseStatus courseStatus) {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("Process instance ID must not be blank");
        }
        if (courseStatus == null) {
            throw new IllegalArgumentException("Course status is required");
        }
        Course course = courseRepository.findByprocessInstanceId(processId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        course.transitionTo(courseStatus);
        courseRepository.save(course);
    }

    /**
     * Vuelca a la BD de la institucion los datos que el proceso produjo (enunciado 3.2).
     * La llama el motor por la API interna cada vez que una etapa genera informacion
     * nueva, asi que es idempotente y solo pisa los campos que llegan con valor.
     */
    @Transactional
    public void updateCourseDetails(String processId, CourseDetailsUpdateDTO details) {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("Process instance ID must not be blank");
        }
        if (details == null || details.isEmpty()) {
            return;
        }
        Course course = courseRepository.findByprocessInstanceId(processId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No course linked to process instance " + processId));

        course.applyDetails(details.program(), details.budget(), details.agreedDate(), details.report());
        courseRepository.save(course);
    }

    public Company getOrCreate(CompanyContactDTO contact) {
        return companyRepository.findByName(contact.name())
            .orElseGet(() -> companyRepository.save(new Company(
                contact.name(),
                contact.phone(),
                contact.email()
            )));
    }
}
