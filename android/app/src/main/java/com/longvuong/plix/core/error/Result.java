package com.longvuong.plix.core.error;

public abstract class Result<T> {

    private Result() {
    }

    public static final class Success<T> extends Result<T> {
        public final T data;

        public Success(T data) {
            this.data = data;
        }
    }

    public static final class Error<T> extends Result<T> {
        public final ErrorType type;
        public final String message;
        public final Throwable cause;

        public Error(ErrorType type, String message, Throwable cause) {
            this.type = type;
            this.message = message;
            this.cause = cause;
        }
    }
}