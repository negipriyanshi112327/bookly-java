package com.example.demo.controller;

/**
 * Builds "redirect:" view names from a URL sent back by a form (e.g. return to the page the
 * user was on). Only local paths are accepted, so the app cannot be used to redirect to
 * another website.
 */
final class SafeRedirect {

    private SafeRedirect() {}

    static String to(String target, String fallback) {
        if (target == null || target.isBlank()
                || !target.startsWith("/") || target.startsWith("//")
                || target.contains("\\") || target.contains("\r") || target.contains("\n")
                || target.contains("{") || target.contains("}")
                || target.length() > 500) {
            return "redirect:" + fallback;
        }
        return "redirect:" + target;
    }
}
