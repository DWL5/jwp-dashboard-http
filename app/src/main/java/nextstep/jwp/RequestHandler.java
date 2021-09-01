package nextstep.jwp;

import nextstep.jwp.controller.Controller;
import nextstep.jwp.http.HttpCookie;
import nextstep.jwp.http.request.HttpRequest;
import nextstep.jwp.http.response.HttpResponse;
import nextstep.jwp.controller.RequestMapping;
import nextstep.jwp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.UUID;

import static nextstep.jwp.http.HttpCookie.J_SESSION_ID;

public class RequestHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private final Socket connection;

    private final UserService userService;

    public RequestHandler(Socket connection, UserService userService) {
        this.connection = Objects.requireNonNull(connection);
        this.userService = userService;
    }

    @Override
    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());
        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = connection.getOutputStream()) {

            HttpRequest httpRequest = HttpRequest.of(inputStream);
            HttpResponse httpResponse = new HttpResponse(outputStream);
            Controller controller = RequestMapping.getController(httpRequest.getPath());

            if (controller == null) {
                String path = getDefaultPath(httpRequest.getPath());
                httpResponse.ok(path);
                return;
            }

            setCookie(httpRequest, httpResponse);
            controller.service(httpRequest, httpResponse);
        } catch (IOException exception) {
            log.error("Exception stream", exception);
        } finally {
            close();
        }
    }

    private void setCookie(HttpRequest httpRequest, HttpResponse httpResponse) {
        HttpCookie httpCookie = httpRequest.getCookies();
        if (httpCookie.getCookie(J_SESSION_ID) != null) {
            httpResponse.addHeader("Set-Cookie", J_SESSION_ID+"="+httpCookie.getCookie(J_SESSION_ID));
            return;
        }

        httpResponse.addHeader("Set-Cookie", J_SESSION_ID+"="+ UUID.randomUUID());
    }

    private String getDefaultPath(String path) {
        if ("/".equals(path)) {
            return "/index.html";
        }
        return path;
    }

    private void close() {
        try {
            connection.close();
        } catch (IOException exception) {
            log.error("Exception closing socket", exception);
        }
    }
}
