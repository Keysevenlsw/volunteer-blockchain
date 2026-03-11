package com.gzu.volunteerblockchain.service.impl;

import com.gzu.volunteerblockchain.entity.Activity;
import com.gzu.volunteerblockchain.mapper.ActivityMapper;
import com.gzu.volunteerblockchain.service.PublicService;
import com.gzu.volunteerblockchain.vo.InfoItemVO;
import com.gzu.volunteerblockchain.vo.ProjectItemVO;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublicServiceImpl implements PublicService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ActivityMapper activityMapper;

    public PublicServiceImpl(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Override
    public List<InfoItemVO> getInfoList() {
        List<InfoItemVO> list = new ArrayList<>();
        list.add(new InfoItemVO("关于提升志愿服务认证质量的通知", "通知", "2026-03-05"));
        list.add(new InfoItemVO("春季城市关爱行动志愿者招募启动", "动态", "2026-02-26"));
        list.add(new InfoItemVO("公益组织积分商城规则更新说明", "政策", "2026-02-12"));
        list.add(new InfoItemVO("优秀志愿项目案例：社区关怀行动", "案例", "2026-01-28"));
        list.add(new InfoItemVO("平台新版上线：服务记录上链提速", "公告", "2026-01-18"));
        return list;
    }

    @Override
    public List<ProjectItemVO> getCompletedProjects(int limit) {
        List<Activity> activities = activityMapper.selectCompletedProjects(limit);
        List<ProjectItemVO> result = new ArrayList<>();
        if (activities == null) {
            return result;
        }
        for (Activity activity : activities) {
            ProjectItemVO vo = new ProjectItemVO();
            vo.setId(activity.getActivityId());
            vo.setTitle(activity.getActivityName());
            vo.setDescription(activity.getDescription());
            vo.setLocation(activity.getLocation());
            if (activity.getEndDate() != null) {
                vo.setEndDate(activity.getEndDate().format(DATE_FORMATTER));
            }
            vo.setImage(activity.getImagePath());
            result.add(vo);
        }
        return result;
    }
}
