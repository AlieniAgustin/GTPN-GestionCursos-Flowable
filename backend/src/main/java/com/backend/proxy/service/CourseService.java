package com.backend.proxy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.CompanyContactDTO;
import com.backend.proxy.dto.CourseDetailsUpdateDTO;
import com.backend.proxy.dto.CourseProcessStartRequest;
import com.backend.proxy.dto.CourseProcessStartResponse;
import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.CourseResponseDTO;
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

    private final CourseRepository courseRepository;
    private final CompanyRepository companyRepository;
    private final FlowableClient flowableClient;

    @Transactional
    public CourseResponseDTO processCourse(CourseRequestDTO request) {
        CompanyContactDTO contact = request.contact();
        Company company = getOrCreate(contact);
        Course course = new Course(company, request.name(), request.description(), request.possibleDates());
        course = courseRepository.save(course);

        CourseProcessStartResponse response = flowableClient.startCourseProcess(
                new CourseProcessStartRequest(
                        course.getId(),
                        course.getName(),
                        company.getName(),
                        company.getEmail()
                )
        );

        if (response != null && response.processInstanceId() != null) {
            course.setProcessInstanceId(response.processInstanceId());
            course = courseRepository.save(course);
        }
        return CourseResponseDTO.from(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDTO> findVisibleFor(String username, boolean isCompany) {
        List<Course> courses = isCompany
                ? courseRepository.findByCompanyAccessUsernameOrderByIdDesc(username)
                : courseRepository.findAllByOrderByIdDesc();

        return courses.stream().map(CourseResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponseDTO findOneFor(Long courseId, String username, boolean isCompany) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if (isCompany && !username.equals(course.getCompany().getAccessUsername())) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }

        return CourseResponseDTO.from(course);
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
