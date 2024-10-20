package com.yupi.wisdomShare.model.vo;

import cn.hutool.json.JSONUtil;
import com.yupi.wisdomShare.model.entity.APP;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 应用视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class APPVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 封装类转对象
     *
     * @param appVO
     * @return
     */
    public static APP voToObj(APPVO appVO) {
        if (appVO == null) {
            return null;
        }
        APP app = new APP();
        BeanUtils.copyProperties(appVO, app);
        List<String> tagList = appVO.getTagList();
        app.setTags(JSONUtil.toJsonStr(tagList));
        return app;
    }

    /**
     * 对象转封装类
     *
     * @param app
     * @return
     */
    public static APPVO objToVo(APP app) {
        if (app == null) {
            return null;
        }
        APPVO appVO = new APPVO();
        BeanUtils.copyProperties(app, appVO);
        appVO.setTagList(JSONUtil.toList(app.getTags(), String.class));
        return appVO;
    }
}
