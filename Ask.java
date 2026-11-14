import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Ask {

    static Map<String, String> loadDotenv(Path path) {
        Map<String, String> env = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int eq = line.indexOf('=');
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                env.putIfAbsent(key, value);
            }
        } catch (IOException e) {
            // No .env file — the caller prints the friendly message.
        }
        return env;
    }

    static String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    static String jsonUnescape(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'n') {
                    out.append('\n');
                } else if (n == 'r') {
                    out.append('\r');
                } else if (n == 't') {
                    out.append('\t');
                } else if (n == '"') {
                    out.append('"');
                } else if (n == '\\') {
                    out.append('\\');
                } else if (n == '/') {
                    out.append('/');
                } else if (n == 'u' && i + 5 < s.length()) {
                    out.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                    i += 4;
                } else {
                    out.append(n);
                }
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static String extractReply(String body) {
        int marker = body.indexOf("\"text\"");
        if (marker < 0) {
            return null;
        }
        int colon = body.indexOf(':', marker);
        int startQuote = body.indexOf('"', colon);
        if (startQuote < 0) {
            return null;
        }
        int i = startQuote + 1;
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return jsonUnescape(body.substring(startQuote + 1, i));
            } else {
                i++;
            }
        }
        return null;
    }

    static String ask(String prompt, String model) {
        Map<String, String> dotenv = loadDotenv(Path.of(".env"));
        String key = dotenv.getOrDefault("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY"));
        if (key == null || key.isBlank()) {
            System.out.println("No GEMINI_API_KEY found. Create a .env file with GEMINI_API_KEY=your-key and try again.");
            return null;
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
        String payload = "{\"contents\": [{\"parts\": [{\"text\": \"" + jsonEscape(prompt) + "\"}]}]}";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8)))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            System.out.println("Could not reach Gemini: " + e.getMessage());
            System.out.println("Check your internet connection and try again.");
            return null;
        }
        if (response.statusCode() != 200) {
            System.out.println("Gemini API error: " + response.statusCode());
            String body = response.body();
            System.out.println(body.substring(0, Math.min(500, body.length())));
            return null;
        }
        String reply = extractReply(response.body());
        if (reply == null) {
            System.out.println("Unexpected reply shape:");
            String body = response.body();
            System.out.println(body.substring(0, Math.min(1000, body.length())));
            return null;
        }
        return reply;
    }

    public static void main(String[] args) {
        String prompt = String.join(" ", args);
        if (prompt.isBlank()) {
            prompt = "Explain what an API is in one sentence, like I am a Java developer.";
        }
        System.out.println("Prompt: " + prompt);
        String reply = ask(prompt, "gemini-3.5-flash-lite");
        if (reply != null) {
            System.out.println("Reply:");
            System.out.println(reply);
        }
    }
}