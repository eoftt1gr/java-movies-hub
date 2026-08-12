package ru.practicum.moviehub.api;

import ru.practicum.moviehub.model.Movie;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovieValidator {
    private static final int MAX_YEAR = LocalDate.now().getYear() + 1;
    private static final int MIN_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;

    public List<String> validate(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            errors.add("название не должно быть пустым");
        }

        if (movie.getTitle() != null && movie.getTitle().length() > MAX_TITLE_LENGTH) {
            errors.add("название не должно быть длиннее 100 символов");
        }

        if (!validateYear(movie.getYear())) {
            errors.add("год должен быть между 1888 и " + MAX_YEAR);
        }

        return errors;
    }

    public boolean validateYear(int year) {
        return year >= MIN_YEAR && year <= MAX_YEAR;
    }
}