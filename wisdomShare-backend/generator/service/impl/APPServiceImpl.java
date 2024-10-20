package com.yupi.wisdomShare.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.wisdomShare.common.ErrorCode;
import com.yupi.wisdomShare.constant.CommonConstant;
import com.yupi.wisdomShare.exception.ThrowUtils;
import com.yupi.wisdomShare.mapper.APPMapper;
import com.yupi.wisdomShare.model.dto.app.APPQueryRequest;
import com.yupi.wisdomShare.model.entity.APP;
import com.yupi.wisdomShare.model.entity.APPFavour;
import com.yupi.wisdomShare.model.entity.APPThumb;
import com.yupi.wisdomShare.model.entity.User;
import com.yupi.wisdomShare.model.vo.APPVO;
import com.yupi.wisdomShare.model.vo.UserVO;
import com.yupi.wisdomShare.service.APPService;
import com.yupi.wisdomShare.service.UserService;
import com.yupi.wisdomShare.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class APPServiceImpl extends ServiceImpl<APPMapper, APP> implements APPService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param app
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validAPP(APP app, boolean add) {
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = app.getTitle();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param appQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<APP> getQueryWrapper(APPQueryRequest appQueryRequest) {
        QueryWrapper<APP> queryWrapper = new QueryWrapper<>();
        if (appQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = appQueryRequest.getId();
        Long notId = appQueryRequest.getNotId();
        String title = appQueryRequest.getTitle();
        String content = appQueryRequest.getContent();
        String searchText = appQueryRequest.getSearchText();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        List<String> tagList = appQueryRequest.getTags();
        Long userId = appQueryRequest.getUserId();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取应用封装
     *
     * @param app
     * @param request
     * @return
     */
    @Override
    public APPVO getAPPVO(APP app, HttpServletRequest request) {
        // 对象转封装类
        APPVO appVO = APPVO.objToVo(app);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = app.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        appVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long appId = app.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<APPThumb> appThumbQueryWrapper = new QueryWrapper<>();
            appThumbQueryWrapper.in("appId", appId);
            appThumbQueryWrapper.eq("userId", loginUser.getId());
            APPThumb appThumb = appThumbMapper.selectOne(appThumbQueryWrapper);
            appVO.setHasThumb(appThumb != null);
            // 获取收藏
            QueryWrapper<APPFavour> appFavourQueryWrapper = new QueryWrapper<>();
            appFavourQueryWrapper.in("appId", appId);
            appFavourQueryWrapper.eq("userId", loginUser.getId());
            APPFavour appFavour = appFavourMapper.selectOne(appFavourQueryWrapper);
            appVO.setHasFavour(appFavour != null);
        }
        // endregion

        return appVO;
    }

    /**
     * 分页获取应用封装
     *
     * @param appPage
     * @param request
     * @return
     */
    @Override
    public Page<APPVO> getAPPVOPage(Page<APP> appPage, HttpServletRequest request) {
        List<APP> appList = appPage.getRecords();
        Page<APPVO> appVOPage = new Page<>(appPage.getCurrent(), appPage.getSize(), appPage.getTotal());
        if (CollUtil.isEmpty(appList)) {
            return appVOPage;
        }
        // 对象列表 => 封装对象列表
        List<APPVO> appVOList = appList.stream().map(app -> {
            return APPVO.objToVo(app);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = appList.stream().map(APP::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> appIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> appIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> appIdSet = appList.stream().map(APP::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<APPThumb> appThumbQueryWrapper = new QueryWrapper<>();
            appThumbQueryWrapper.in("appId", appIdSet);
            appThumbQueryWrapper.eq("userId", loginUser.getId());
            List<APPThumb> appAPPThumbList = appThumbMapper.selectList(appThumbQueryWrapper);
            appAPPThumbList.forEach(appAPPThumb -> appIdHasThumbMap.put(appAPPThumb.getAPPId(), true));
            // 获取收藏
            QueryWrapper<APPFavour> appFavourQueryWrapper = new QueryWrapper<>();
            appFavourQueryWrapper.in("appId", appIdSet);
            appFavourQueryWrapper.eq("userId", loginUser.getId());
            List<APPFavour> appFavourList = appFavourMapper.selectList(appFavourQueryWrapper);
            appFavourList.forEach(appFavour -> appIdHasFavourMap.put(appFavour.getAPPId(), true));
        }
        // 填充信息
        appVOList.forEach(appVO -> {
            Long userId = appVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            appVO.setUser(userService.getUserVO(user));
            appVO.setHasThumb(appIdHasThumbMap.getOrDefault(appVO.getId(), false));
            appVO.setHasFavour(appIdHasFavourMap.getOrDefault(appVO.getId(), false));
        });
        // endregion

        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

}
