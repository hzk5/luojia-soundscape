package com.luojia.soundscape.album.service.impl;

import cn.hutool.core.util.IdUtil;
import com.luojia.soundscape.album.config.MinioConstantProperties;
import com.luojia.soundscape.album.config.VodConstantProperties;
import com.luojia.soundscape.album.service.VodService;
import com.luojia.soundscape.common.util.UploadFileUtil;
import com.luojia.soundscape.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Autowired
    private VodUploadClient vodUploadClient;

    @Autowired
    private VodClient vodClient;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    /**
     * 本地开发默认使用 MinIO；上线后在 Nacos 中设为 false 即可恢复腾讯云 VOD。
     */
    @Value("${luojia-soundscape.vod.local-enabled:true}")
    private boolean localEnabled;

    @Value("${luojia-soundscape.vod.local-default-duration:60}")
    private Float localDefaultDuration;

    /**
     * 文件上传，将音视频文件上传到点播平台
     *
     * @param file 文件
     * @return {mediaFileId:"文件唯一标识",mediaUrl:"播放地址"}
     */
    @Override
    public Map<String, String> uploadTrack(MultipartFile file) {
        if (localEnabled) {
            return uploadTrackToMinio(file);
        }
        try {
            //1.将接收到文件保存到当前服务器指定 临时目录下
            String filePath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
            //2.调用点播平台SDK上传文件
            //2.1 构造上传请求对象
            VodUploadRequest request = new VodUploadRequest();
            request.setMediaFilePath(filePath);
            //2.2 调用上传方法，传入接入点地域及上传请求。
            VodUploadResponse response = vodUploadClient.upload(vodConstantProperties.getRegion(), request);

            //3.封装返回结果
            if (response != null) {
                return Map.of("mediaFileId", response.getFileId(), "mediaUrl", response.getMediaUrl());
            }
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private Map<String, String> uploadTrackToMinio(MultipartFile file) {
        try {
            // media_file_id 字段长度只有 30，因此使用 local- + 雪花 ID。
            String mediaFileId = "local-" + IdUtil.getSnowflakeNextIdStr();
            String objectName = "audio/" + mediaFileId;
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "audio/mpeg";
            }
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConstantProperties.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
            String endpoint = minioConstantProperties.getEndpointUrl().replaceAll("/+$", "");
            String mediaUrl = endpoint + "/" + minioConstantProperties.getBucketName() + "/" + objectName;
            return Map.of("mediaFileId", mediaFileId, "mediaUrl", mediaUrl);
        } catch (Exception e) {
            log.error("上传音频到 MinIO 失败", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 从点播平台获取媒体文件详情，得到媒体文件详情
     *
     * @param mediaFileId 文件唯一标识
     * @return
     */
    @Override
    public TrackMediaInfoVo getMediaInfo(String mediaFileId) {
        if (isLocalMedia(mediaFileId)) {
            return getMinioMediaInfo(mediaFileId);
        }
        try {
            //1.实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            String[] fileIds1 = {mediaFileId};
            req.setFileIds(fileIds1);
            //2.发起请求获取音频文件详细列表
            DescribeMediaInfosResponse resp = vodClient.DescribeMediaInfos(req);
            //3.解析结果
            if (resp != null) {
                MediaInfo[] mediaInfoSet = resp.getMediaInfoSet();
                if (mediaInfoSet != null && mediaInfoSet.length > 0) {
                    MediaInfo mediaInfo = mediaInfoSet[0];
                    //3.1 获取基本信息 音频文件类型
                    String type = mediaInfo.getBasicInfo().getType();
                    //3.2 获取元信息 时长、大小
                    MediaMetaData metaData = mediaInfo.getMetaData();
                    Float audioDuration = metaData.getAudioDuration();
                    Long size = metaData.getSize();
                    //3.封装结果
                    TrackMediaInfoVo vo = new TrackMediaInfoVo();
                    vo.setDuration(audioDuration);
                    vo.setSize(size);
                    vo.setType(type);
                    return vo;
                }
            }
        } catch (TencentCloudSDKException e) {
            log.error("获取媒体文件详情失败", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private TrackMediaInfoVo getMinioMediaInfo(String mediaFileId) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConstantProperties.getBucketName())
                            .object("audio/" + mediaFileId)
                            .build()
            );
            TrackMediaInfoVo vo = new TrackMediaInfoVo();
            vo.setSize(stat.size());
            vo.setDuration(localDefaultDuration);
            vo.setType(resolveMediaType(stat.contentType()));
            return vo;
        } catch (Exception e) {
            log.error("读取 MinIO 音频信息失败，mediaFileId={}", mediaFileId, e);
            throw new RuntimeException(e);
        }
    }

    private String resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "audio";
        }
        String type = contentType.contains("/") ? contentType.substring(contentType.indexOf('/') + 1) : contentType;
        type = type.contains(";") ? type.substring(0, type.indexOf(';')) : type;
        return "mpeg".equalsIgnoreCase(type) ? "mp3" : type;
    }

    private boolean isLocalMedia(String mediaFileId) {
        return mediaFileId != null && mediaFileId.startsWith("local-");
    }

    @Override
    public void deleteMedia(String mediaFileId) {
        if (isLocalMedia(mediaFileId)) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConstantProperties.getBucketName())
                                .object("audio/" + mediaFileId)
                                .build()
                );
            } catch (Exception e) {
                log.error("删除 MinIO 音频失败，mediaFileId={}", mediaFileId, e);
            }
            return;
        }
        try {
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(mediaFileId);
            // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            vodClient.DeleteMedia(req);
        } catch (TencentCloudSDKException e) {
            log.error("删除媒体文件失败", e);
        }
    }
}
