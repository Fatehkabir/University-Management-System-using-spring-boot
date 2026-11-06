package edu.kuet.ums.unit;

import edu.kuet.ums.dto.Requests.RegisterRequest;
import edu.kuet.ums.dto.Requests.SignupRole;
import edu.kuet.ums.dto.Requests.StudentRequest;
import edu.kuet.ums.dto.Requests.TeacherRequest;
import edu.kuet.ums.dto.Responses.RegisterResponse;
import edu.kuet.ums.exception.ApiExceptions.FieldValidationException;
import edu.kuet.ums.exception.ApiExceptions.ForbiddenException;
import edu.kuet.ums.service.DepartmentService;
import edu.kuet.ums.service.SignupService;
import edu.kuet.ums.service.StudentService;
import edu.kuet.ums.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock TeacherService teachers;
    @Mock StudentService students;
    @Mock DepartmentService departments;

    private RegisterRequest student(String studentId) {
        return new RegisterRequest(SignupRole.STUDENT, " bob ", "secret1", " bob@x.test ", " Bob ", null, null,
                1L, null, null, null, null, studentId, "2024", 2);
    }

    private RegisterRequest teacher(String employeeId) {
        return new RegisterRequest(SignupRole.TEACHER, "tina", "secret1", "tina@x.test", "Tina", null, null,
                1L, employeeId, "Lecturer", null, null, null, null, null);
    }

    @Test
    void studentSignupTrimsInputCreatesActiveAccountAndNeedsStudentId() {
        SignupService service = new SignupService(teachers, students, departments, true, false);

        RegisterResponse res = service.register(student(" 2207001 "));

        ArgumentCaptor<StudentRequest> captor = ArgumentCaptor.forClass(StudentRequest.class);
        verify(students).create(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("bob");
        assertThat(captor.getValue().email()).isEqualTo("bob@x.test");
        assertThat(captor.getValue().studentId()).isEqualTo("2207001");
        assertThat(captor.getValue().enabled()).isTrue();
        assertThat(res.approvalRequired()).isFalse();

        assertThatThrownBy(() -> service.register(student("  ")))
                .isInstanceOfSatisfying(FieldValidationException.class,
                        e -> assertThat(e.getFieldErrors()).containsKey("studentId"));
        assertThatThrownBy(() -> service.register(teacher(null)))
                .isInstanceOfSatisfying(FieldValidationException.class,
                        e -> assertThat(e.getFieldErrors()).containsKey("employeeId"));
    }

    @Test
    void disabledSignupIsRefusedAndApprovalModeCreatesDisabledAccounts() {
        SignupService off = new SignupService(teachers, students, departments, false, false);
        assertThatThrownBy(() -> off.register(student("1"))).isInstanceOf(ForbiddenException.class);
        verify(students, never()).create(any());
        assertThat(off.info().enabled()).isFalse();
        assertThat(off.info().departments()).isEmpty();

        SignupService approval = new SignupService(teachers, students, departments, true, true);
        RegisterResponse res = approval.register(teacher("E-1"));
        ArgumentCaptor<TeacherRequest> captor = ArgumentCaptor.forClass(TeacherRequest.class);
        verify(teachers).create(captor.capture());
        assertThat(captor.getValue().enabled()).isFalse();
        assertThat(res.approvalRequired()).isTrue();
    }
}
