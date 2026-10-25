package edu.kuet.ums.mapper;

import edu.kuet.ums.dto.Responses.*;
import edu.kuet.ums.entity.*;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public DepartmentResponse toDepartment(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getCode());
    }

    public TeacherResponse toTeacher(Teacher t) {
        return new TeacherResponse(t.getId(), t.getUsername(), t.getEmail(), t.getFullName(), t.getPhone(),
                t.getAddress(), t.isEnabled(), t.getEmployeeId(), t.getDepartment().getId(),
                t.getDepartment().getName(), t.getDesignation(), t.getQualification(), t.getJoiningDate());
    }

    public StudentResponse toStudent(Student s) {
        return new StudentResponse(s.getId(), s.getUsername(), s.getEmail(), s.getFullName(), s.getPhone(),
                s.getAddress(), s.isEnabled(), s.getStudentId(), s.getDepartment().getId(),
                s.getDepartment().getName(), s.getBatch(), s.getSemester());
    }

    public CourseResponse toCourse(Course c) {
        Teacher t = c.getTeacher();
        return new CourseResponse(c.getId(), c.getCode(), c.getTitle(), c.getCreditHours(),
                c.getDepartment().getId(), c.getDepartment().getName(),
                t == null ? null : t.getId(), t == null ? null : t.getFullName());
    }

    public EnrollmentResponse toEnrollment(Enrollment e) {
        Course c = e.getCourse();
        Student s = e.getStudent();
        return new EnrollmentResponse(e.getId(), s.getId(), s.getFullName(), s.getStudentId(), c.getId(),
                c.getCode(), c.getTitle(), c.getTeacher() == null ? null : c.getTeacher().getFullName(),
                e.getStatus(), e.getGrade(), e.getEnrolledAt(), c.getCreditHours());
    }

    public AttendanceResponse toAttendance(Attendance a) {
        return new AttendanceResponse(a.getId(), a.getStudent().getId(), a.getStudent().getFullName(),
                a.getCourse().getId(), a.getCourse().getCode(), a.getDate(), a.getStatus());
    }

    public AnnouncementResponse toAnnouncement(Announcement a) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getContent(), a.getTargetRole(),
                a.getPostedBy().getFullName(), a.getCreatedAt());
    }
}

