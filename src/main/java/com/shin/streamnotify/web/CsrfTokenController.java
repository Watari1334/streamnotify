package com.shin.streamnotify.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SPA向けにCSRFトークンを配布するコントローラ。
 * dashboard.htmlが読み込まれた際に真っ先にこのエンドポイントを叩き、
 * 取得したトークンを以降のPOST/DELETEリクエストのヘッダーに付与する運用になっている。
 */
@RestController
public class CsrfTokenController {

    /**
     * 現在のリクエストに対応するCSRFトークンを返す。
     * CsrfTokenを引数として受け取ること自体が、Spring Securityの遅延評価をその場でトリガーし、
     * Cookieへの書き込みを確実に発生させる(Spring Security公式が推奨する、SPA向けの正規パターン)。
     *
     * @param csrfToken Spring Securityが自動注入する、現在のCSRFトークン
     * @return ヘッダー名とトークン値を含むレスポンス
     */
    @GetMapping("/api/csrf-token")
    public CsrfTokenResponse csrfToken(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    /**
     * CSRFトークン配布用のレスポンスDTO。
     *
     * @param headerName リクエスト時にトークンを乗せるべきヘッダー名(例: X-XSRF-TOKEN)
     * @param token トークンの値
     */
    record CsrfTokenResponse(String headerName, String token) {}
}