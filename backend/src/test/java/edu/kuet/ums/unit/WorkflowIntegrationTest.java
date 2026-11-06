package edu.kuet.ums.unit;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // ---------- helpers ----------

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

    private JsonNode read(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private long departmentId() throws Exception {
        return read(mvc.perform(MockMvcRequestBuilders.get("/api/public/signup-info")).andExpect(status().isOk()))
                .get("departments").get(0).get("id").asLong();
    }

    private void registerTeacher(String username, String employeeId, long dep) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(
                "{\"role\":\"TEACHER\",\"username\":\"" + username + "\",\"password\":\"secret1\",\"email\":\""
                        + username + "@university.local\",\"fullName\":\"" + username + " Person\",\"departmentId\":"
                        + dep + ",\"employeeId\":\"" + employeeId + "\"}")).andExpect(status().isCreated());
    }

    private void registerStudent(String username, String studentId, long dep) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(
                "{\"role\":\"STUDENT\",\"username\":\"" + username + "\",\"password\":\"secret1\",\"email\":\""
                        + username + "@university.local\",\"fullName\":\"" + username + " Person\",\"departmentId\":"
                        + dep + ",\"studentId\":\"" + studentId + "\"}")).andExpect(status().isCreated());
    }

    private long idOf(String admin, String listUrl, String username) throws Exception {
        for (JsonNode n : read(get(listUrl, admin).andExpect(status().isOk()))) {
            if (n.get("username").asText().equals(username)) return n.get("id").asLong();
        }
        throw new AssertionError("not found: " + username);
    }

    private long createCourseAndEnroll(String admin, String code, long dep, long teacherId, Long studentId)
            throws Exception {
        long courseId = read(send(post("/api/admin/courses"), admin, "{\"code\":\"" + code
                + "\",\"title\":\"" + code + " title\",\"creditHours\":3,\"departmentId\":" + dep
                + ",\"teacherId\":" + teacherId + "}").andExpect(status().isCreated())).get("id").asLong();
        if (studentId != null) {
            send(post("/api/admin/enrollments"), admin,
                    "{\"studentId\":" + studentId + ",\"courseId\":" + courseId + "}")
                    .andExpect(status().is2xxSuccessful());
        }
        return courseId;
    }

    // ---------- integration test 4 ----------

    @Test
    void teacherCreatesAndGradesAssignmentAndStudentSeesTheScore() throws Exception {
        String admin = login("admin", "admin123");
        long dep = departmentId();
        registerTeacher("wf_teacher", "WF-T1", dep);
        registerTeacher("wf_other", "WF-T2", dep);
        registerStudent("wf_student", "WF-S1", dep);
        long teacherId = idOf(admin, "/api/admin/teachers", "wf_teacher");
        long studentId = idOf(admin, "/api/admin/students", "wf_student");
        long courseId = createCourseAndEnroll(admin, "WF-101", dep, teacherId, studentId);

        String teacher = login("wf_teacher", "secret1");
        String other = login("wf_other", "secret1");
        String student = login("wf_student", "secret1");
        String base = "/api/teacher/courses/" + courseId + "/assignments";

        // a teacher who does not teach the course, and a student, are both refused
        send(post(base), other, "{\"title\":\"Nope\",\"maxMarks\":10}").andExpect(status().isForbidden());
        send(post(base), student, "{\"title\":\"Nope\",\"maxMarks\":10}").andExpect(status().isForbidden());
        // validation: negative max marks, blank title
        send(post(base), teacher, "{\"title\":\"Bad\",\"maxMarks\":-1}").andExpect(status().isBadRequest());
        send(post(base), teacher, "{\"title\":\"\",\"maxMarks\":10}").andExpect(status().isBadRequest());

        long assignmentId = read(send(post(base), teacher,
                "{\"title\":\"Lab 1\",\"description\":\"ER diagram\",\"maxMarks\":20,\"dueDate\":\"2030-01-31\"}")
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.title").value("Lab 1"))).get("id").asLong();

        // student sees it, not yet graded
        JsonNode mine = read(get("/api/student/assignments", student).andExpect(status().isOk()));
        JsonNode item = null;
        for (JsonNode n : mine) if (n.get("assignmentId").asLong() == assignmentId) item = n;
        assertThat(item).isNotNull();
        assertThat(item.get("obtainedMarks").isNull()).isTrue();

        // marks above the maximum are rejected, valid marks are saved
        String scoreUrl = base + "/" + assignmentId + "/scores";
        send(put(scoreUrl), teacher, "{\"studentId\":" + studentId + ",\"obtainedMarks\":25}")
                .andExpect(status().isBadRequest());
        send(put(scoreUrl), other, "{\"studentId\":" + studentId + ",\"obtainedMarks\":15}")
                .andExpect(status().isForbidden());
        send(put(scoreUrl), teacher,
                "{\"studentId\":" + studentId + ",\"obtainedMarks\":15,\"feedback\":\"Good\"}")
                .andExpect(status().isOk());

        mine = read(get("/api/student/assignments", student).andExpect(status().isOk()));
        for (JsonNode n : mine) if (n.get("assignmentId").asLong() == assignmentId) item = n;
        assertThat(item.get("obtainedMarks").decimalValue().intValue()).isEqualTo(15);
        assertThat(item.get("feedback").asText()).isEqualTo("Good");
    }

    // ---------- integration test 5 ----------

    @Test
    void disabledAccountsAndForgedTokensAreRejected() throws Exception {
        String admin = login("admin", "admin123");
        long dep = departmentId();
        registerTeacher("wf_disable", "WF-T3", dep);
        long id = idOf(admin, "/api/admin/teachers", "wf_disable");
        String token = login("wf_disable", "secret1");
        get("/api/teacher/courses", token).andExpect(status().isOk());

        // admin switches the account off: the existing token stops working at once
        send(put("/api/admin/teachers/" + id), admin, "{\"username\":\"wf_disable\",\"email\":\"wf_disable@university.local\","
                + "\"fullName\":\"wf_disable Person\",\"employeeId\":\"WF-T3\",\"departmentId\":" + dep
                + ",\"enabled\":false}").andExpect(status().isOk());
        get("/api/teacher/courses", token).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wf_disable\",\"password\":\"secret1\"}"))
                .andExpect(status().is4xxClientError());

        // switching it back on restores access
        send(put("/api/admin/teachers/" + id), admin, "{\"username\":\"wf_disable\",\"email\":\"wf_disable@university.local\","
                + "\"fullName\":\"wf_disable Person\",\"employeeId\":\"WF-T3\",\"departmentId\":" + dep
                + ",\"enabled\":true}").andExpect(status().isOk());
        get("/api/teacher/courses", token).andExpect(status().isOk());

        // garbage and tampered tokens are 401, and public endpoints stay open
        get("/api/teacher/courses", "abc.def.ghi").andExpect(status().isUnauthorized());
        get("/api/teacher/courses", token.substring(0, token.length() - 5) + "XXXXX").andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.get("/api/public/signup-info")).andExpect(status().isOk());
    }

    // ---------- integration test 6 ----------

    @Test
    void roleChangeTakesEffectOnAnExistingTokenWithoutRelogin() throws Exception {
        String admin = login("admin", "admin123");
        long dep = departmentId();
        registerStudent("wf_promoted", "WF-S2", dep);
        long id = idOf(admin, "/api/admin/students", "wf_promoted");
        String token = login("wf_promoted", "secret1");

        get("/api/student/enrollments", token).andExpect(status().isOk());
        get("/api/teacher/courses", token).andExpect(status().isForbidden());

        // no enrollments yet, so no confirmation is needed
        send(post("/api/admin/students/" + id + "/convert-to-teacher"), admin, "{\"employeeId\":\"WF-T4\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("WF-T4"));

        // the SAME token now has teacher rights and has lost the student ones
        get("/api/teacher/courses", token).andExpect(status().isOk());
        get("/api/student/enrollments", token).andExpect(status().isForbidden());
        get("/api/admin/students", token).andExpect(status().isForbidden());
    }
}
