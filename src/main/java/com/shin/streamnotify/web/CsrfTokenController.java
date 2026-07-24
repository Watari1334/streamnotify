package com.shin.streamnotify.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfTokenController {

    // CsrfTokenを引数として受け取ること自体が、
    // Spring Securityの"遅延評価"をその場でトリガーし、
    // Cookieへの書き込みを確実に発生させる。
    // これはSpring Security公式が推奨する、SPA向けの正規パターン。
    @GetMapping("/api/csrf-token")
    public CsrfTokenResponse csrfToken(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    record CsrfTokenResponse(String headerName, String token) {}
}