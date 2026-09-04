package com.mdl.platform.users.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
public class UserCredentialGenerator {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final String EMAIL_DOMAIN = "@mdl.local";

    public String generateEmail(String firstName, String lastName, Predicate<String> emailTaken) {
        String last = slug(lastName);
        String first = slug(firstName);
        String localPart = buildLocalPart(last, first);
        localPart = localPart.substring(0, Math.min(localPart.length(), 64));

        String candidate = localPart + EMAIL_DOMAIN;
        int suffix = 2;
        while (emailTaken.test(candidate)) {
            candidate = localPart + suffix + EMAIL_DOMAIN;
            suffix++;
        }
        return candidate;
    }

    public String generateUsername(String firstName, String lastName, Predicate<String> usernameTaken) {
        String first = slug(firstName);
        String last = slug(lastName);
        String base = (first + last);
        if (base.length() < 3) {
            base = (first + "user").substring(0, Math.max(3, (first + "user").length()));
        }
        base = base.substring(0, Math.min(base.length(), 90));

        String candidate = base;
        int suffix = 2;
        while (usernameTaken.test(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    public String generatePassword(String firstName, String lastName) {
        String first = capitalizeWord(firstName);
        String last = capitalizeWord(lastName);
        if (first.isEmpty()) {
            first = "User";
        }
        if (last.isEmpty()) {
            last = "MDL";
        }
        String password = first + "@" + last;
        if (password.length() < 8) {
            password = first + "@" + last + "1!";
        }
        return password;
    }

    private String buildLocalPart(String last, String first) {
        if (last.isEmpty() && first.isEmpty()) {
            return "user";
        }
        if (last.isEmpty()) {
            return first;
        }
        if (first.isEmpty()) {
            return last;
        }
        return last + "." + first;
    }

    private String slug(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return NON_ALNUM.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private String capitalizeWord(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT)
                + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }
}
