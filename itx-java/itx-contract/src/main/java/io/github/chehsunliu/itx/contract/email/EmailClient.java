package io.github.chehsunliu.itx.contract.email;

public interface EmailClient {
    /** Sends a single email. Returns normally on a 2xx response from the upstream provider. */
    void send(SendEmailMessage msg);
}
