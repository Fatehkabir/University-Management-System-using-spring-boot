package edu.kuet.ums.config;

import edu.kuet.ums.entity.*;
import edu.kuet.ums.entity.enums.Role;
import edu.kuet.ums.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final AdminRepository admins;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final DepartmentRepository departments;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final AnnouncementRepository announcements;
    private final PasswordEncoder encoder;

    @Value("${app.seed.admin-username}")
    private String adminUsername;
    @Value("${app.seed.admin-password}")
    private String adminPassword;
    @Value("${app.seed.admin-email}")
    private String adminEmail;
    @Value("${app.seed.demo-data}")
    private boolean demoData;

    @Override
    @Transactional
    public void run(String... args) {
        Admin admin = admins.findByUsername(adminUsername).orElse(null);
        if (admin == null && !users.existsByUsername(adminUsername)) {
            admin = admins.save(new Admin(adminUsername, encoder.encode(adminPassword), adminEmail,
                    "System Administrator", null, null, "ADM-001"));
            log.info("Created default admin user '{}'. Change its password after first login.", adminUsername);
        } else if (admin != null) {

            admin.setPassword(encoder.encode(adminPassword));
            admins.save(admin);
            log.info("Synchronized admin password for user '{}'.", adminUsername);
        }
        if (demoData && admin != null && departments.count() == 0) {
            seedDemo(admin);
        }
    }

    private void seedDemo(Admin admin) {
        Department cse = departments.save(Department.builder().name("Computer Science and Engineering").code("CSE").build());
        Department eee = departments.save(Department.builder().name("Electrical and Electronic Engineering").code("EEE").build());

        Teacher t1 = teachers.save(new Teacher("teacher1", encoder.encode("teacher123"), "teacher1@university.local",
                "Dr. Rahim Ahmed", "01711000001", "Khulna", "EMP-1001", cse, "Associate Professor",
                "PhD in Computer Science", LocalDate.of(2015, 7, 1)));
        Teacher t2 = teachers.save(new Teacher("teacher2", encoder.encode("teacher123"), "teacher2@university.local",
                "Ms. Nusrat Jahan", "01711000002", "Khulna", "EMP-1002", eee, "Lecturer",
                "MSc in Electrical Engineering", LocalDate.of(2020, 1, 15)));

        Student s1 = students.save(new Student("student1", encoder.encode("student123"), "student1@university.local",
                "Karim Uddin", "01811000001", "Khulna", "2207001", cse, "2022", 5));
        Student s2 = students.save(new Student("student2", encoder.encode("student123"), "student2@university.local",
                "Ayesha Siddika", "01811000002", "Dhaka", "2207002", cse, "2022", 5));
        Student s3 = students.save(new Student("student3", encoder.encode("student123"), "student3@university.local",
                "Tanvir Hasan", "01811000003", "Jessore", "2209001", eee, "2022", 3));

        Course db = courses.save(new Course("CSE-3101", "Database Systems", 3.0, cse, t1));
        Course se = courses.save(new Course("CSE-3201", "Software Engineering", 3.0, cse, t1));
        Course ec = courses.save(new Course("EEE-2101", "Electrical Circuits", 3.0, eee, t2));
        courses.save(new Course("CSE-4101", "Machine Learning", 3.0, cse, null));

        enrollments.save(new Enrollment(s1, db));
        enrollments.save(new Enrollment(s1, se));
        enrollments.save(new Enrollment(s2, db));
        enrollments.save(new Enrollment(s3, ec));

        announcements.save(new Announcement("Welcome to the new semester",
                "Classes begin on Sunday. Check your course list and contact your department for any changes.",
                admin, null));
        announcements.save(new Announcement("Faculty meeting",
                "All teachers are requested to attend the faculty meeting on Thursday at 11:00 AM.",
                admin, Role.TEACHER));
        announcements.save(new Announcement("Course registration deadline",
                "Students can add or drop courses from their dashboard until the end of next week.",
                admin, Role.STUDENT));
        log.info("Seeded demo data (teacher1/teacher123, student1/student123).");
    }
}

