package org.dromara.game.adapter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class HmacSigner {
    private HmacSigner() {}

    public static String sign(String secret, String timestamp, String nonce, String method, String path, String body) {
        try {
            String canonical = timestamp + "\n" + nonce + "\n" + method + "\n" + path + "\n" + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 签名失败", e);
        }
    }
}
