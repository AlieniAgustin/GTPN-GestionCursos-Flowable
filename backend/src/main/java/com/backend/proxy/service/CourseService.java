package com.backend.proxy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.CompanyContactDTO;
import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.FlowableProcessInstanceRequest;
import com.backend.proxy.dto.FlowableProcessInstanceResponse;
import com.backend.proxy.dto.FlowableVariable;
import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.Course;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.repository.CourseRepository;
import com.backend.proxy.types.CourseStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final String PROCESS_KEY = "Process_Curso";

    private final CourseRepository courseRepository;
    private final CompanyRepository companyRepository;
    private final FlowableClient flowableClient;

    public Course processCourse(CourseRequestDTO request) {
        CompanyContactDTO contact = request.contact();
        Company company = getOrCreate(contact);
        Course course = new Course(company, request.name(), request.description(), request.possibleDates());
        course = courseRepository.save(course);

        var flowableRequest = new FlowableProcessInstanceRequest(
                PROCESS_KEY,
                String.valueOf(course.getId()),
                List.of(
                        new FlowableVariable("courseId", course.getId()),
                        new FlowableVariable("courseName", course.getName())
                )
        );

        FlowableProcessInstanceResponse response = flowableClient.post()
                .uri("/process-api/runtime/process-instances")
                .body(flowableRequest)
                .retrieve()
                .body(FlowableProcessInstanceResponse.class);

        if (response != null && response.id() != null) {
            course.setProcessInstanceId(response.id());
            course = courseRepository.save(course);
        }

        return course;
    }

    public void updateCourse(String processId, CourseStatus courseStatus) {
        Course course = courseRepository.findByprocessInstanceId(processId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        course.setStatus(courseStatus);
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
