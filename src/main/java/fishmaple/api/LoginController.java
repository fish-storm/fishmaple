package fishmaple.api;


import fishmaple.DAO.LoginLogMapper;
import fishmaple.DAO.UserMapper;
import fishmaple.DTO.User;
import fishmaple.shiro.ShiroService;
import fishmaple.shiro.TokenService;
import fishmaple.utils.*;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author 鱼鱼
 * 注册、登录的接口
 * */
@RestController
@RequestMapping("/api")
public class LoginController {
    @Autowired
    PswEncodeFacade pswEncodeFacade;
    @Autowired
    UserMapper userMapper;
    @Autowired
    ShiroService shiroService;
    @Autowired
    TokenService tokenService;
    @Autowired
    LoginLogMapper loginLogMapper;
    @RequestMapping("/registerCheck")
    public String registerCheck(@RequestBody User user) {
        if(userMapper.selectNameCount(user.getName())>0){
            return "用户名重复";
        }else if(user.getName().indexOf(" ")>-1){
            return "用户名含有非法字符";
        }else if(user.getName().length()>15){
            return "用户名过长";
        }else if(user.getPswd().length()<6){
            return "密码太短啦";
        }else if(user.getName().equals(user.getPswd())){
            return "用户名请不要与密码相同";
        }else
            return "ok";
    }

    @RequestMapping(value="/register",method= RequestMethod.POST)
    public String register(@RequestParam(required = false) String from,@RequestBody User user,HttpServletRequest request) {
        if(user.getPswd().contains("@@@@@@@@@")){
            return "密码含有非法字符";
        }
        if(registerCheck(user).equals("ok")){
            String psw= pswEncodeFacade.pswEncode(user.getPswd(),user.getName());
            Long timenow=System.currentTimeMillis()/1000;
            userMapper.setUser(EncoderUtil.getUUID(1),user.getName(),timenow,psw);
            Map<String,String> map=RequestUtil.getInfo(request);


            loginLogMapper.insertLog(EncoderUtil.getUUID(1),user.getName(),
                    TimeDate.timestamp2time(0L,0),map.get("browser"),map.get("os"),
                    "m".equals(from)?"register-app":"register",request.getRemoteAddr());
            return "SUCCESS";
        }
            return registerCheck(user);
    }

    @PostMapping(value="/tokenSign")
    public Map<String,String> tokenSign(@RequestBody User user,HttpServletRequest request) {
        Map<String,String> result = new HashMap<>();
        if(user.getPswd().contains("@@@@@@@@@")){
            result.put("msg","密码含有非法字符");
        }
        Subject subject= SecurityUtils.getSubject();
        try{
            UsernamePasswordToken userToken=new UsernamePasswordToken(user.getName(),user.getPswd());
            subject.login(userToken);

            Map<String,String> map=RequestUtil.getInfo(request);
            String uid=EncoderUtil.getUUID(1);
            loginLogMapper.insertLog(EncoderUtil.getUUID(1),user.getName(),
                    TimeDate.timestamp2time(0L,0),map.get("browser"),map.get("os"),
                    "login-app",request.getRemoteAddr());
            result.put("msg","SUCCESS");
            result.put("token",tokenService.sign(user.getName(),user.getPswd(),shiroService.getCurrentUser().getId()));
            result.put("id",shiroService.getCurrentUser().getId());
        }catch(UnknownAccountException e){
            result.put("msg","用户名不存在");
        }catch(IncorrectCredentialsException e){
            result.put("msg","密码错误");
        }catch(AuthenticationException e){
            result.put("msg","无此用户");
        }
        return result;
    }

    @PostMapping(value="/tokenLogin")
    public String tokenSign(@RequestBody Map<String,String> map,HttpServletRequest request) {
        if(tokenService.setTempQrId(map.get("uuid"),map.get("token"),request)){
            return "SUCCESS";
        }else{
        return "会话已过期";
        }
    }

    @RequestMapping(value="/login",method= RequestMethod.POST)
    public String login(@RequestBody User user, HttpServletRequest request) {
        Subject subject=SecurityUtils.getSubject();
        if(user.getPswd().contains("@@@@@@@@@")){
            return "含有非法字符";
        }
        UsernamePasswordToken userToken=new UsernamePasswordToken(user.getName(),user.getPswd());
        userToken.setRememberMe(user.getRememberMe());
        try{
            Jedis jedis= JedisUtil.getJedis();
            subject.login(userToken);
            Map<String,String> map=RequestUtil.getInfo(request);
            loginLogMapper.insertLog(EncoderUtil.getUUID(1),user.getName(),
                    TimeDate.timestamp2time(System.currentTimeMillis(),0),map.get("browser"),map.get("os"),
                    "login",request.getRemoteAddr());
            if(!jedis.sismember("currentUsers", user.getName())) {
                jedis.sadd("currentUsers", user.getName());
                jedis.close();
            }
            return "SUCCESS";
        }catch(UnknownAccountException e){
            return "用户名不存在";
        }catch(IncorrectCredentialsException e){
            return "密码错误";
        }catch(AuthenticationException e){
            return "无此用户";
        }
    }
    @RequestMapping("/online")
    public Long getCurrentUserCount(){
        Jedis jedis= JedisUtil.getJedis();
        Long count=jedis.scard("currentUsers");
        jedis.close();
        return count;
    }
    @RequestMapping("/online/d")
    public Set<String> getCurrentUser(){
        Jedis jedis= JedisUtil.getJedis();
        Set<String> setValues = jedis.smembers("currentUsers");
        jedis.close();
        return setValues;
    }
    @RequestMapping("/logout")
    public void logout(HttpServletResponse response) throws IOException {
        Jedis jedis= JedisUtil.getJedis();
        jedis.srem("currentUsers",shiroService.getUserName());
        jedis.close();
        response.sendRedirect("/logout");

    }
    /**
     * 根据随机码生成登录用的随机二维码
     * */
    @RequestMapping("/getQrLogin")
    public void getQrPic(HttpServletResponse response, @RequestParam String uuid) throws Exception {
        response.setContentType("image/png");
        QrCodeUtil.generateQRCode(uuid,null,null,200,210,"png",response,null);
    }

    /**
     *  校验二维码，是否已登录
     * */
    @GetMapping("/checkLogin")
    public String checkLogin(HttpServletResponse response, @RequestParam String uuid) {
        if(tokenService.getToken(uuid)!=null){
            String uid = tokenService.getUserId(tokenService.getToken(uuid));
            User user=userMapper.selectUserById(uid);

            UsernamePasswordToken userToken=new UsernamePasswordToken(user.getName(),"@@@@@@@@"+user.getPswd());
            Subject subject= SecurityUtils.getSubject();

            try {
                Jedis jedis = JedisUtil.getJedis();
                subject.login(userToken);

                if (!jedis.sismember("currentUsers", user.getName())) {
                    jedis.sadd("currentUsers", user.getName());
                    jedis.close();
                }
            }catch(AuthenticationException e){
                return "业务异常";
            }
            return "true";
        }else{
            return "false";
        }
    }

    /**
     * 生成随机码
     * */
    @RequestMapping("/getQrUUID")
    public String getLoginUuid(HttpServletResponse response)  {
        return EncoderUtil.getUUID(0)+".-"+EncoderUtil.getUUID(0);
    }
}
