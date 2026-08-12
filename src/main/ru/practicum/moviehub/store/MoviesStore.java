package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class MoviesStore {
    private final List<Movie> store;
    private int id = 0;

    public MoviesStore() {
        store = new ArrayList<>();
    }

    public void addMovie(Movie movie) {
        movie.setId(++id);
        store.add(movie);
    }

    public List<Movie> getMovies() {
        return store;
    }

    public Optional<Movie> getMovieOpt(int id) {
        return store.stream()
                .filter(movie -> movie.getId() == id)
                .findFirst();
    }

    public boolean deleteMovie(int id) {
        return store.removeIf(movie -> movie.getId() == id);
    }

    public List<Movie> getMoviesByYear(int year) {
        return store.stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public void clearMovies() {
        store.clear();
        id = 0;
    }
}