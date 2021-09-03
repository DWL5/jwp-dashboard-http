package nextstep.jwp.controller;

import nextstep.jwp.MockSocket;
import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.http.request.HttpRequest;
import nextstep.jwp.http.response.HttpResponse;
import nextstep.jwp.http.session.HttpSession;
import nextstep.jwp.http.session.HttpSessions;
import nextstep.jwp.model.User;
import nextstep.jwp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class LoginControllerTest {
    LoginController loginController;

    @BeforeEach
    private void setUp() {
        loginController = new LoginController(new UserService());
        InMemoryUserRepository.save(new User("test", "test", "test@test.com"));
    }

    @DisplayName("/login GET매핑 200 ok을 반환한다.")
    @Test
    void doGet() throws IOException {
        MockSocket mockSocket = new MockSocket("GET /login HTTP/1.1 \r\n");
        loginController.doGet(HttpRequest.of(mockSocket.getInputStream()), new HttpResponse(mockSocket.getOutputStream()));

        assertThat(mockSocket.output().split("\r\n")[0]).isEqualTo("HTTP/1.1 200 OK ");
    }

    @DisplayName("/login GET매핑 로그인이 되어있다면, index.html로 Redirect한다.")
    @Test
    void doGetWhenLogin() throws IOException {
        String uuid = UUID.randomUUID().toString();
        HttpSession session = HttpSessions.getSession(uuid);
        session.setAttribute("user", InMemoryUserRepository.findByAccount("gugu").get());

        MockSocket mockSocket = new MockSocket("GET /login HTTP/1.1 \r\n" +
                "Cookie: JSESSIONID=" + uuid + "\r\n");

        loginController.doGet(HttpRequest.of(mockSocket.getInputStream()), new HttpResponse(mockSocket.getOutputStream()));

        assertThat302Response(mockSocket, "HTTP/1.1 302 Redirect ", "Location: /index.html ");
    }

    @DisplayName("로그인 성공 시 index.html로 Redirect 한다.")
    @Test
    void doPost() throws IOException {
        String uuid = UUID.randomUUID().toString();
        MockSocket mockSocket = new MockSocket(
                generatePostRequestHeaderWithSession(uuid) +
                "account=test&password=test");

        loginController.doPost(HttpRequest.of(mockSocket.getInputStream()), new HttpResponse(mockSocket.getOutputStream()));
        assertThat302Response(mockSocket, "HTTP/1.1 302 Redirect ", "Location: /index.html ");
    }

    @DisplayName("로그인 실패 시 401.html로 Redirect 한다.")
    @Test
    void doPostWhenLoginFailed() throws IOException {
        String uuid = UUID.randomUUID().toString();
        MockSocket mockSocket = new MockSocket(
                generatePostRequestHeaderWithSession(uuid) +
                "account=test&password=tost");

        loginController.doPost(HttpRequest.of(mockSocket.getInputStream()), new HttpResponse(mockSocket.getOutputStream()));

        assertThat302Response(mockSocket, "HTTP/1.1 302 Redirect ", "Location: /401.html ");
    }


    private void assertThat302Response(MockSocket mockSocket, String responseLine, String header) {
        assertThat(mockSocket.output().split("\r\n")[0]).isEqualTo(responseLine);
        assertThat(mockSocket.output().split("\r\n")[1]).isEqualTo(header);
    }


    private String generatePostRequestHeaderWithSession(String uuid) {
        return "POST /login HTTP/1.1 \r\n" +
                "Host: localhost:8080 \r\n" +
                "Content-Length: 26 \r\n" +
                "Connection-Type: application/x-www-form-urlencoded \r\n" +
                "Cookie: JSESSIONID=" + uuid + "\r\n" +
                "Accept: */* \r\n" +
                "\r\n";
    }
}