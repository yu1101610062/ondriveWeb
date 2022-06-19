package com.yyzy.ondriveweb.service.impl;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.ListItemRequestBuilder;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import com.yyzy.ondriveweb.dao.OneDriveUserDao;
import com.yyzy.ondriveweb.dto.OneDriveUser;
import com.yyzy.ondriveweb.dto.common.Result;
import com.yyzy.ondriveweb.service.OneDriveService;
import lombok.RequiredArgsConstructor;
import okhttp3.Request;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OneDriveServiceImpl implements OneDriveService {

    private final OneDriveUserDao oneDriveUserDao;

    /**
     * //TODO 需要登录模块实现
     * login need
     */
    @Override
    public Result<OneDriveUser> getOneDriveUserForAdmin(Long driveId) {
        OneDriveUser oneDriveUser = oneDriveUserDao.selectById(driveId);
        return new Result<OneDriveUser>().setSuccessResult(oneDriveUser);
    }

    /**
     * 仅包含name与driveId
     * just include name and driveId
     */
    @Override
    public Result<List<OneDriveUser>> getOneDriveUserList() {
        return new Result<List<OneDriveUser>>().setSuccessResult(oneDriveUserDao.selectList(new QueryWrapper<OneDriveUser>().select("id", "name")));
    }

    /**
     * //TODO 需要登录模块实现
     * login need
     */
    @Override
    public Result<List<OneDriveUser>> getOneDriveUserListForAdmin() {
        return new Result<List<OneDriveUser>>().setSuccessResult(oneDriveUserDao.selectList(new QueryWrapper<>()));
    }

    /**
     * //TODO 需要登录模块实现
     * login need
     */
    @Override
    public Result addOrUpdateOneDriveUser(OneDriveUser oneDriveUser) {
        int i = 0;
        if (oneDriveUser.getId() == null) {
            i = oneDriveUserDao.insert(oneDriveUser);

        } else {
            i = oneDriveUserDao.updateById(oneDriveUser);
        }
        return i > 0 ? new Result().setSuccessResult(null) : new Result().setFailResult();

    }

    @Override
    public Result<List<DriveItem>> getFileList(Long driveId, String itemsId) {
        //根据driveId找到登陆凭证并获取客户端实例
        OneDriveUser oneDriveUser = oneDriveUserDao.selectById(driveId);
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(oneDriveUser.getClientId())
                .clientSecret(oneDriveUser.getClientSecret())
                .tenantId(oneDriveUser.getTenantId())
                .build();
        TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(clientSecretCredential);
        GraphServiceClient<Request> client = GraphServiceClient.builder()
                .authenticationProvider(tokenCredentialAuthProvider)
                .buildClient();

        DriveItemCollectionPage driveItemCollectionPage;
        if (StringUtils.isNotBlank(itemsId)) {
            //查询指定目录
            driveItemCollectionPage = client.users(oneDriveUser.getMail()).drive().items(itemsId).children().buildRequest().top(Integer.MAX_VALUE).get();
        } else {
            //查询根目录
            driveItemCollectionPage = client.users(oneDriveUser.getMail()).drive().root().children().buildRequest().top(Integer.MAX_VALUE).get();
        }
        if (driveItemCollectionPage != null) {
            return new Result<List<DriveItem>>().setSuccessResult(driveItemCollectionPage.getCurrentPage());
        }
        return new Result<List<DriveItem>>().setFailResult();
    }

    @Override
    public Result<String> getFileDownloadUrl(Long driveId, String itemsId) {
        //根据driveId找到登陆凭证并获取客户端实例
        OneDriveUser oneDriveUser = oneDriveUserDao.selectById(driveId);
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(oneDriveUser.getClientId())
                .clientSecret(oneDriveUser.getClientSecret())
                .tenantId(oneDriveUser.getTenantId())
                .build();
        TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(clientSecretCredential);
        GraphServiceClient<Request> client = GraphServiceClient.builder()
                .authenticationProvider(tokenCredentialAuthProvider)
                .buildClient();
        JsonElement jsonElement = client.customRequest("/users/" + oneDriveUser.getMail() + "/drive/items/" + itemsId + "?@microsoft.graph.downloadUrl").buildRequest().get();
        if (jsonElement == null) {
            return new Result<String>().setFailResult();
        }
        return new Result<String>().setSuccessResult(jsonElement.getAsJsonObject().get("@microsoft.graph.downloadUrl").getAsString());
    }
}
