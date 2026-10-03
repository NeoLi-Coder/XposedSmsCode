package com.tianma.xsmscode.common.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.github.tianma8023.xposed.smscode.BuildConfig;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.data.db.DBProvider;
import com.tianma.xsmscode.data.db.entity.SmsCodeRule;
import com.tianma.xsmscode.data.db.entity.SmsCodeRuleDao;
import com.tianma.xsmscode.feature.store.EntityStoreManager;
import com.tianma.xsmscode.feature.store.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XSharedPreferences;

/**
 * 验证码相关Utils
 */
public class SmsCodeUtils {

    private SmsCodeUtils() {
    }

    private static String loadCodeKeywordsBySP(Context context) {
        return SPUtils.getSMSCodeKeywords(context);
    }

    private static String loadCodeKeywordsByXSP() {
        XSharedPreferences preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, PrefConst.PREF_NAME);
        return XSPUtils.getSMSCodeKeywords(preferences);
    }

    /**
     * 解析文本中的验证码并返回，如果不存在返回空字符
     */
    public static String parseSmsCodeIfExists(Context context, String content, boolean useXSP) {
        if (TextUtils.isEmpty(content)) return "";
        String result = parseByCustomRules(context, content);
        if (TextUtils.isEmpty(result)) {
            result = parseByDefaultRule(context, content, useXSP);
        }
        return result;
    }

    /**
     * Parse SMS code by default rule
     *
     * @param context context
     * @param content message body
     * @param useXSP  whether use XSharedPreferences or not.
     * @return the SMS code if matches, otherwise return empty string
     */
    private static String parseByDefaultRule(Context context, String content, boolean useXSP) {
        String keywords = useXSP ? loadCodeKeywordsByXSP() : loadCodeKeywordsBySP(context);
        if (!SmsCodeParser.isValidRegex(keywords)) {
            XLog.e("Invalid SMS keywords regex, using defaults");
            keywords = PrefConst.SMSCODE_KEYWORDS_DEFAULT;
        }
        return SmsCodeParser.parse(content, keywords);
    }

    /**
     * Parse SMS code by custom rules
     *
     * @param context context
     * @param content message body
     * @return the SMS code if matches, otherwise return empty string
     */
    private static String parseByCustomRules(Context context, String content) {
        List<SmsCodeRule> rules = queryAllSmsCodeRules(context);
        String lowerContent = content.toLowerCase(Locale.ROOT);
        for (SmsCodeRule rule : rules) {
            if (rule == null || rule.getCompany() == null || rule.getCodeKeyword() == null
                    || TextUtils.isEmpty(rule.getCodeRegex())) continue;
            if (lowerContent.contains(rule.getCompany().toLowerCase(Locale.ROOT))
                    && lowerContent.contains(rule.getCodeKeyword().toLowerCase(Locale.ROOT))) {
                try {
                    Matcher matcher = Pattern.compile(rule.getCodeRegex()).matcher(content);
                    if (matcher.find() && !TextUtils.isEmpty(matcher.group())) return matcher.group();
                } catch (PatternSyntaxException e) {
                    XLog.e("Invalid custom SMS rule, skipping", e);
                }
            }
        }
        return "";
    }

    private static List<SmsCodeRule> queryAllSmsCodeRules(Context context) {
        List<SmsCodeRule> rules = new ArrayList<>();
        try {
            Uri smsCodeRuleUri = DBProvider.SMS_CODE_RULE_URI;
            ContentResolver resolver = context.getContentResolver();

            final String companyColumn = SmsCodeRuleDao.Properties.Company.columnName;
            final String keywordColumn = SmsCodeRuleDao.Properties.CodeKeyword.columnName;
            final String regexColumn = SmsCodeRuleDao.Properties.CodeRegex.columnName;

            String[] projection = {
                    companyColumn,
                    keywordColumn,
                    regexColumn,
            };

            try (Cursor cursor = resolver.query(smsCodeRuleUri, projection, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        SmsCodeRule rule = new SmsCodeRule();
                        rule.setCompany(cursor.getString(cursor.getColumnIndexOrThrow(companyColumn)));
                        rule.setCodeKeyword(cursor.getString(cursor.getColumnIndexOrThrow(keywordColumn)));
                        rule.setCodeRegex(cursor.getString(cursor.getColumnIndexOrThrow(regexColumn)));
                        rules.add(rule);
                    }
                    XLog.d("Load SmsCode rules succeed by content provider");
                } else {
                    throw new Exception("Cursor is null");
                }
            }
        } catch (Throwable e) {
            rules = EntityStoreManager.loadEntitiesFromFile(
                    EntityType.CODE_RULES, SmsCodeRule.class
            );
            XLog.d("Load SmsCode rules by file");
        }
        return rules == null ? new ArrayList<>() : rules;
    }

    /**
     * Parse company info from message content if it exists
     *
     * @param content message content
     * @return company info if it exists, otherwise return empty string
     */
    public static String parseCompany(String content) {
        if (TextUtils.isEmpty(content)) return "";
        String regex = "((?<=【)(.*?)(?=】))|((?<=\\[)(.*?)(?=\\]))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        List<String> possibleCompanies = new ArrayList<>();
        while (matcher.find()) {
            possibleCompanies.add(matcher.group());
        }
        StringBuilder sb = new StringBuilder();
        boolean needSpace = false; // 是否需要空格分隔
        for (String company : possibleCompanies) {
            if (needSpace) {
                sb.append(' ');
            } else {
                needSpace = true;
            }
            sb.append(company);
        }
        return sb.toString();
    }
}
