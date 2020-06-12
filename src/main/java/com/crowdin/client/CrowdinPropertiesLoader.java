package com.crowdin.client;

import com.crowdin.util.PropertyUtil;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.crowdin.Constants.*;

public class CrowdinPropertiesLoader {


    public static CrowdinProperties load(Project project) {
        Properties properties = PropertyUtil.getProperties(project);
        return CrowdinPropertiesLoader.load(properties);
    }

    public static CrowdinProperties load(Properties properties) {
        List<String> errors = new ArrayList<>();
        CrowdinProperties crowdinProperties = new CrowdinProperties();
        if (properties == null) {
            errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_config_file"), PROPERTIES_FILE));
        } else {
            String propProjectId = properties.getProperty(PROJECT_ID);
            if (StringUtils.isNotEmpty(propProjectId)) {
                try {
                    crowdinProperties.setProjectId(Long.valueOf(propProjectId));
                } catch (NumberFormatException e) {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.property_is_not_number"), PROJECT_ID));
                }
            } else {
                errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), PROJECT_ID));
            }
            String propApiToken = properties.getProperty(API_TOKEN);
            if (StringUtils.isNotEmpty(propApiToken)) {
                crowdinProperties.setApiToken(propApiToken);
            } else {
                errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), API_TOKEN));
            }
            String propBaseUrl = properties.getProperty(BASE_URL);
            if (StringUtils.isNotEmpty(propBaseUrl)) {
                if (isBaseUrlValid(propBaseUrl)) {
                    crowdinProperties.setBaseUrl(propBaseUrl);
                } else {
                    errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.invalid_url_property"), BASE_URL));
                }
            }
            String disabledBranches = properties.getProperty(PROPERTY_DISABLE_BRANCHES);
            if (disabledBranches != null) {
                crowdinProperties.setDisabledBranches(Boolean.parseBoolean(disabledBranches));
            } else {
                crowdinProperties.setDisabledBranches(DISABLE_BRANCHES_DEFAULT);
            }
            String preserveHierarchy = properties.getProperty(PROPERTY_PRESERVE_HIERARCHY);
            if (preserveHierarchy != null) {
                crowdinProperties.setPreserveHierarchy(Boolean.parseBoolean(preserveHierarchy));
            } else {
                crowdinProperties.setPreserveHierarchy(PRESERVE_HIERARCHY_DEFAULT);
            }

            Map<String, String> files = getSources(properties, errors);
            crowdinProperties.setSourcesWithPatterns(files);
        }

        if (!errors.isEmpty()) {
            String errorsInOne = String.join("\n", errors);
            throw new RuntimeException(MESSAGES_BUNDLE.getString("errors.config.has_errors") + "\n" + errorsInOne);
        }

        return crowdinProperties;
    }

    private static Map<String, String> getSources(Properties properties, List<String> errors) {
        Map<String, String> values = getSourcesList(properties);
        values.putAll(getSourcesWithTranslations(properties, errors));
        if (values.isEmpty()) {
            values.put("**/" + STANDARD_SOURCE_PATH + "/" + STANDARD_SOURCE_NAME, STANDARD_TRANSLATION_PATTERN);
        }
        return values;
    }

    private static Map<String, String> getSourcesWithTranslations(Properties properties, List<String> errors) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String ident = (i == 0) ? "" : i + ".";
            String filesSource = String.format(PROPERTY_FILES_SOURCES_PATTERN, ident);
            String filesTranslation = String.format(PROPERTY_FILES_TRANSLATIONS_PATTERN, ident);
            if (properties.containsKey(filesSource) && properties.containsKey(filesTranslation)) {
                values.put(StringUtils.removeStart(properties.getProperty(filesSource).replaceAll("[\\\\/]+", "/"), "/"), properties.getProperty(filesTranslation).replaceAll("[\\\\/]+", "/"));
            } else if (properties.containsKey(filesSource)) {
                errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), filesTranslation));
            } else if (properties.containsKey(filesTranslation)) {
                errors.add(String.format(MESSAGES_BUNDLE.getString("errors.config.missing_property"), filesSource));
            }
        }
        return values;
    }

    private static Map<String, String> getSourcesList(Properties properties) {
        String sources = properties.getProperty(PROPERTY_SOURCES);
        if (sources == null) {
            return new HashMap<>();
        }
        return Arrays.stream(sources.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotEmpty)
            .map(s -> "**/" + STANDARD_SOURCE_PATH + "/" + s)
            .collect(Collectors.toMap(Function.identity(), (s) -> STANDARD_TRANSLATION_PATTERN));
    }

    protected static boolean isBaseUrlValid(String baseUrl) {
        return BASE_URL_PATTERN.matcher(baseUrl).matches();
    }
}
