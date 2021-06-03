package com.evergreen.treetop.architecture;

public interface Exceptions {

    public static class NoSuchDocumentException extends Exception {

        public NoSuchDocumentException() {}

        public NoSuchDocumentException(String message) {
            super(message);
        }

        public NoSuchDocumentException(Throwable cause) {
            super(cause);
        }

        public NoSuchDocumentException(String message, Throwable cause) {
            super(message, cause);
        }

    }
}
