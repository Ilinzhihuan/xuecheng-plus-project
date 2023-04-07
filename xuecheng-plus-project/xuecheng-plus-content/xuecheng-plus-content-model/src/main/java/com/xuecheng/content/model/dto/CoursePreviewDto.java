package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel(value = "课程预览信息类")
@Data
public class CoursePreviewDto {

    @ApiModelProperty(value = "课程基本信息 营销信息")
    private CourseBaseInfoDto courseBase;

    @ApiModelProperty(value = "课程计划信息")
    private List<TeachplanDto> teachplans;

    // 课程师资信息

}
