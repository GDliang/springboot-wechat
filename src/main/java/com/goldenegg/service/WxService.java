package com.goldenegg.service;

import com.goldenegg.entity.User;
import com.goldenegg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class WxService {

    @Value("${wx.miniapp.appid}")
    private String appid;

    @Value("${wx.miniapp.secret}")
    private String secret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    public Map<String, Object> login(String code) {
        // 调用微信API获取openid
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, code
        );

        Map<String, Object> result = restTemplate.getForObject(url, Map.class);

        if (result != null && result.containsKey("openid")) {
            String openid = (String) result.get("openid");

            // 保存或更新用户信息
            User user = userRepository.findByOpenid(openid)
                    .orElse(new User());
            user.setOpenid(openid);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("openid", openid);
            response.put("session_key", result.get("session_key"));
            return response;
        }

        throw new RuntimeException("微信登录失败");
    }

    public User saveUserInfo(String openid, String nickname, String avatarUrl) {
        User user = userRepository.findByOpenid(openid)
                .orElse(new User());
        user.setOpenid(openid);
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        user.setHasContactInfo(false); // 默认未填写联系方式
        return userRepository.save(user);
    }
}