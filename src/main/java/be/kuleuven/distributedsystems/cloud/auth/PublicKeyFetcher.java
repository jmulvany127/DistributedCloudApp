package be.kuleuven.distributedsystems.cloud.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Matcher.quoteReplacement;

public class PublicKeyFetcher {

    private final String publicKeysUrl = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

    public Map<String, String> fetchPublicKeys() throws IOException {
        URL url = new URL(publicKeysUrl);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        int responseCode = conn.getResponseCode();

        if (responseCode != 200) {
            throw new RuntimeException("Http response code: " + responseCode);
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(conn.getInputStream());
            return parseJsonToPublicKeys(jsonNode);
        }
    }

    // function to return keys and certs when given json
    private static Map<String, String> parseJsonToPublicKeys(JsonNode jsonNode) {
        Map<String, String> publicKeys = new HashMap<>();

        // iterate over json nodes and extract the keys and X.509 certificates
        for (Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields(); fields.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            String certificate = returnMatch(entry.getValue());
            if (certificate != null) {
                publicKeys.put(key, certificate.replace("\\n", ""));
            }
        }
        return publicKeys;
    }

    // parsing function
    private static String returnMatch(JsonNode entry) {
        Pattern pattern = Pattern.compile("-----BEGIN CERTIFICATE-----\\s*(.*?)\\s*-----END CERTIFICATE-----", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(entry.toString());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    // function to convert from string into RSAPublicKey
    static RSAPublicKey convertStringToRSAPublicKey(String publicKeyString) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);

        // generate public key from the byte array
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        System.out.println(keySpec);
        //error seems to be here
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

}
