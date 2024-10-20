package com.yupi.wisdomShare.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.wisdomShare.model.dto.app.APPQueryRequest;
import com.yupi.wisdomShare.model.entity.APP;
import com.yupi.wisdomShare.model.vo.APPVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 应用服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
public interface APPService extends IService<APP> {

    /**
     * 校验数据
     *
     * @param app
     * @param add 对创建的数据进行校验
     */
    void validAPP(APP app, boolean add);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper<APP> getQueryWrapper(APPQueryRequest appQueryRequest);
    
    /**
     * 获取应用封装
     *
     * @param app
     * @param request
     * @return
     */
    APPVO getAPPVO(APP app, HttpServletRequest request);

    /**
     * 分页获取应用封装
     *
     * @param appPage
     * @param request
     * @return
     */
    Page<APPVO> getAPPVOPage(Page<APP> appPage, HttpServletRequest request);
}
