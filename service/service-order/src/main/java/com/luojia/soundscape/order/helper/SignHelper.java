package com.luojia.soundscape.order.helper;

import com.luojia.soundscape.common.execption.SoundscapeException;
import com.luojia.soundscape.common.result.ResultCodeEnum;
import com.luojia.soundscape.common.util.MD5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class SignHelper {

    private static final String SIGN_KEY_PROPERTY = "soundscape.sign-key";
    private static final String SIGN_KEY_ENV = "LUOJIA_SOUNDSCAPE_SIGN_KEY";
    /**
     * 验签方法
     * @param parameterMap
     */
    public static void checkSign(Map<String, Object> parameterMap){
        //校验签名时间
        Long remoteTimestamp = (Long)parameterMap.get("timestamp");
        if(StringUtils.isEmpty(remoteTimestamp)){
            throw new SoundscapeException(ResultCodeEnum.SIGN_ERROR);
        }
        long currentTimestamp = getTimestamp();
        if (Math.abs(currentTimestamp - remoteTimestamp) > 500000) {
            log.error("签名已过期，服务器当前时间:{}", currentTimestamp);
            throw new SoundscapeException(ResultCodeEnum.SIGN_OVERDUE);
        }

        //校验签名
        //获取结算接口生成的签名
        String signRemote = (String)parameterMap.get("sign");

        //再次生成新签名
        String signLocal = getSign(parameterMap);
        if(StringUtils.isEmpty(signRemote)){
            throw new SoundscapeException(ResultCodeEnum.SIGN_ERROR);
        }

        if(!signRemote.equals(signLocal)){
            throw new SoundscapeException(ResultCodeEnum.SIGN_ERROR);
        }
    }

    /**
     * 请求数据获取签名
     * @param parameterMap
     * @return
     */
    public static String getSign(Map<String, Object> parameterMap) {
        //去掉sign参数
        if(parameterMap.containsKey("sign")) {
            parameterMap.remove("sign");
        }

        //有序
        TreeMap<String, Object> sorted = new TreeMap<>(parameterMap);
        StringBuilder str = new StringBuilder();
        for (Map.Entry<String, Object> param : sorted.entrySet()) {
            //获取键值对中的值
            str.append(param.getValue()).append("|");
        }
        // 签名密钥只能通过 JVM 参数或环境变量注入，不在仓库中保存默认值。
        String signKey = System.getProperty(SIGN_KEY_PROPERTY, System.getenv(SIGN_KEY_ENV));
        if (!StringUtils.hasText(signKey)) {
            throw new IllegalStateException(
                    "缺少签名密钥：请设置 -D" + SIGN_KEY_PROPERTY + " 或 " + SIGN_KEY_ENV);
        }
        str.append(signKey);
        String md5Str = MD5.encrypt(str.toString());//不可逆加密算法
        return md5Str;
    }

    /**
     * 获取时间戳
     * @return
     */
    public static long getTimestamp() {
        return new Date().getTime();
    }

}
