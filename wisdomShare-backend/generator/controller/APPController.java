package com.yupi.wisdomShare.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.wisdomShare.annotation.AuthCheck;
import com.yupi.wisdomShare.common.BaseResponse;
import com.yupi.wisdomShare.common.DeleteRequest;
import com.yupi.wisdomShare.common.ErrorCode;
import com.yupi.wisdomShare.common.ResultUtils;
import com.yupi.wisdomShare.constant.UserConstant;
import com.yupi.wisdomShare.exception.BusinessException;
import com.yupi.wisdomShare.exception.ThrowUtils;
import com.yupi.wisdomShare.model.dto.app.APPAddRequest;
import com.yupi.wisdomShare.model.dto.app.APPEditRequest;
import com.yupi.wisdomShare.model.dto.app.APPQueryRequest;
import com.yupi.wisdomShare.model.dto.app.APPUpdateRequest;
import com.yupi.wisdomShare.model.entity.APP;
import com.yupi.wisdomShare.model.entity.User;
import com.yupi.wisdomShare.model.vo.APPVO;
import com.yupi.wisdomShare.service.APPService;
import com.yupi.wisdomShare.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 应用接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/app")
@Slf4j
public class APPController {

    @Resource
    private APPService appService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建应用
     *
     * @param appAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addAPP(@RequestBody APPAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        APP app = new APP();
        BeanUtils.copyProperties(appAddRequest, app);
        // 数据校验
        appService.validAPP(app, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        app.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newAPPId = app.getId();
        return ResultUtils.success(newAPPId);
    }

    /**
     * 删除应用
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteAPP(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        APP oldAPP = appService.getById(id);
        ThrowUtils.throwIf(oldAPP == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldAPP.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = appService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新应用（仅管理员可用）
     *
     * @param appUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAPP(@RequestBody APPUpdateRequest appUpdateRequest) {
        if (appUpdateRequest == null || appUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        APP app = new APP();
        BeanUtils.copyProperties(appUpdateRequest, app);
        // 数据校验
        appService.validAPP(app, false);
        // 判断是否存在
        long id = appUpdateRequest.getId();
        APP oldAPP = appService.getById(id);
        ThrowUtils.throwIf(oldAPP == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取应用（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<APPVO> getAPPVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        APP app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAPPVO(app, request));
    }

    /**
     * 分页获取应用列表（仅管理员可用）
     *
     * @param appQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<APP>> listAPPByPage(@RequestBody APPQueryRequest appQueryRequest) {
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        // 查询数据库
        Page<APP> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appPage);
    }

    /**
     * 分页获取应用列表（封装类）
     *
     * @param appQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<APPVO>> listAPPVOByPage(@RequestBody APPQueryRequest appQueryRequest,
                                                               HttpServletRequest request) {
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<APP> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        // 获取封装类
        return ResultUtils.success(appService.getAPPVOPage(appPage, request));
    }

    /**
     * 分页获取当前登录用户创建的应用列表
     *
     * @param appQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<APPVO>> listMyAPPVOByPage(@RequestBody APPQueryRequest appQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        appQueryRequest.setUserId(loginUser.getId());
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<APP> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        // 获取封装类
        return ResultUtils.success(appService.getAPPVOPage(appPage, request));
    }

    /**
     * 编辑应用（给用户使用）
     *
     * @param appEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editAPP(@RequestBody APPEditRequest appEditRequest, HttpServletRequest request) {
        if (appEditRequest == null || appEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        APP app = new APP();
        BeanUtils.copyProperties(appEditRequest, app);
        // 数据校验
        appService.validAPP(app, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = appEditRequest.getId();
        APP oldAPP = appService.getById(id);
        ThrowUtils.throwIf(oldAPP == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldAPP.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
