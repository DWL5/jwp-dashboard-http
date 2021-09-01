package nextstep.jwp.http.request;

import nextstep.jwp.http.HttpMethod;
import nextstep.jwp.http.exception.HttpFormatException;

import java.util.HashMap;
import java.util.Map;

import static nextstep.jwp.http.HttpUtil.parseQuery;

public class RequestLine {

    private HttpMethod httpMethod;
    private String path;
    private Map<String, String> params;

    private RequestLine(HttpMethod method, String path, Map<String, String> params) {
        this.httpMethod = method;
        this.path = path;
        this.params = params;
    }

    public static RequestLine of(String requestLine) {
        String[] tokens = requestLine.split(" ");

        if (tokens.length != 3) {
            throw new HttpFormatException();
        }

        String method = tokens[0];
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        String path = tokens[1];
        if ("POST".equals(method)) {
            return new RequestLine(httpMethod, path, new HashMap<>());
        }

        int index = tokens[1].indexOf("?");
        Map<String, String> params = new HashMap<>();

        if (index != -1) {
            path = tokens[1].substring(0, index);
            params = parseQuery(tokens[1].substring(index + 1));
        }

        return new RequestLine(httpMethod, path, params);
    }

    public boolean checkMethod(String method) {
        return httpMethod.checkHttpMethod(HttpMethod.valueOf(method));
    }

    public String getPath() {
        return path;
    }

    public String getQueryParam(String key) {
        return params.get(key);
    }
}
