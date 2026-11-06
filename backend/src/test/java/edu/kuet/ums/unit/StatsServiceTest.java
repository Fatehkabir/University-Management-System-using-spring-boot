package edu.kuet.ums.unit;

import edu.kuet.ums.dto.Responses.StatsResponse;
import edu.kuet.ums.dto.Responses.TeacherStatsResponse;
import edu.kuet.ums.entity.Course;
import edu.kuet.ums.entity.enums.EnrollmentStatus;
import edu.kuet.ums.repository.*;
import edu.kuet.ums.service.StatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock TeacherRepository teachers;
    @Mock StudentRepository students;
    @Mock CourseRepository courses;
    @Mock DepartmentRepository departments;
    @Mock EnrollmentRepository enrollments;
    @Mock AttendanceRepository attendance;
    @Mock UserRepository users;

    private StatsService service() {
        return new StatsService(teachers, students, courses, departments, enrollments, attendance, users);
    }

    @Test
    void adminStatsCombineCountsAndRoundAttendanceToOneDecimal() {
        when(teachers.count()).thenReturn(2L);
        when(students.count()).thenReturn(3L);
        when(courses.count()).thenReturn(4L);
        when(departments.count()).thenReturn(2L);
        when(enrollments.count()).thenReturn(10L);
        when(enrollments.countByStatus(EnrollmentStatus.ACTIVE)).thenReturn(6L);
        when(enrollments.countByStatus(EnrollmentStatus.COMPLETED)).thenReturn(3L);
        when(enrollments.countByStatus(EnrollmentStatus.DROPPED)).thenReturn(1L);
        when(courses.countByTeacherIsNull()).thenReturn(1L);
        when(users.countByEnabledFalse()).thenReturn(2L);
        when(attendance.count()).thenReturn(3L);
        when(attendance.countByStatusIn(any(Collection.class))).thenReturn(2L);
        when(students.countByDepartment()).thenReturn(List.<Object[]>of(new Object[]{"CSE", 2L}));
        when(enrollments.topCourses(eq(EnrollmentStatus.DROPPED), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"CSE-3101", "Database Systems", 2L}));

        StatsResponse s = service().stats();

        assertThat(s.teachers()).isEqualTo(2);
        assertThat(s.activeEnrollments()).isEqualTo(6);
        assertThat(s.coursesWithoutTeacher()).isEqualTo(1);
        assertThat(s.disabledAccounts()).isEqualTo(2);
        assertThat(s.attendanceRate()).isEqualTo(66.7);
        assertThat(s.studentsByDepartment()).hasSize(1);
        assertThat(s.studentsByDepartment().get(0).name()).isEqualTo("CSE");
        assertThat(s.topCourses().get(0).code()).isEqualTo("CSE-3101");
    }

}
