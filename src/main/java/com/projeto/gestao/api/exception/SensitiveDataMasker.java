package com.projeto.gestao.api.exception;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {
    private static final Pattern CPF = Pattern.compile("(?<!\\d)(\\d{3})\\.?\\d{3}\\.?\\d{3}-?(\\d{2})(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)(?<![a-z0-9._%+-])([a-z0-9._%+-])[a-z0-9._%+-]*@([a-z0-9.-]+)");
    private static final Pattern AUTHORIZATION = Pattern.compile(
            "(?i)(authorization)\\s*[:=]\\s*(?:bearer\\s+)?[^,;\\r\\n]+");
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(password|senha|cookie|api[-_]?key)\\s*[:=]\\s*"
                    + "(?:\"[^\"]*\"|'[^']*'|[^,;\\r\\n]+)");

    private SensitiveDataMasker() {
    }

    public static String mask(String value) {
        if (value == null) {
            return null;
        }
        String withoutAuthorization = AUTHORIZATION.matcher(value).replaceAll("$1=***");
        String withoutSecrets = SECRET.matcher(withoutAuthorization).replaceAll("$1=***");
        String cpfMasked = CPF.matcher(withoutSecrets).replaceAll("$1.***.***-$2");
        Matcher emailMatcher = EMAIL.matcher(cpfMasked);
        StringBuffer result = new StringBuffer();
        while (emailMatcher.find()) {
            emailMatcher.appendReplacement(result,
                    Matcher.quoteReplacement(emailMatcher.group(1) + "***@" + emailMatcher.group(2)));
        }
        emailMatcher.appendTail(result);
        return result.toString();
    }
}
