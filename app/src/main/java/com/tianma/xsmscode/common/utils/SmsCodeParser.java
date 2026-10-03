package com.tianma.xsmscode.common.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** 默认识别算法保持为纯 Java，供手机与本地回归检查共用。 */
public final class SmsCodeParser {
    private static final String SEPARATORS = "[\\s\\u00a0\\u202f\\u200b\\u200e\\u200f\\u2060-]+";
    private static final Pattern CANDIDATES = Pattern.compile(
            "(?<![a-zA-Z0-9])(?:[0-9]{1,4}(?:" + SEPARATORS
                    + "[0-9]{1,4}){1,7}|[a-zA-Z0-9]+)(?![a-zA-Z0-9])");
    private static final Pattern CODE_SEPARATORS = Pattern.compile(SEPARATORS);
    private static final Pattern DATE = Pattern.compile("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}");

    private SmsCodeParser() { }

    public static boolean isValidRegex(String regex) {
        if (regex == null || regex.isEmpty()) return false;
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    public static String parse(String content, String keywordsRegex) {
        if (content == null || content.isEmpty() || !isValidRegex(keywordsRegex)) return "";
        String text = normalize(content);
        List<int[]> keywords = new ArrayList<>();
        Matcher keywordMatcher = Pattern.compile(keywordsRegex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        while (keywordMatcher.find()) {
            if (keywordMatcher.start() < keywordMatcher.end()) {
                keywords.add(new int[] { keywordMatcher.start(), keywordMatcher.end() });
            }
        }
        if (keywords.isEmpty()) return "";
        String best = "";
        int bestDistance = Integer.MAX_VALUE;
        int bestLevel = -1;
        Matcher candidates = CANDIDATES.matcher(text);
        while (candidates.find()) {
            String raw = candidates.group();
            String code = CODE_SEPARATORS.matcher(raw).replaceAll("");
            // 纯品牌词不能作为默认验证码；纯字母验证码仍可通过自定义规则提取。
            if (code.length() < 4 || code.length() > 8 || !code.matches(".*[0-9].*")) continue;
            if (DATE.matcher(raw).matches() || isNumberFragment(text, candidates.start(), candidates.end())) continue;
            int distance = Integer.MAX_VALUE;
            for (int[] keyword : keywords) {
                int gap = Math.max(0, Math.max(keyword[0] - candidates.end(), candidates.start() - keyword[1]));
                distance = Math.min(distance, gap);
            }
            // 不再把离关键词很远的任意数字当作兜底验证码。
            if (distance > 30) continue;
            int level = code.matches("[0-9]{6}") ? 3 : code.matches("[0-9]{4}") ? 2
                    : code.matches("[0-9]+") ? 1 : 0;
            if (distance < bestDistance || (distance == bestDistance && level > bestLevel)) {
                best = code;
                bestDistance = distance;
                bestLevel = level;
            }
        }
        return best;
    }

    private static boolean isNumberFragment(String text, int start, int end) {
        if (start >= 2 && ".,:/-".indexOf(text.charAt(start - 1)) >= 0
                && Character.isDigit(text.charAt(start - 2))) return true;
        if (end + 1 < text.length() && ".,:/-".indexOf(text.charAt(end)) >= 0
                && Character.isDigit(text.charAt(end + 1))) return true;
        // 金额、年份和有效期等数值不参与默认候选排序。
        if (start > 0 && "$￥€£".indexOf(text.charAt(start - 1)) >= 0) return true;
        return end < text.length() && "年月日时分秒元".indexOf(text.charAt(end)) >= 0;
    }

    private static String normalize(String content) {
        String text = Normalizer.normalize(content, Normalizer.Form.NFKC);
        StringBuilder normalized = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int digit = Character.digit(ch, 10);
            normalized.append(digit >= 0 ? (char) ('0' + digit) : ch);
        }
        return normalized.toString();
    }
}
