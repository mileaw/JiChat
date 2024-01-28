package com.ji.jichat.client.manager;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.ji.jichat.chat.api.vo.UserChatServerVO;
import com.ji.jichat.client.client.ClientInfo;
import com.ji.jichat.common.pojo.CommonResult;
import com.ji.jichat.common.pojo.PageDTO;
import com.ji.jichat.common.pojo.PageVO;
import com.ji.jichat.user.api.dto.AuthLoginDTO;
import com.ji.jichat.user.api.dto.ChatMessageDTO;
import com.ji.jichat.user.api.vo.AuthLoginVO;
import com.ji.jichat.user.api.vo.ChatMessageVO;
import com.ji.jichat.user.api.vo.UserRelationVO;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.SystemException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author jisl on 2024/1/23 9:04
 */
@Component
@Slf4j
public class JiChatServerManager {

    @Value("${user.url}")
    private String userUrl;

    @Value("${chat.url}")
    private String chatUrl;


    @Autowired
    private ClientInfo clientInfo;


    /**
     * 登录+路由服务器
     *
     * @return 路由服务器信息
     * @throws Exception
     */
    public void userLogin() {
        final AuthLoginDTO loginDTO = AuthLoginDTO.builder()
                .username(clientInfo.getUserName()).password(clientInfo.getPassword()).deviceIdentifier(clientInfo.getDeviceIdentifier())
                .deviceName(clientInfo.getDeviceName()).deviceType(clientInfo.getDeviceType()).osType(clientInfo.getOsType())
                .build();
        final String url = userUrl + "/user/login";
        final AuthLoginVO authLoginVO = exchangeResponseResult(url, HttpMethod.POST, loginDTO, new ParameterizedTypeReference<CommonResult<AuthLoginVO>>() {
        });
        clientInfo.setAuthLoginVO(authLoginVO);
        try {
            clientInfo.setIp(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        getUserChatServer();

    }


    public <T> T exchangeResponseResult(String url, HttpMethod httpMethod, Object request, ParameterizedTypeReference<CommonResult<T>> typeReference, Object... uriVariables) {
        log.info("{},url=[{}],uriVariables={},requestBody=[ {} ]", httpMethod.name(), url, Arrays.toString(uriVariables), JSON.toJSONString(request));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (Objects.nonNull(clientInfo.getAuthLoginVO())) {
            headers.set("Authorization", clientInfo.getAuthLoginVO().getAccessToken());
        }
        HttpEntity httpEntity = new HttpEntity<>(request, headers);
        ResponseEntity<CommonResult<T>> res = restTemplate.exchange(url,
                httpMethod,
                httpEntity,
                typeReference,
                uriVariables);
        final CommonResult<T> body = res.getBody();
        body.checkError();
        log.info("请求返回值:{}", body.getData());
        return body.getData();

    }


    public void getUserChatServer() {
        final UserChatServerVO authLoginVO = exchangeResponseResult(chatUrl + "/chatServer/routeServer", HttpMethod.POST, null, new ParameterizedTypeReference<CommonResult<UserChatServerVO>>() {
        });
        clientInfo.setUserChatServerVO(authLoginVO);
    }

    public void offLine() {
        exchangeResponseResult(chatUrl + "/chatServer/offLine", HttpMethod.POST, null, new ParameterizedTypeReference<CommonResult<Void>>() {
        });
    }

    public List<UserRelationVO> listUserRelation() {
        final List<UserRelationVO> userRelationVOS = exchangeResponseResult(userUrl + "/userRelation/listUserRelation", HttpMethod.GET, null, new ParameterizedTypeReference<CommonResult<List<UserRelationVO>>>() {
        });
        return userRelationVOS;
    }

    public PageVO<ChatMessageVO> queryChatMessage(ChatMessageDTO chatMessageDTO, PageDTO pageDTO) {
        final String url = userUrl + "/chatMessage/query" + convertToUrlParams(chatMessageDTO, pageDTO);
        final PageVO<ChatMessageVO> userRelationVOS = exchangeResponseResult(url, HttpMethod.GET, null, new ParameterizedTypeReference<CommonResult<PageVO<ChatMessageVO>>>() {
        });
        return userRelationVOS;
    }

    public String convertToUrlParams(Object myBean, PageDTO pageDTO) {
        final Map<String, Object> map = BeanUtil.beanToMap(myBean);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        if (Objects.nonNull(pageDTO)) {
            builder.queryParam("pageNum", pageDTO.getPageNum());
            builder.queryParam("pageSize", pageDTO.getPageSize());
        }
        return builder.build().encode().toUriString();
    }
}
