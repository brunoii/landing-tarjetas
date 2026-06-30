package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class StaticUiContractTests {

    private static final Path STATIC_ROOT = Path.of("src/main/resources/static");

    @Test
    void indexLinksExpectedStaticAssets() throws IOException {
        String index = readStatic("index.html");

        assertThat(index).contains("<link rel=\"stylesheet\" href=\"/css/styles.css\">");
        assertThat(index).contains("<script type=\"module\" src=\"/js/app.js\"></script>");
        assertThat(readStatic("js/app.js"))
                .contains("./api.js", "./categories.js", "./dashboard.js", "./transactions.js", "./utils.js");
    }

    @Test
    void apiModuleReferencesOnlyEtapaThreeReadAndCategoryEndpoints() throws IOException {
        String api = readStatic("js/api.js");

        assertThat(api).contains(
                "/api/dashboard/summary",
                "/api/statements",
                "/api/transactions",
                "/api/categories"
        );
        assertThat(api).doesNotContain("/api/uploads", "/api/upload", "/api/parse", "/api/parsing", "/api/projections");
    }

    @Test
    void staticUiDoesNotReferenceUploadParsingOrProjectionApiEndpoints() throws IOException {
        String staticFiles = readAllStaticText();

        assertThat(staticFiles).doesNotContainPattern("/api/[^\\\"']*(upload|uploads|parse|parsing|projection|projections)");
    }

    @Test
    void filterOptionValuesMatchBackendEnums() throws IOException {
        String index = readStatic("index.html");

        assertThat(optionValues(index, "filter-card")).isEqualTo(enumValues("CardBrand"));
        assertThat(optionValues(index, "filter-type")).isEqualTo(enumValues("TransactionType"));
    }

    @Test
    void categoryColorContractUsesSafeHexOnlyPolicy() throws IOException {
        String index = readStatic("index.html");
        String categories = readStatic("js/categories.js");
        String utils = readStatic("js/utils.js");

        assertThat(index).contains("pattern=\"#[0-9A-Fa-f]{6}\"");
        assertThat(categories).contains("pattern=\"#[0-9A-Fa-f]{6}\"", "safeHexColor(category.color)");
        assertThat(categories).doesNotContain("style.background = category.color");
        assertThat(utils).contains("/^#[0-9A-Fa-f]{6}$/");
    }

    @Test
    void currencyCopyKeepsPesosAndUsdSeparateWithoutConversion() throws IOException {
        String index = readStatic("index.html");

        assertThat(index).contains("USD is shown separately. No conversion is applied.");
        assertThat(index).contains("Total pesos", "Total USD");
    }

    private static String readStatic(String relativePath) throws IOException {
        return Files.readString(STATIC_ROOT.resolve(relativePath));
    }

    private static String readAllStaticText() throws IOException {
        try (var files = Files.walk(STATIC_ROOT)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html") || path.toString().endsWith(".css") || path.toString().endsWith(".js"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .collect(Collectors.joining("\n"));
        }
    }

    private static Set<String> optionValues(String html, String selectId) {
        var selectMatcher = Pattern.compile("<select id=\\\"" + selectId + "\\\">(.*?)</select>", Pattern.DOTALL).matcher(html);
        assertThat(selectMatcher.find()).isTrue();

        return Pattern.compile("<option value=\\\"([^\\\"]+)\\\">")
                .matcher(selectMatcher.group(1))
                .results()
                .map(result -> result.group(1))
                .collect(Collectors.toSet());
    }

    private static Set<String> enumValues(String enumName) throws IOException {
        Path enumPath = Path.of("src/main/java/com/gentleia/landingtarjetas/shared", enumName + ".java");
        String enumBody = Files.readString(enumPath)
                .replaceAll("(?s).*?\\{", "")
                .replaceAll("(?s)\\}.*", "");

        return Arrays.stream(enumBody.split(","))
                .map(value -> value.replaceAll("[;\\s]", ""))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }
}
