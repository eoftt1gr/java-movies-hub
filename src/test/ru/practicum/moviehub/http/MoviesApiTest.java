package ru.practicum.moviehub.http;

import com.google.gson.Gson;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoviesApiTest {
    private static final int socket = 8080;
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json; charset=UTF-8";
    private static final String BASE = "http://localhost:8080";

    private static MoviesServer server;
    private static HttpClient client;
    private static Gson gson;


    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), socket);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        gson = new Gson();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clearMovies();
    }

    @AfterAll
    static void afterAll() {
        if (server != null)
            server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(APPLICATION_JSON,
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");


        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenIsNotEmpty_returnsArray() throws Exception {
        server.getStore().addMovie(new Movie("а", 2000));
        server.getStore().addMovie(new Movie("b", 2000));
        server.getStore().addMovie(new Movie("c", 2000));
        server.getStore().addMovie(new Movie("d", 2001));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(APPLICATION_JSON,
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");


        String body = cleanLine(resp.body());
        String expected = gson.toJson(server.getStore().getMovies());

        assertEquals(expected, body);
    }

    @Test
    void getMovieById_ifMovieExists_returnsMovie() throws IOException, InterruptedException {
        Movie expected = new Movie("title", 2000);
        server.getStore().addMovie(expected);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(APPLICATION_JSON,
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Movie movie = gson.fromJson(resp.body(), Movie.class);

        assertEquals(expected, movie);
    }

    @Test
    void getMovieById_ifMovieDoesNotExist_returns404() throws IOException, InterruptedException {
        Movie expected = new Movie("title", 2000);
        server.getStore().addMovie(expected);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies/{id} должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(APPLICATION_JSON,
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        String errors = """
                {
                    "error" : "NotFound",
                    "details": [
                    ]
                }
                """;

        assertEquals(cleanLine(errors), cleanLine(body));
    }

    @Test
    void getMovieById_ifIdIsNotNumber_returns400() throws IOException, InterruptedException {
        Movie expected = new Movie("title", 2000);
        server.getStore().addMovie(expected);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/s"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies/{id} должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(APPLICATION_JSON,
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        String errors = """
                {
                    "error" : "Bad Request",
                    "details": [
                    ]
                }
                """;

        assertEquals(cleanLine(errors), cleanLine(body));
    }


    @Test
    void postMovies_ifSomethingIsEmpty_returnsErrors() throws Exception {
        String json = """
                {
                    "title": "",
                    "year": 2028
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .headers(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body();
        String errors = """
                {
                    "error" : "Validation Error",
                    "details": [
                       "название не должно быть пустым",
                       "год должен быть между 1888 и 2027"
                    ]
                }
                """;

        assertEquals(cleanLine(errors), cleanLine(body));
    }

    @Test
    void postMovies_ifContentTypeIsIncorrect_returns415() throws IOException, InterruptedException {
        String json = """
                {
                    "title": "ABC",
                    "year": 2025
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header(CONTENT_TYPE, "text/plain")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(),
                "POST /movies с неправильным Content-Type должен вернуть 415");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals(APPLICATION_JSON, contentTypeHeaderValue,
                "Content-Type ответа должен быть application/json; charset=UTF-8");

        String errors = """
                {
                    "error": "Unsupported Media Type",
                    "details": []
                }
                """;

        JsonElement expected = JsonParser.parseString(errors);
        JsonElement actual = JsonParser.parseString(resp.body());

        assertEquals(expected, actual);
    }

    @Test
    void postMovies_ifTitleOutOfRange_returnsError() throws IOException, InterruptedException {
        String title = "a".repeat(101);
        Movie movie = new Movie(title, 2000);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );

        String body = resp.body();

        String errors = """
                {
                    "error": "Validation Error",
                    "details": [
                        "название не должно быть длиннее 100 символов"
                    ]
                }
                """;

        assertEquals(cleanLine(errors), cleanLine(body));
    }

    @Test
    void postMovies_ifAllOk_returns201() throws Exception {
        String json = """
                {
                    "title": "ABC",
                    "year": 2025
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .headers(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        String expected = """
                {
                    "id": 1,
                    "title": "ABC",
                    "year": 2025
                }
                """;
        assertEquals(cleanLine(expected), cleanLine(body));
    }

    @Test
    void deleteMovie_ifMovieExist_returns204() throws IOException, InterruptedException {
        Movie movie = new Movie("title", 2000);
        server.getStore().addMovie(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .headers(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        assertTrue(resp.body().isEmpty());
    }

    @Test
    void deleteMovie_ifDoesNotMovieExist_returns404() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .headers(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "DELETE /movies/{id} должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String errors = """
                {
                    "error" : "Not Found",
                    "details": [
                    ]
                }
                """;
        assertEquals(cleanLine(errors), cleanLine(resp.body()));
    }

    @Test
    void getMoviesByYear_ifYearIsCorrect_returnsMovies() throws IOException, InterruptedException {
        server.getStore().addMovie(new Movie("а", 2000));
        server.getStore().addMovie(new Movie("b", 2000));
        server.getStore().addMovie(new Movie("c", 2000));
        server.getStore().addMovie(new Movie("d", 2001));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .GET()
                .headers(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        List<Movie> movieList =
                gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        List<Movie> expected = server.getStore().getMoviesByYear(2000);

        assertEquals(expected, movieList);
    }

    @Test
    void getMoviesByYear_ifNoMoviesFound_returnsEmptyList() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .GET()
                .headers(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 204");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        List<Movie> movieList =
                gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        List<Movie> expected = server.getStore().getMoviesByYear(2000);

        assertEquals(expected, movieList);
    }

    @Test
    void getMoviesByYear_ifYearIsNotNumber_returns400() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=paasd"))
                .GET()
                .headers(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String errors = """
                {
                    "error" : "Bad Request",
                    "details": [
                    "Некорректный параметр запроса — 'year'"
                    ]
                }
                """;
        JsonElement expected = JsonParser.parseString(errors);
        JsonElement actual = JsonParser.parseString(resp.body());

        assertEquals(expected, actual);
    }

    @Test
    void getMoviesByYear_ifYearIsOutOfRange_returns400() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=20000"))
                .GET()
                .headers(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String errors = """
                {
                    "error" : "Bad Request",
                    "details": [
                    "Некорректный параметр запроса — 'year'"
                    ]
                }
                """;
        JsonElement expected = JsonParser.parseString(errors);
        JsonElement actual = JsonParser.parseString(resp.body());

        assertEquals(expected, actual);
    }

    @Test
    void getMoviesByYear_ifQueryParameterIsIncorrect_returns400() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000&id=30"))
                .GET()
                .headers(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String errors = """
                {
                    "error" : "Bad Request",
                    "details": [
                    "Некорректный параметр запроса — 'year'"
                    ]
                }
                """;
        JsonElement expected = JsonParser.parseString(errors);
        JsonElement actual = JsonParser.parseString(resp.body());

        assertEquals(expected, actual);
    }

    @Test
    void movies_ifMethodIsNotSupported_returns405() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode(),
                "Неподдерживаемый HTTP-метод должен вернуть 405");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");

        assertEquals(APPLICATION_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String errors = """
                {
                    "error": "Method Not Allowed",
                    "details": []
                }
                """;

        JsonElement expected = JsonParser.parseString(errors);
        JsonElement actual = JsonParser.parseString(resp.body());

        assertEquals(expected, actual);
    }


    private String cleanLine(String str) {
        return str.replaceAll("\\s+", "");
    }
}