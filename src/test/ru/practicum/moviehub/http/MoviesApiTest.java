package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final int SOCKET = 8080;
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json; charset=UTF-8";
    private static final String BASE = "http://localhost:8080";

    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, SOCKET);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        gson = new Gson();
    }

    @BeforeEach
    void beforeEach() {
        store.clearMovies();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies должен вернуть 200");

        assertContentType(resp);

        String body = resp.body().trim();

        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenIsNotEmpty_returnsArray() throws Exception {
        createSomeMovies();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies должен вернуть 200");

        assertContentType(resp);

        String actual = resp.body();
        String expected = gson.toJson(store.getMovies());

        assertEquals(expected, actual);
    }

    @Test
    void getMovieById_ifMovieExists_returnsMovie()
            throws IOException, InterruptedException {

        Movie expected = new Movie("title", 2000);
        store.addMovie(expected);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1/4toto"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies/{id} должен вернуть 200");

        assertContentType(resp);

        Movie actual = gson.fromJson(resp.body(), Movie.class);

        assertEquals(expected, actual);
    }

    @Test
    void getMovieById_ifMovieDoesNotExist_returns404()
            throws IOException, InterruptedException {

        store.addMovie(new Movie("title", 2000));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertErrorResponse(resp, 404, "Not Found", List.of());
    }

    @Test
    void getMovieById_ifIdIsNotNumber_returns400()
            throws IOException, InterruptedException {

        store.addMovie(new Movie("title", 2000));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/s"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertErrorResponse(resp, 400, "Bad Request", List.of());
    }

    @Test
    void postMovies_ifSomethingIsEmpty_returnsErrors() throws Exception {
        Movie movie = new Movie("", 2028);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertErrorResponse(
                resp,
                422,
                "Validation Error",
                List.of(
                        "название не должно быть пустым",
                        "год должен быть между 1888 и 2027"
                )
        );
    }

    @Test
    void postMovies_ifContentTypeIsIncorrect_returns415()
            throws IOException, InterruptedException {
        Movie movie = new Movie("ABC", 2025);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header(CONTENT_TYPE, "text/plain")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertErrorResponse(
                resp,
                415,
                "Unsupported Media Type",
                List.of()
        );
    }

    @Test
    void postMovies_ifTitleOutOfRange_returnsError()
            throws IOException, InterruptedException {

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

        assertErrorResponse(
                resp,
                422,
                "Validation Error",
                List.of("название не должно быть длиннее 100 символов")
        );
    }

    @Test
    void postMovies_ifAllOk_returns201() throws Exception {
        Movie movie = new Movie("ABC", 2025);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(),
                "POST /movies должен вернуть 201");

        assertContentType(resp);

        Movie actual = gson.fromJson(resp.body(), Movie.class);

        Movie expected = new Movie("ABC", 2025);
        expected.setId(1);

        assertEquals(expected, actual);
    }

    @Test
    void deleteMovie_ifMovieExists_returns204()
            throws IOException, InterruptedException {

        store.addMovie(new Movie("title", 2000));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(),
                "DELETE /movies/{id} должен вернуть 204");

        assertTrue(resp.body().isEmpty());
    }

    @Test
    void deleteMovie_ifMovieDoesNotExist_returns404()
            throws IOException, InterruptedException {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertErrorResponse(resp, 404, "Not Found", List.of());
    }

    @Test
    void getMoviesByYear_ifYearIsCorrect_returnsMovies()
            throws IOException, InterruptedException {

        createSomeMovies();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000%20&id=30"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies?year=YYYY должен вернуть 200");

        assertContentType(resp);

        List<Movie> actual =
                gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        List<Movie> expected = store.getMoviesByYear(2000);

        assertEquals(expected, actual);
    }

    private static void createSomeMovies() {
        store.addMovie(new Movie("а", 2000));
        store.addMovie(new Movie("b", 2000));
        store.addMovie(new Movie("c", 2000));
        store.addMovie(new Movie("d", 2001));
    }

    @Test
    void getMoviesByYear_ifNoMoviesFound_returnsEmptyList()
            throws IOException, InterruptedException {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies?year=YYYY должен вернуть 200");

        assertContentType(resp);

        List<Movie> actual =
                gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertTrue(actual.isEmpty());
    }

    @Test
    void getMoviesByYear_ifYearIsNotNumber_returns400()
            throws IOException, InterruptedException {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=paasd"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertErrorResponse(
                resp,
                400,
                "Bad Request",
                List.of("Некорректный параметр запроса — 'year'")
        );
    }

    @Test
    void getMoviesByYear_ifYearIsOutOfRange_returns400()
            throws IOException, InterruptedException {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=20000%20"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertErrorResponse(
                resp,
                400,
                "Bad Request",
                List.of("Некорректный параметр запроса — 'year'")
        );
    }

    @Test
    void getMoviesByYear_ifQueryParameterIsIncorrect_returns400()
            throws IOException, InterruptedException {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=sing&id=30"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertErrorResponse(
                resp,
                400,
                "Bad Request",
                List.of("Некорректный параметр запроса — 'year'")
        );
    }

    @Test
    void movies_ifMethodIsNotSupported_returns405()
            throws IOException, InterruptedException {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertErrorResponse(
                resp,
                405,
                "Method Not Allowed",
                List.of()
        );
    }

    private void assertContentType(HttpResponse<String> response) {
        String contentType = response.headers()
                .firstValue(CONTENT_TYPE)
                .orElse("");

        assertEquals(
                APPLICATION_JSON,
                contentType,
                "Content-Type должен содержать формат данных и кодировку"
        );
    }

    private void assertErrorResponse(
            HttpResponse<String> response,
            int expectedStatus,
            String expectedError,
            List<String> expectedDetails
    ) {
        assertEquals(expectedStatus, response.statusCode());

        assertContentType(response);

        JsonObject expected = new JsonObject();
        expected.addProperty("error", expectedError);

        JsonArray details = new JsonArray();
        expectedDetails.forEach(details::add);

        expected.add("details", details);

        JsonElement actual = JsonParser.parseString(response.body());

        assertEquals(expected, actual);
    }
}