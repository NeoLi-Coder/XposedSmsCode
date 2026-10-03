package com.tianma.xsmscode.common.utils;

import com.tianma.xsmscode.common.constant.SmsCodeConst;

/** 无需 Android 或测试框架，使用 scripts/check-core.ps1 执行。 */
public final class SmsCodeParserCheck {
    private static int checked;
    private static void check(String expected, String message) {
        String actual = SmsCodeParser.parse(message, SmsCodeConst.VERIFICATION_KEYWORDS_REGEX);
        if (!expected.equals(actual)) throw new AssertionError(message + " -> " + actual + ", expected " + expected);
        checked++;
    }
    public static void main(String[] args) {
        check("123456", "【银行】验证码123456，请勿泄露。");
        check("1234", "您的验证码是1234，有效期5分钟。");
        check("987654", "Your verification CODE is 987654.");
        check("A1B2C3", "Your login code is A1B2C3.");
        check("123456", "Wise：验证码为 123456，请使用。");
        check("123456", "Your PayPal code: 123456");
        check("123456", "验证码：１２３４５６");
        check("123456", "OTP: ١٢٣٤٥٦");
        check("123456", "OTP: ۱۲۳۴۵۶");
        check("123456", "验证码 1 2 3 4 5 6");
        check("123456", "验证码 123 456");
        check("123456", "验证码 123-456");
        check("123456", "验证码 12-34-56");
        check("123456", "验证码 1\u200b2\u200b3\u200b4\u200b5\u200b6");
        check("123456", "验证码 123\u00a0456");
        check("654321", "123456元到账，验证码 654321。");
        check("", "验证码服务：余额123456.78元。");
        check("", "Code service balance: 1234,5678");
        check("", "验证码服务日期：2026-10-03");
        check("", "验证码服务日期：2026/10/03");
        check("", "验证码服务于2026年到期");
        check("", "验证码支持电话：13812345678");
        check("", "Code service: 123456789");
        check("", "Code service contact Wise and PayPal");
        check("", "普通短信：123456");
        check("", "验证码服务暂停。" + "请稍后再尝试。".repeat(10) + "123456");
        check("222222", "111111：旧验证码已失效；请使用新验证码 222222");
        check("123456", "123456 is your verification code.");
        check("", null);
        check("", "");
        if (SmsCodeParser.isValidRegex("[") || SmsCodeParser.isValidRegex(null)) throw new AssertionError("Invalid regex accepted");
        if (!SmsCodeParser.isValidRegex("(?<=验证码)[0-9]{4,8}")) throw new AssertionError("Valid rule rejected");
        if (!SmsCodeParser.parse("验证码 123456", "[").isEmpty()) throw new AssertionError("Invalid keyword regex parsed");
        System.out.println("Parser regression checks passed: " + checked + " messages and regex validation");
    }
}
