package io.github.chehsunliu.itx.backend.error;

public class BackendException extends RuntimeException {
    private final int status;

    public BackendException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static BackendException notFound() {
        return new BackendException(404, "not found");
    }

    public static BackendException badRequest(String message) {
        return new BackendException(400, message);
    }

    public static BackendException unknown(String message) {
        return new BackendException(500, message);
    }
}
