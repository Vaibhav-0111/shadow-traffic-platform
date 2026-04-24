package com.shadow.duplicator.controller;

import com.shadow.duplicator.service.TrafficDuplicatorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catch-all controller.  Every request that reaches the duplicator
 * is fanned out to both v1 and v2.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DuplicatorController {

    private final TrafficDuplicatorService duplicatorService;

    @RequestMapping("/api/**")
    public ResponseEntity<String> handle(HttpServletRequest request,
                                         @RequestBody(required = false) String body) throws IOException {

        // Collect headers as a flat map
        Map<String, String> headers = Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(
                        h -> h,
                        h -> request.getHeader(h),
                        (a, b) -> a));   // keep first on duplicates

        String path  = request.getRequestURI();
        String query = request.getQueryString();
        if (query != null) path = path + "?" + query;

        return duplicatorService.duplicate(request.getMethod(), path, body, headers);
    }
}
