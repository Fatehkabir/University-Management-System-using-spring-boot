package edu.kuet.ums.service;

import edu.kuet.ums.dto.Responses.CourseCount;
import edu.kuet.ums.dto.Responses.NameCount;
import edu.kuet.ums.dto.Responses.StatsResponse;
import edu.kuet.ums.dto.Responses.TeacherCourseStat;
import edu.kuet.ums.dto.Responses.TeacherStatsResponse;
import edu.kuet.ums.entity.Course;
import edu.kuet.ums.entity.enums.AttendanceStatus;
import edu.kuet.ums.entity.enums.EnrollmentStatus;
import edu.kuet.ums.repository.AttendanceRepository;
import edu.kuet.ums.repository.CourseRepository;
import edu.kuet.ums.repository.DepartmentRepository;
import edu.kuet.ums.repository.EnrollmentRepository;
import edu.kuet.ums.repository.StudentRepository;
import edu.kuet.ums.repository.TeacherRepository;
import edu.kuet.ums.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private static final Set<AttendanceStatus> ATTENDED = Set.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE);
    private static final int TOP_COURSES = 6;

    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final CourseRepository courses;
    private final DepartmentRepository departments;
    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendance;
    private final UserRepository users;

    public StatsResponse stats() {
        List<NameCount> byDepartment = students.countByDepartment().stream()
                .map(r -> new NameCount((String) r[0], ((Number) r[1]).longValue()))
                .toList();
        List<CourseCount> top = enrollments.topCourses(EnrollmentStatus.DROPPED, PageRequest.of(0, TOP_COURSES))
                .stream()
                .map(r -> new CourseCount((String) r[0], (String) r[1], ((Number) r[2]).longValue()))
                .toList();

        return new StatsResponse(
                teachers.count(), students.count(), courses.count(), departments.count(),
                enrollments.count(),
                enrollments.countByStatus(EnrollmentStatus.ACTIVE),
                enrollments.countByStatus(EnrollmentStatus.COMPLETED),
                enrollments.countByStatus(EnrollmentStatus.DROPPED),
                courses.countByTeacherIsNull(),
                users.countByEnabledFalse(),
                percent(attendance.countByStatusIn(ATTENDED), attendance.count()),
                byDepartment, top);
    }

    public TeacherStatsResponse teacherStats(String teacherUsername) {
        List<Course> mine = courses.findByTeacherUsername(teacherUsername);
        List<TeacherCourseStat> perCourse = new ArrayList<>();
        long totalStudents = 0;
        long totalGraded = 0;
        long totalClasses = 0;
        long totalAttended = 0;

        for (Course c : mine) {
            long enrolled = enrollments.countByCourseIdAndStatusNot(c.getId(), EnrollmentStatus.DROPPED);
            long graded = enrollments.countByCourseIdAndGradeIsNotNull(c.getId());
            long classes = attendance.countByCourseId(c.getId());
            long attended = attendance.countByCourseIdAndStatusIn(c.getId(), ATTENDED);
            perCourse.add(new TeacherCourseStat(c.getId(), c.getCode(), c.getTitle(), c.getCreditHours(),
                    enrolled, graded, percent(attended, classes)));
            totalStudents += enrolled;
            totalGraded += graded;
            totalClasses += classes;
            totalAttended += attended;
        }
        return new TeacherStatsResponse(mine.size(), totalStudents, totalGraded,
                percent(totalAttended, totalClasses), perCourse);
    }

    private static Double percent(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return Math.round(part * 1000.0 / whole) / 10.0;
    }
}

