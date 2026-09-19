package com.luojia.soundscape.dispatch.job;

import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.search.client.SearchFeignClient;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchHandler {

    @Autowired
    private SearchFeignClient searchFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @XxlJob("firstJobHandler")
    public void firstJobHandler() {
        String message = "XXL-JOB 与珞珈声场调度服务集成成功";
        log.info(message);
        XxlJobHelper.log(message);
    }

    @XxlJob("updateAlbumRanking")
    public void updateAlbumRanking() {
        String jobParam = XxlJobHelper.getJobParam();
        int topN = 10;
        if (jobParam != null && !jobParam.trim().isEmpty()) {
            try {
                topN = Integer.parseInt(jobParam.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("任务参数必须是正整数，例如 10", e);
            }
        }
        if (topN <= 0) {
            throw new IllegalArgumentException("排行榜数量必须大于 0");
        }

        XxlJobHelper.log("开始刷新专辑排行榜，topN={}", topN);
        Result result = searchFeignClient.updateLatelyAlbumRanking(topN);
        if (result == null || !Integer.valueOf(200).equals(result.getCode())) {
            String message = result == null ? "搜索服务无响应" : result.getMessage();
            throw new IllegalStateException("刷新专辑排行榜失败：" + message);
        }
        log.info("定时任务刷新专辑排行榜成功，topN={}", topN);
        XxlJobHelper.log("专辑排行榜刷新成功，topN={}", topN);
    }

    @XxlJob("updateVipExpireStatus")
    public void updateVipExpireStatus() {
        XxlJobHelper.log("开始处理已过期 VIP 用户");
        Result<Integer> result = userFeignClient.updateVipExpireStatus();
        if (result == null || !Integer.valueOf(200).equals(result.getCode())) {
            String message = result == null ? "用户服务无响应" : result.getMessage();
            throw new IllegalStateException("处理 VIP 过期状态失败：" + message);
        }
        int updated = result.getData() == null ? 0 : result.getData();
        log.info("定时任务处理 VIP 过期状态成功，更新用户数={}", updated);
        XxlJobHelper.log("VIP 过期状态处理成功，更新用户数={}", updated);
    }
}
