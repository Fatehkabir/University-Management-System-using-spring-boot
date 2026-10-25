package edu.kuet.ums;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndCrudIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private ResultActions get(String url, String token) throws Exception {

        return mvc.perform(MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token));
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b,
                               String token, String body) throws Exception {
        return mvc.perform(b.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void loginRejectsBadPasswordAndAnonymousIsBlocked() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.get("/api/admin/teachers")).andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.get("/api/admin/teachers").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleSeparationIsEnforced() throws Exception {
        String admin = login("admin", "admin123");
        String teacher = login("teacher1", "teacher123");
        String student = login("student1", "student123");

        get("/api/admin/teachers", admin).andExpect(status().isOk());
        get("/api/admin/teachers", teacher).andExpect(status().isForbidden());
        get("/api/admin/students", student).andExpect(status().isForbidden());
        get("/api/teacher/courses", student).andExpect(status().isForbidden());
        get("/api/student/enrollments", teacher).andExpect(status().isForbidden());
        get("/api/teacher/courses", teacher).andExpect(status().isOk());
        get("/api/student/enrollments", student).andExpect(status().isOk());
    }

    @Test
    void onlyAdminCanChangeTeacherAndStudentInfo() throws Exception {
        String admin = login("admin", "admin123");
        String teacher = login("teacher1", "teacher123");
        String student = login("student1", "student123");

        JsonNode students = json.readTree(get("/api/admin/students", admin).andReturn().getResponse().getContentAsString());
        JsonNode s1 = null;
        for (JsonNode s : students) if (s.get("username").asText().equals("student2")) s1 = s;
        assertThat(s1).isNotNull();
        long id = s1.get("id").asLong();
        String update = "{\"username\":\"student2\",\"email\":\"student2@university.local\",\"fullName\":\"Ayesha Updated\","
                + "\"studentId\":\"2207002\",\"departmentId\":" + s1.get("departmentId").asLong() + ",\"semester\":6}";

        send(put("/api/admin/students/" + id), teacher, update).andExpect(status().isForbidden());
        send(put("/api/admin/students/" + id), student, update).andExpect(status().isForbidden());
        send(delete("/api/admin/students/" + id), student, "{}").andExpect(status().isForbidden());

        send(put("/api/admin/students/" + id), admin, update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ayesha Updated"))
                .andExpect(jsonPath("$.semester").value(6));
    }

    @Test
    void adminCrudAndValidation() throws Exception {
        String admin = login("admin", "admin123");

        String dep = send(post("/api/admin/departments"), admin, "{\"name\":\"Physics\",\"code\":\"phy\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("PHY"))
                .andReturn().getResponse().getContentAsString();
        long depId = json.readTree(dep).get("id").asLong();

        String teacherBody = "{\"username\":\"newteacher\",\"password\":\"secret1\",\"email\":\"nt@university.local\","
                + "\"fullName\":\"New Teacher\",\"employeeId\":\"EMP-9\",\"departmentId\":" + depId + "}";
        String created = send(post("/api/admin/teachers"), admin, teacherBody)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long tid = json.readTree(created).get("id").asLong();
        assertThat(created).doesNotContain("password");

        send(post("/api/admin/teachers"), admin, teacherBody).andExpect(status().isConflict());
        send(post("/api/admin/teachers"), admin, teacherBody.replace("nt@university.local", "nope"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.email").exists());

        login("newteacher", "secret1");
        send(delete("/api/admin/departments/" + depId), admin, "{}").andExpect(status().isConflict());

        send(delete("/api/admin/teachers/" + tid), admin, "{}").andExpect(status().isNoContent());
        send(delete("/api/admin/departments/" + depId), admin, "{}").andExpect(status().isNoContent());
        get("/api/admin/teachers/" + tid, admin).andExpect(status().isNotFound());
    }

    @Test
    void teacherOwnershipGradingAttendanceAndStudentEnrollment() throws Exception {
        String admin = login("admin", "admin123");
        String t1 = login("teacher1", "teacher123");
        String t2 = login("teacher2", "teacher123");
        String student = login("student1", "student123");

        long dbCourse = courseId(admin, "CSE-3101");
        long eeeCourse = courseId(admin, "EEE-2101");

        String roster = get("/api/teacher/courses/" + dbCourse + "/students", t1)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode first = json.readTree(roster).get(0);
        get("/api/teacher/courses/" + dbCourse + "/students", t2).andExpect(status().isForbidden());

        long enrollmentId = first.get("id").asLong();
        send(put("/api/teacher/enrollments/" + enrollmentId + "/grade"), t2, "{\"grade\":\"A\"}")
                .andExpect(status().isForbidden());
        send(put("/api/teacher/enrollments/" + enrollmentId + "/grade"), t1, "{\"grade\":\"Z\"}")
                .andExpect(status().isBadRequest());
        send(put("/api/teacher/enrollments/" + enrollmentId + "/grade"), t1, "{\"grade\":\"A\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));

        long studentId = first.get("studentId").asLong();
        String today = java.time.LocalDate.now().toString();

        send(post("/api/teacher/courses/" + dbCourse + "/attendance"), t1,
                "{\"date\":\"" + today + "\",\"entries\":[{\"studentId\":" + studentId + ",\"status\":\"PRESENT\"}]}")
                .andExpect(status().isBadRequest());

        String enrolled = send(post("/api/student/enrollments"), student, "{\"courseId\":" + eeeCourse + "}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long newEnrollment = json.readTree(enrolled).get("id").asLong();
        send(post("/api/student/enrollments"), student, "{\"courseId\":" + eeeCourse + "}")
                .andExpect(status().isConflict());
        send(post("/api/teacher/courses/" + eeeCourse + "/attendance"), t2,
                "{\"date\":\"" + today + "\",\"entries\":[{\"studentId\":" + json.readTree(enrolled).get("studentId").asLong()
                        + ",\"status\":\"LATE\"}]}").andExpect(status().isOk());
        get("/api/student/attendance", student).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("LATE"));
        send(delete("/api/student/enrollments/" + newEnrollment), student, "{}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DROPPED"));
    }

    @Test
    void announcementsAreFilteredByRole() throws Exception {
        String admin = login("admin", "admin123");
        String teacher = login("teacher1", "teacher123");
        String student = login("student1", "student123");

        assertThat(titles(get("/api/announcements", admin))).contains("Faculty meeting", "Course registration deadline");
        assertThat(titles(get("/api/announcements", teacher)))
                .contains("Faculty meeting").doesNotContain("Course registration deadline");
        assertThat(titles(get("/api/announcements", student)))
                .contains("Course registration deadline").doesNotContain("Faculty meeting");
    }

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        String student = login("student3", "student123");
        send(post("/api/auth/change-password"), student, "{\"currentPassword\":\"bad\",\"newPassword\":\"newpass1\"}")
                .andExpect(status().isBadRequest());
        send(post("/api/auth/change-password"), student, "{\"currentPassword\":\"student123\",\"newPassword\":\"newpass1\"}")
                .andExpect(status().isNoContent());
        login("student3", "newpass1");
    }

    private long firstDepartmentId() throws Exception {
        String body = mvc.perform(MockMvcRequestBuilders.get("/api/public/signup-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("departments").get(0).get("id").asLong();
    }

    private String studentSignup(String username, String studentId, long dep) {
        return "{\"role\":\"STUDENT\",\"username\":\"" + username + "\",\"password\":\"secret1\",\"email\":\""
                + username + "@university.local\",\"fullName\":\"" + username + " Person\",\"departmentId\":" + dep
                + ",\"studentId\":\"" + studentId + "\",\"batch\":\"2024\",\"semester\":2}";
    }

    private String teacherSignup(String username, String employeeId, long dep) {
        return "{\"role\":\"TEACHER\",\"username\":\"" + username + "\",\"password\":\"secret1\",\"email\":\""
                + username + "@university.local\",\"fullName\":\"" + username + " Person\",\"departmentId\":" + dep
                + ",\"employeeId\":\"" + employeeId + "\",\"designation\":\"Lecturer\"}";
    }

    private ResultActions anonymousPost(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void publicSignupCreatesStudentAndTeacherWhoCanLogInImmediately() throws Exception {
        long dep = firstDepartmentId();

        anonymousPost("/api/auth/register", studentSignup("signup_stu", "SU-1", dep))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvalRequired").value(false));
        anonymousPost("/api/auth/register", teacherSignup("signup_tea", "SE-1", dep))
                .andExpect(status().isCreated());

        String stu = login("signup_stu", "secret1");
        String tea = login("signup_tea", "secret1");
        get("/api/student/enrollments", stu).andExpect(status().isOk());
        get("/api/teacher/courses", tea).andExpect(status().isOk());
        get("/api/admin/students", stu).andExpect(status().isForbidden());
    }

    @Test
    void publicSignupRejectsDuplicatesMissingIdsAndAdminRole() throws Exception {
        long dep = firstDepartmentId();
        anonymousPost("/api/auth/register", studentSignup("dup_stu", "DS-1", dep)).andExpect(status().isCreated());

        anonymousPost("/api/auth/register", studentSignup("dup_stu", "DS-2", dep)).andExpect(status().isConflict());

        anonymousPost("/api/auth/register", studentSignup("no_id", "x", dep).replace(",\"studentId\":\"x\"", ""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.studentId").exists());

        anonymousPost("/api/auth/register", teacherSignup("no_emp", "x", dep).replace(",\"employeeId\":\"x\"", ""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.employeeId").exists());

        anonymousPost("/api/auth/register", studentSignup("evil", "EV-1", dep).replace("STUDENT", "ADMIN"))
                .andExpect(status().isBadRequest());

        anonymousPost("/api/auth/register", studentSignup("shorty", "SH-1", dep).replace("secret1", "123"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void adminCanTurnATeacherIntoAStudent() throws Exception {
        String admin = login("admin", "admin123");
        long dep = firstDepartmentId();
        String created = anonymousPost("/api/auth/register", teacherSignup("to_student", "TS-1", dep))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(created).isNotBlank();
        long id = userId(admin, "/api/admin/teachers", "to_student");

        send(post("/api/admin/courses"), admin, "{\"code\":\"RC-100\",\"title\":\"Role change course\","
                + "\"creditHours\":3,\"departmentId\":" + dep + ",\"teacherId\":" + id + "}")
                .andExpect(status().isCreated());

        String oldToken = login("to_student", "secret1");
        get("/api/teacher/courses", oldToken).andExpect(status().isOk());

        send(post("/api/admin/teachers/" + id + "/convert-to-student"), oldToken, "{\"studentId\":\"RC-S1\"}")
                .andExpect(status().isForbidden());
        send(post("/api/admin/teachers/" + id + "/convert-to-student"), admin, "{\"studentId\":\"2207001\"}")
                .andExpect(status().isConflict());
        send(post("/api/admin/teachers/" + id + "/convert-to-student"), admin, "{\"batch\":\"2024\"}")
                .andExpect(status().isBadRequest());

        send(post("/api/admin/teachers/" + id + "/convert-to-student"), admin,
                "{\"studentId\":\"RC-S1\",\"batch\":\"2024\",\"semester\":3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.studentId").value("RC-S1"))
                .andExpect(jsonPath("$.semester").value(3))
                .andExpect(jsonPath("$.username").value("to_student"));

        String newToken = login("to_student", "secret1");
        get("/api/student/enrollments", newToken).andExpect(status().isOk());
        get("/api/teacher/courses", newToken).andExpect(status().isForbidden());
        get("/api/admin/teachers/" + id, admin).andExpect(status().isNotFound());
        get("/api/admin/students/" + id, admin).andExpect(status().isOk());

        JsonNode courses = json.readTree(get("/api/courses", admin).andReturn().getResponse().getContentAsString());
        for (JsonNode c : courses) {
            if (c.get("code").asText().equals("RC-100")) {
                assertThat(c.get("teacherId").isNull()).isTrue();
            }
        }
    }

    @Test
    void adminCanTurnAStudentIntoATeacherButMustConfirmDeletingRecords() throws Exception {
        String admin = login("admin", "admin123");
        long dep = firstDepartmentId();
        anonymousPost("/api/auth/register", studentSignup("to_teacher", "TT-1", dep)).andExpect(status().isCreated());
        long id = userId(admin, "/api/admin/students", "to_teacher");

        String stu = login("to_teacher", "secret1");
        long course = courseId(admin, "EEE-2101");
        send(post("/api/student/enrollments"), stu, "{\"courseId\":" + course + "}").andExpect(status().isCreated());

        send(post("/api/admin/students/" + id + "/convert-to-teacher"), admin, "{\"employeeId\":\"TT-E1\"}")
                .andExpect(status().isConflict());

        send(post("/api/admin/students/" + id + "/convert-to-teacher"), stu,
                "{\"employeeId\":\"TT-E1\",\"deleteRecords\":true}").andExpect(status().isForbidden());

        send(post("/api/admin/students/" + id + "/convert-to-teacher"), admin,
                "{\"employeeId\":\"TT-E1\",\"designation\":\"Lecturer\",\"deleteRecords\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.employeeId").value("TT-E1"))
                .andExpect(jsonPath("$.designation").value("Lecturer"));

        String tea = login("to_teacher", "secret1");
        get("/api/teacher/courses", tea).andExpect(status().isOk());
        get("/api/student/enrollments", tea).andExpect(status().isForbidden());
        get("/api/admin/students/" + id, admin).andExpect(status().isNotFound());
        get("/api/admin/teachers/" + id, admin).andExpect(status().isOk());
    }

    @Test
    void dashboardStatsExposeTheNewBreakdowns() throws Exception {
        String admin = login("admin", "admin123");
        String teacher = login("teacher1", "teacher123");
        String student = login("student1", "student123");

        get("/api/admin/stats", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.teachers").isNumber())
                .andExpect(jsonPath("$.activeEnrollments").isNumber())
                .andExpect(jsonPath("$.coursesWithoutTeacher").isNumber())
                .andExpect(jsonPath("$.studentsByDepartment").isArray())
                .andExpect(jsonPath("$.topCourses").isArray());
        get("/api/admin/stats", teacher).andExpect(status().isForbidden());

        get("/api/teacher/stats", teacher).andExpect(status().isOk())
                .andExpect(jsonPath("$.courses").isNumber())
                .andExpect(jsonPath("$.courseStats").isArray());
        get("/api/teacher/stats", student).andExpect(status().isForbidden());
    }

    private long userId(String adminToken, String listUrl, String username) throws Exception {
        JsonNode all = json.readTree(get(listUrl, adminToken).andReturn().getResponse().getContentAsString());
        for (JsonNode n : all) if (n.get("username").asText().equals(username)) return n.get("id").asLong();
        throw new AssertionError("user not found " + username);
    }

    private long courseId(String token, String code) throws Exception {
        JsonNode courses = json.readTree(get("/api/courses", token).andReturn().getResponse().getContentAsString());
        for (JsonNode c : courses) if (c.get("code").asText().equals(code)) return c.get("id").asLong();
        throw new AssertionError("course not found " + code);
    }

    private java.util.List<String> titles(ResultActions r) throws Exception {
        JsonNode arr = json.readTree(r.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        java.util.List<String> out = new java.util.ArrayList<>();
        arr.forEach(n -> out.add(n.get("title").asText()));
        return out;
    }
}

