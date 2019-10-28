package milfont.com.tezosj_android.helper;
import org.apache.commons.lang3.StringUtils;

public class StringUtil {
    public static String unescapeJSONString(String jsonStr) {

        if (jsonStr.startsWith("\"")){
            jsonStr = StringUtils.removeFirst(jsonStr, "\"");
        }
        if (jsonStr.endsWith("\"")){
            jsonStr = StringUtils.removeEnd(jsonStr, "\"");
        }
        return StringUtils.replaceAll(StringUtils.replaceAll(jsonStr,"\\\\n",""), "\\\\", "");

    }
}
