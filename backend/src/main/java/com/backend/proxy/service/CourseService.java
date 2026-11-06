package com.backend.proxy.service;

import org.springframework.stereotype.Service;

import com.backend.proxy.dto.CompanyContactDTO;
import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.Course;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.repository.CourseRepository;
import com.backend.proxy.types.CourseStatus;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CompanyRepository companyRepository;

    public CourseService(CompanyRepository companyRepository, CourseRepository courseRepository) {
        this.companyRepository = companyRepository;
        this.courseRepository = courseRepository;
    }

    public Course processCourse(CourseRequestDTO request) {
        CompanyContactDTO contact = request.contact();
        Company company = getOrCreate(contact);
        Course course = new Course(company, request.name(), request.description(), request.possibleDates());
        courseRepository.save(course);
        // send info to flowable and await a response for extract a process_id assigned
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
