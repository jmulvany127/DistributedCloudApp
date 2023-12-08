package be.kuleuven.distributedsystems.cloud.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
                publicKeys.put(key, certificate.replace("\\n", "").replace("\\s", ""));
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
    static RSAPublicKey convertStringToRSAPublicKey(String publicKeyString) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, URISyntaxException, CertificateException {
        System.out.println("pub key string: " + publicKeyString);
        try {
            //String testString = "MIIDHDCCAgSgAwIBAgIIES38GekKYOswDQYJKoZIhvcNAQEFBQAwMTEvMC0GA1UEAwwmc2VjdXJldG9rZW4uc3lzdGVtLmdzZXJ2aWNlYWNjb3VudC5jb20wHhcNMjMxMTMwMDczMjAyWhcNMjMxMjE2MTk0NzAyWjAxMS8wLQYDVQQDDCZzZWN1cmV0b2tlbi5zeXN0ZW0uZ3NlcnZpY2VhY2NvdW50LmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKFo+EwpnudDkGhzymq+g4Gkqwh3heP8KPQ13r4eAMJDSGQEkZ1EJFKTYdZY8Edm+oIZyUOhALs79HhXOnsof7LfLSBHocL5wPQdSgDAfqxvw2nZ87KteP4sTYdChVkbAX4Lbrqx3aQNZFuMfGYxhYtXfzB896Db9FgmXQYNxmVMYm4GsWrZ+7LxqhugvZ18RN5eCuPj4n155RlIyg3QCtTuDd1IElXF5daIa8FgZ8lv6CNA76CR72yLF+OrSi3UVmr1IwdTysj1NXWAI/GAnOnRMatA6uCPEcl9xfiJTT6wIz42YWX5J8vzHlIypVi12uMNQ1gmyCygm/fpKks0R1kCAwEAAaM4MDYwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCB4AwFgYDVR0lAQH/BAwwCgYIKwYBBQUHAwIwDQYJKoZIhvcNAQEFBQADggEBAEbQiyUmE6i75T4xAjbWgIsm1cwCQPZZuMgzSGogNrAUfXh7niSjBts31jaoDTnuguCYQorr6qojIZxG/ea61QvA28I+LsekFB3bQwnoeKAVEO5ehIWfFX+s4FICWIy7uI1FRl8f0/myPnzidG8gOFoukcrxFPnkYGOzzqqMzv2nuDrRxdqTfRiGpVZWYI0Ph9WfZCpeSdPqCypdBVPy/aZ5tHdWcIOTyEiaEFQcrfy9GEDJni3YnQQWLKsk+uuYxkwPEQjhbN7EkpfPSgRQ9+REVc0WIe6kBBokQeFqkfk01r+/DAnTVBQTJhgSFJI0uR/V/AvrFHfbj6kL6BliqU=";

            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(publicKeyBytes));

            PublicKey publicKey = certificate.getPublicKey();

            if (publicKey instanceof RSAPublicKey) {
                return (RSAPublicKey) publicKey;
            } else {
                throw new RuntimeException("Key no RSAPublic key");
            }
        } catch(Exception e) {
            throw new RuntimeException("Error converting key");
        }
    }

}
