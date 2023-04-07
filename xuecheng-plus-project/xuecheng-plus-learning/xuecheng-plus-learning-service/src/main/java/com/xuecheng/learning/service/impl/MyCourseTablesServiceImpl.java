package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 选课相关接口实现
 */
@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Resource
    private XcChooseCourseMapper chooseCourseMapper;

    @Resource
    private XcCourseTablesMapper courseTablesMapper;

    @Resource
    private ContentServiceClient contentServiceClient;

    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        // 远程调用查询课程的收费规则
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        XcChooseCourse chooseCourse = null;
        if (coursepublish == null) {
            XueChengPlusException.cast("课程不存在");
        }
        String charge = coursepublish.getCharge();
        if ("201000".equals(charge)) {
            // 免费课程 写入选课记录表 我的课程表
            chooseCourse = addFreeCourse(userId, coursepublish);
            XcCourseTables courseTables = addCourseTables(chooseCourse);
        } else {
            // 收费课程 写入选课记录表
            chooseCourse = addChargeCourse(userId, coursepublish);
        }

        // 判断学生的学习资格
        XcCourseTablesDto courseTablesDto = getLearningStatus(userId, courseId);

        XcChooseCourseDto chooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse, chooseCourseDto);
        chooseCourseDto.setLearnStatus(courseTablesDto.getLearnStatus());

        return chooseCourseDto;
    }

    //[{"code":"702001","desc":"正常学习"},
    // {"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //返回的结果
        XcCourseTablesDto courseTablesDto = new XcCourseTablesDto();

        //查询我的课程表，如果查不到说明没有选课
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables == null) {
            //"code":"702002","desc":"没有选课或选课后没有支付"
            courseTablesDto.setLearnStatus("702002");
            return courseTablesDto;
        }
        //如果查到了，判断是否过期，如果过期不能继续学习，没有过期可以继续学习

        boolean before = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        BeanUtils.copyProperties(xcCourseTables, courseTablesDto);
        if (before) {
            //"code":"702003","desc":"已过期需要申请续期或重新支付"
            courseTablesDto.setLearnStatus("702003");
        } else {
            //"code":"702001","desc":"正常学习"
            courseTablesDto.setLearnStatus("702001");
        }
        return courseTablesDto;
    }

    @Transactional
    @Override
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        // 根据选课id查询选课表
        XcChooseCourse chooseCourse = chooseCourseMapper.selectById(chooseCourseId);
        if (chooseCourse == null) {
            log.error("接受购买课程的消息，选课id关联的选课记录不存在：{}", chooseCourseId);
            return false;
        }
        // 选课状态
        String status = chooseCourse.getStatus();
        // 支付状态更新为已支付
        if ("701002".equals(status)) {
            // 更新选课记录的状态
            chooseCourse.setStatus("701001");
            int i = chooseCourseMapper.updateById(chooseCourse);
            if (i == 0) {
                log.error("添加选课记录失败:{}", chooseCourse);
                XueChengPlusException.cast("添加选课记录失败");
            }
            // 向我的课程表插入记录
            addCourseTables(chooseCourse);
        }
        return true;
    }

    /*String userId;

    //课程类型  [{"code":"700001","desc":"免费课程"},{"code":"700002","desc":"收费课程"}]
    private String courseType;

    //排序 1按学习时间进行排序 2按加入时间进行排序
    private String sortType;

    //1即将过期、2已经过期
    private String expiresType;*/
    @Override
    public PageResult<XcCourseTables> mycoursetables(MyCourseTableParams params) {
        Page<XcCourseTables> courseTablesPage = new Page<>(params.getPage(), params.getSize());
        LambdaQueryWrapper<XcCourseTables> lambdaQueryWrapper =
                new LambdaQueryWrapper<XcCourseTables>()
                        .eq(XcCourseTables::getUserId, params.getUserId());
//                        .eq(XcCourseTables::getCourseType, params.getCourseType());
        Page<XcCourseTables> result = courseTablesMapper.selectPage(courseTablesPage, lambdaQueryWrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getPages(), result.getSize());
    }

    //添加免费课程,免费课程加入选课记录表、我的课程表
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        // 课程id
        Long courseId = coursepublish.getId();
        LambdaQueryWrapper<XcChooseCourse> wrapper =
                new LambdaQueryWrapper<XcChooseCourse>()
                        .eq(XcChooseCourse::getCourseId, courseId)
                        .eq(XcChooseCourse::getUserId, userId)
                        .eq(XcChooseCourse::getOrderType, "700001")
                        .eq(XcChooseCourse::getStatus, "701001");
        List<XcChooseCourse> xcChooseCourses = chooseCourseMapper.selectList(wrapper);
        if (xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setStatus("701001");
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365L));

        int insert = chooseCourseMapper.insert(xcChooseCourse);
        if (insert == 0) {
            XueChengPlusException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }

    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId, CoursePublish coursepublish) {
        // 课程id
        // 存在收费的选课记录且选课状态为待支付 直接返回
        Long courseId = coursepublish.getId();
        LambdaQueryWrapper<XcChooseCourse> wrapper =
                new LambdaQueryWrapper<XcChooseCourse>()
                        .eq(XcChooseCourse::getCourseId, courseId)
                        .eq(XcChooseCourse::getUserId, userId)
                        .eq(XcChooseCourse::getOrderType, "700002")
                        .eq(XcChooseCourse::getStatus, "701002");
        List<XcChooseCourse> xcChooseCourses = chooseCourseMapper.selectList(wrapper);
        if (xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setStatus("701002");
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365L));

        int insert = chooseCourseMapper.insert(xcChooseCourse);
        if (insert == 0) {
            XueChengPlusException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }

    //添加到我的课程表
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse) {
        String status = xcChooseCourse.getStatus();
        Long id = xcChooseCourse.getId();
        if (!"701001".equals(status)) {
            XueChengPlusException.cast("选课没有成功无法添加到课程表");
        }
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null) {
            return xcCourseTables;
        }
        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse, xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());//记录选课表的id
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        xcCourseTables.setChooseCourseId(id);
        int insert = courseTablesMapper.insert(xcCourseTables);
        if (insert == 0) {
            XueChengPlusException.cast("添加我的课程失败");
        }
        return xcCourseTables;
    }

    /**
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @description 根据课程和用户查询我的课程表中某一门课程
     * @author Mr.M
     * @date 2022/10/2 17:07
     */
    public XcCourseTables getXcCourseTables(String userId, Long courseId) {
        return courseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));

    }
}
