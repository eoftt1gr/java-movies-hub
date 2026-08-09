package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.api.MovieValidator;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final MovieValidator validator = new MovieValidator();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        switch (ex.getRequestMethod().toUpperCase()) {
            case "GET":
                handleGet(ex);
                break;

            case "POST":
                handlePost(ex);
                break;

            case "DELETE":
                handleDelete(ex);

            default:
                sendJson(ex, 405, new ErrorResponse("Method Not Allowed", List.of()));
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        if (path.equals("/movies")) {
            if (query == null) {
                handleGetMovies(ex);
            } else {
                handleGetMoviesByYear(ex, query);
            }
            return;
        }
        handleGetMovieById(ex);
    }

    private void handleGetMovies(HttpExchange ex) throws IOException {
        sendJson(ex, 200, store.getMovies());
    }

    private void handleGetMovieById(HttpExchange ex) throws IOException {
        Optional<Integer> id = getMovieId(ex);
        if (id.isEmpty()) {
            sendJson(ex, 400,
                    new ErrorResponse("Bad Request", List.of()));
            return;
        }

        Optional<Movie> movie = store.getMovieOpt(id.get());
        if (movie.isEmpty()) {
            sendJson(ex, 404,
                    new ErrorResponse("Not Found", List.of()));
            return;
        }

        sendJson(ex, 200, movie.get());
    }

    private void handleGetMoviesByYear(HttpExchange ex, String query) throws IOException {
        String[] split = query.split("=");

        if (split.length != 2 || !split[0].equals("year")) {
            sendInvalidYear(ex);
            return;
        }

        try {
            int year = Integer.parseInt(split[1]);

            if (!validator.validateYear(year)) {
                sendInvalidYear(ex);
                return;
            }

            List<Movie> movies = store.getMoviesByYear(year);
            sendJson(ex, 200, movies);
        } catch (NumberFormatException e) {
            sendInvalidYear(ex);
        }
    }


    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders()
                .getFirst("Content-Type");
        if (!CT_JSON.equals(contentType)) {
            sendJson(ex, 415, new ErrorResponse("Unsupported Media Type", List.of()));
            return;
        }

        Movie movie = readJson(ex, Movie.class);

        List<String> errors = validator.validate(movie);

        if (!errors.isEmpty()) {
            sendJson(ex, 422, new ErrorResponse("Validation Error", errors));
            return;
        }

        store.addMovie(movie);
        sendJson(ex, 201, movie);
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        Optional<Integer> id = getMovieId(ex);

        if (id.isEmpty()) {
            sendJson(ex, 400,
                    new ErrorResponse("Bad Request", List.of()));
            return;
        }

        if (store.deleteMovie(id.get())) {
            sendNoContent(ex);
        } else {
            sendJson(ex, 404, new ErrorResponse("Not Found", List.of()));
        }
    }

    private Optional<Integer> getMovieId(HttpExchange exchange) {
        String[] pathParts = exchange.getRequestURI()
                .getPath()
                .split("/");

        if (pathParts.length < 3) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(pathParts[2]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void sendInvalidYear(HttpExchange ex) throws IOException {
        sendJson(ex, 400,
                new ErrorResponse(
                        "Bad Request",
                        List.of("Некорректный параметр запроса — 'year'")
                ));
    }
}