package org.jenkinsci.plugins.qywechat.dto;

import hudson.model.*;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.qywechat.NotificationUtil;
import org.jenkinsci.plugins.qywechat.model.NotificationConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结束构建的通知信息
 * @author jiaju
 */
public class BuildPreInfo {
    /**
     * 请求参数
     */
    private Map params = new HashMap<String, Object>();
    /**
     * 使用时间，毫秒
     */
    private String useTimeString = "";

    /**
     * 预计时间，毫秒
     */
    private Long durationTime = 0L;

    /**
     * 本次构建控制台地址
     */
    private String consoleUrl;

    /**
     * 工程名称
     */
    private String projectName;
    private String projectDescription;
    private String buildUser;

    /**
     * 环境名称
     */
    private String topicName = "";

    /**
     * 更多自定消息
     */
    private String moreInfo = "";

    /**
     * 执行结果
     */
    private Result result;

    public BuildPreInfo(String projectName, String projectDescription, Run<?, ?> run, NotificationConfig config){
        // 使用时间
        this.useTimeString = run.getTimestampString();
        //获取请求参数
       /* List<ParametersAction> parameterList = build.getActions(ParametersAction.class);
        if(parameterList!=null && parameterList.size()>0){
            for(ParametersAction p : parameterList){
                for(ParameterValue pv : p.getParameters()){
                    this.params.put(pv.getName(), pv.getValue());
                }
            }
        }*/
        //预计时间
        /*if(build.getProject().getEstimatedDuration()>0){
            this.durationTime = build.getProject().getEstimatedDuration();
        }*/

        // 控制台地址
        StringBuilder urlBuilder = new StringBuilder();
        String jenkinsUrl = NotificationUtil.getJenkinsUrl();
        if(StringUtils.isNotEmpty(jenkinsUrl)){
            urlBuilder.append(jenkinsUrl);
            if(!jenkinsUrl.endsWith("/")){
                urlBuilder.append("/");
            }
            String buildUrl = run.getUrl();

            urlBuilder.append(buildUrl);
            if(!buildUrl.endsWith("/")){
                urlBuilder.append("/");
            }
            urlBuilder.append("console");
        }
        this.consoleUrl = urlBuilder.toString();
        //工程名称
        this.projectName = projectName;
        this.projectDescription = projectDescription;
        Cause.UserIdCause userIdCause = run.getCause(Cause.UserIdCause.class);
        this.buildUser = userIdCause != null ? userIdCause.getUserName() : null;
        //环境名称
        if(config.topicName!=null){
            topicName = config.topicName;
        }
        if (StringUtils.isNotEmpty(config.moreInfo)){
            moreInfo = config.moreInfo;
        }
        //结果
        result = run.getResult();
    }

    public String toJSONString(){

        //组装内容
        StringBuilder content = new StringBuilder();
        if(StringUtils.isNotEmpty(topicName)){
            content.append(this.topicName);
        }
        content.append("**<font color=\"info\">【" + this.projectName + "】</font>开始构建**\n");
        if(!params.isEmpty()) {
            StringBuffer paramBuffer = new StringBuffer();
            params.forEach((key, val)->{
                paramBuffer.append(key);
                paramBuffer.append("=");
                paramBuffer.append(val);
                paramBuffer.append(", ");
            });
            if(paramBuffer.length()==0){
                paramBuffer.append("无");
            }else{
                paramBuffer.deleteCharAt(paramBuffer.length()-2);
            }
            content.append(" >构建参数：<font color=\"comment\">" + paramBuffer + "</font>\n");
        }


        if (StringUtils.isNotEmpty(projectDescription)) {
            content.append(" >任务描述：<font color=\"comment\">" +  this.projectDescription + "</font>\n");
        }
        if (StringUtils.isNotEmpty(buildUser)) {
            content.append(" >构建人员：<font color=\"comment\">" +  this.buildUser + "</font>\n");
        }
/*
        content.append(" >预计用时：<font color=\"comment\">" +  this.useTimeString + "</font>\n");
*/

        if (StringUtils.isNotEmpty(moreInfo)){
            content.append(" >"+moreInfo+"\n");
        }
        if(StringUtils.isNotEmpty(this.consoleUrl)) {
            content.append(" >[查看控制台](" + this.consoleUrl + ")");
        }

        Map markdown = new HashMap<String, Object>();
        markdown.put("content", content.toString());

        Map data = new HashMap<String, Object>();
        data.put("msgtype", "markdown");
        data.put("markdown", markdown);

        return JSONObject.fromObject(data).toString();
    }
}
