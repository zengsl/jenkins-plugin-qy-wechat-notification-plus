package org.jenkinsci.plugins.qywechat;

import com.arronlong.httpclientutil.exception.HttpProcessException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.qywechat.dto.BuildMentionedInfo;
import org.jenkinsci.plugins.qywechat.dto.BuildPreInfo;
import org.jenkinsci.plugins.qywechat.model.NotificationConfig;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * 企业微信构建通知
 *
 * @author jiaju
 */
public class QyWechatPreNotify extends Publisher implements SimpleBuildStep {

    private String webhookUrl;

    private String mentionedId;

    private String mentionedMobile;

    private String moreInfo;

    private boolean failNotify;

    private boolean onlyFailSendQyWechatNotify;

    private String projectName;

    private String projectDescription;

    @Extension
    public static final DescriptorPreImpl DESCRIPTOR = new DescriptorPreImpl();

    @DataBoundConstructor
    public QyWechatPreNotify() {
    }

    /**
     * 开始执行构建
     *
     * @param build
     * @param listener
     * @return
     */
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return true;
    }

    /**
     * 构建结束
     *
     * @param run
     * @param workspace
     * @param launcher
     * @param listener
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        NotificationConfig config = getConfig(run.getEnvironment(listener));
        if (StringUtils.isEmpty(config.webhookUrl)) {
            return;
        }
        Result result = run.getResult();
        //设置当前项目名称
        this.projectName = run.getParent().getFullDisplayName();
        this.projectDescription = run.getParent().getDescription();
        //构建结束通知
        BuildPreInfo buildInfo = new BuildPreInfo(this.projectName, this.projectDescription, run, config);
        String req = buildInfo.toJSONString();
        //运行不成功
        if (result == null) {
            return;
        }

        //推送结束通知
        if (!result.equals(Result.SUCCESS) || !config.onlyFailSendQyWechatNotify) {
            listener.getLogger().println("推送开始构建通知" + req);
            push(listener.getLogger(), config.webhookUrl, req, config);
        }

        listener.getLogger().println("项目开始构建结果[" + result + "]");

        //仅在失败的时候，才进行@
        if (!result.equals(Result.SUCCESS) || !config.failNotify) {
            //没有填写UserId和手机号码
            if (StringUtils.isEmpty(config.mentionedId) && StringUtils.isEmpty(config.mentionedMobile)) {
                return;
            }

            //构建@通知
            BuildMentionedInfo consoleInfo = new BuildMentionedInfo(run, config);

            req = consoleInfo.toJSONString();
            listener.getLogger().println("推送通知" + req);
            //执行推送
            push(listener.getLogger(), config.webhookUrl, req, config);
        }
    }

    /**
     * 推送消息
     *
     * @param logger
     * @param url
     * @param data
     * @param config
     */
    private void push(PrintStream logger, String url, String data, NotificationConfig config) {
        String[] urls;
        if (url.contains(",")) {
            urls = url.split(",");
        } else {
            urls = new String[]{url};
        }
        for (String u : urls) {
            try {
                String msg = NotificationUtil.push(u, data, config);
                logger.println("通知结果" + msg);
            } catch (HttpProcessException | KeyManagementException | NoSuchAlgorithmException e) {
                logger.println("通知异常" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * 读取配置，将当前Job与全局配置整合
     *
     * @param envVars
     * @return
     */
    public NotificationConfig getConfig(EnvVars envVars) {
        NotificationConfig config = DESCRIPTOR.getUnsaveConfig();
        if (StringUtils.isNotEmpty(webhookUrl)) {
            config.webhookUrl = webhookUrl;
        }
        if (StringUtils.isNotEmpty(mentionedId)) {
            config.mentionedId = mentionedId;
        }
        if (StringUtils.isNotEmpty(mentionedMobile)) {
            config.mentionedMobile = mentionedMobile;
        }
        if (StringUtils.isNotEmpty(moreInfo)) {
            config.moreInfo = moreInfo;
        }
        config.failNotify = failNotify;
        config.onlyFailSendQyWechatNotify = onlyFailSendQyWechatNotify;
        //使用环境变量
        if (config.webhookUrl.contains("$")) {
            String val = NotificationUtil.replaceMultipleEnvValue(config.webhookUrl, envVars);
            config.webhookUrl = val;
        }
        if (config.mentionedId.contains("$")) {
            String val = NotificationUtil.replaceMultipleEnvValue(config.mentionedId, envVars);
            config.mentionedId = val;
        }
        if (config.mentionedMobile.contains("$")) {
            String val = NotificationUtil.replaceMultipleEnvValue(config.mentionedMobile, envVars);
            config.mentionedMobile = val;
        }
        if (config.moreInfo.contains("$")) {
            String val = NotificationUtil.replaceEnvs(config.moreInfo, envVars);
            config.moreInfo = val;
        }
        return config;
    }

    /**
     * 下面为GetSet方法，当前Job保存时进行绑定
     **/

    @DataBoundSetter
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @DataBoundSetter
    public void setMentionedId(String mentionedId) {
        this.mentionedId = mentionedId;
    }

    @DataBoundSetter
    public void setMentionedMobile(String mentionedMobile) {
        this.mentionedMobile = mentionedMobile;
    }

    @DataBoundSetter
    public void setFailNotify(boolean failNotify) {
        this.failNotify = failNotify;
    }

    @DataBoundSetter
    public void setOnlyFailSendQyWechatNotify(boolean onlyFailSendQyWechatNotify) {
        this.onlyFailSendQyWechatNotify = onlyFailSendQyWechatNotify;
    }

    @DataBoundSetter
    public void setMoreInfo(String moreInfo) {
        this.moreInfo = moreInfo;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getMentionedId() {
        return mentionedId;
    }

    public String getMentionedMobile() {
        return mentionedMobile;
    }

    public boolean isFailNotify() {
        return failNotify;
    }

    public boolean isOnlyFailSendQyWechatNotify() {
        return onlyFailSendQyWechatNotify;
    }

    public String getMoreInfo() {
        return moreInfo;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }
}

