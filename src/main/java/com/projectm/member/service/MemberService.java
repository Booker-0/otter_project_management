package com.projectm.member.service;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.poi.excel.sax.Excel07SaxReader;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.framework.common.exception.CustomException;
import com.framework.common.utils.StringUtils;
import com.framework.common.utils.security.Md5Utils;
import com.framework.security.util.UserUtil;
import com.projectm.common.CommUtils;
import com.projectm.common.DateUtil;
import com.projectm.common.ListUtils;
import com.projectm.member.domain.Member;
import com.projectm.member.domain.MemberAccount;
import com.projectm.member.domain.ProjectMember;
import com.projectm.member.mapper.MemberAccountMapper;
import com.projectm.member.mapper.MemberMapper;
import com.projectm.member.mapper.ProjectMemberMapper;
import com.projectm.org.domain.Department;
import com.projectm.org.domain.Organization;
import com.projectm.org.mapper.OrganizationMapper;
import com.projectm.org.service.DepartmentMemberService;
import com.projectm.org.service.DepartmentService;
import com.projectm.org.service.OrganizationService;
import com.projectm.system.service.SystemConfigService;
import com.projectm.task.domain.Task;
import com.projectm.task.domain.TaskMember;
import com.projectm.task.service.TaskMemberService;
import com.projectm.task.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemberService extends ServiceImpl<MemberMapper, Member> {

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private MemberAccountMapper memberAccountMapper;

    @Autowired
    private MemberAccountService memberAccountService;

    @Autowired
    OrganizationMapper organizationMapper;
    @Autowired
    SystemConfigService systemConfigService;

    @Autowired
    TaskService taskService;
    @Autowired
    ProjectMemberService projectMemberService;

    @Autowired
    TaskMemberService taskMemberService;

    public List<Map> selectMemberByLoginParam(Map params) {
        return projectMemberMapper.selectMemberByLoginParam(params);
    }

    public List<Map> selectMemberCountByMemberCode(Map params){
        return projectMemberMapper.selectMemberCountByMemberCode(params);
    }
    //根据用户编号，查询用户信息
    public Map getMemberById(String userCode){
        List<Map> listMap = projectMemberMapper.getMemberById(userCode);
        if(null != listMap && listMap.size() > 0){
            return listMap.get(0);
        }
        return null;
    }

    //根据memberCode获取member信息
    public Member getMemberByCode(String memberCode){
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Member::getCode, memberCode);
        return baseMapper.selectOne(queryWrapper);
    }


    //根据memberCode获取member信息
    public Map getMemberMapByCode(String memberCode){
        return baseMapper.selectMemberByCode(memberCode);
    }

    @Transactional
    public Integer updateMemberAccountAndMember(MemberAccount ma,Member m){
        Integer i1 = baseMapper.updateById(m);
        Integer i2 = memberAccountMapper.updateById(ma);
        return i1+i2;
    }

    public Member getMemberByName(String account){
        QueryWrapper<Member> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", account);
        return baseMapper.selectOne(queryWrapper);
    }

    public List<Organization> getOrgList(String memberCode){
        List<MemberAccount> list = memberAccountService.lambdaQuery().select(MemberAccount::getOrganization_code).eq(MemberAccount::getMember_code, memberCode).list();
        if (CollUtil.isNotEmpty(list)) {
            List<String> orgList = list.parallelStream().map(MemberAccount::getOrganization_code).collect(Collectors.toList());
            return organizationMapper.selectList(Wrappers.<Organization>lambdaQuery().in(Organization::getCode, orgList));
        } else {
            throw new CustomException("此用户没有组织，请先添加到某组织");
        }
    }

    /*public List<Organization> getOrgList(String memberCode,boolean newest){
        List<Organization> listResult = new ArrayList<>();
        if(StringUtils.isEmpty(memberCode)){
            return listResult;
        }
        List<MemberAccount> memberAccountList=memberAccountService.lambdaQuery().eq(MemberAccount::getMember_code,memberCode).orderByAsc(MemberAccount::getId).list();
        if(!CollectionUtils.isEmpty(memberAccountList)){


        }
    }*/



    @Autowired
    DepartmentMemberService departmentMemberService;
    @Autowired
    OrganizationService organizationService;

    @Transactional
    public MemberAccount createMember(Member member){
        member.setCreate_time(DateUtil.getCurrentDateTime());
        save(member);
        return organizationService.createOrganization(member);
    }
}