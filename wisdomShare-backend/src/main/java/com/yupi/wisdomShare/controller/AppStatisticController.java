package com.yupi.wisdomShare.controller;

import cn.hutool.core.io.FileUtil;
import com.yupi.wisdomShare.common.BaseResponse;
import com.yupi.wisdomShare.common.ErrorCode;
import com.yupi.wisdomShare.common.ResultUtils;
import com.yupi.wisdomShare.constant.FileConstant;
import com.yupi.wisdomShare.exception.BusinessException;
import com.yupi.wisdomShare.exception.ThrowUtils;
import com.yupi.wisdomShare.manager.CosManager;
import com.yupi.wisdomShare.mapper.UserAnswerMapper;
import com.yupi.wisdomShare.model.dto.file.UploadFileRequest;
import com.yupi.wisdomShare.model.dto.statistic.AppAnswerCountDTO;
import com.yupi.wisdomShare.model.dto.statistic.AppAnswerResultCountDTO;
import com.yupi.wisdomShare.model.entity.User;
import com.yupi.wisdomShare.model.enums.FileUploadBizEnum;
import com.yupi.wisdomShare.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * App 统计分析接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/app/statistic")
@Slf4j
public class AppStatisticController {

    @Resource
    private UserAnswerMapper userAnswerMapper;

    /**
     * 热门应用回答数统计(TOP 10)
     * @return
     */
    @GetMapping("/answer_count")
    public BaseResponse<List<AppAnswerCountDTO>> getAnswerCount() {
        return ResultUtils.success(userAnswerMapper.doAppAnswerCount());
    }

    /**
     * 某个应用回答结果分布统计
     * @param appId
     * @return
     */

    @GetMapping("/answer_result_count")
    public BaseResponse<List<AppAnswerResultCountDTO>> getAnswerResultCount(Long appId) {
        ThrowUtils.throwIf(appId==null||appId<=0,ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userAnswerMapper.doAppAnswerResultCount(appId));
    }


}
