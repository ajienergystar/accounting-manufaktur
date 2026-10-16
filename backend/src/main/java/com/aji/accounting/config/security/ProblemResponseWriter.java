package com.aji.accounting.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;

/** Menulis respons RFC 7807 untuk error dari filter chain security (401/403). */
@Component
@RequiredArgsConstructor
public class ProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String title, String detail)
            throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", OffsetDateTime.now());
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getOutputStream(), pd);
    }
}
