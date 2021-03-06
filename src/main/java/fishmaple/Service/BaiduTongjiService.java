package fishmaple.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import fishmaple.DAO.TongjiMapper;
import fishmaple.DTO.Tongji;
import fishmaple.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class BaiduTongjiService {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    TongjiMapper tongjiMapper;
    @Autowired
    RedisTemplate redisTemplate;

    @Value("${localConfig.baidu.tongji.name}")
    String name;
    @Value("${localConfig.baidu.tongji.pswd}")
    String pswd;
    @Value("${localConfig.baidu.tongji.token}")
    String token;
    @Value("${localConfig.baidu.tongji.siteId}")
    String siteId;

    private String formatStrDate(Integer date){
        if(date>9){
            return date.toString();
        }else{
            return "0"+date.toString();
        }
    }

    public static void main(String args[]){
        BaiduTongjiService baiduTongjiService=new BaiduTongjiService();
        baiduTongjiService.getResult();
        //System.out.println(baiduTongjiService.getJsonResult("20190209",TimeDate.getTimeStampNow()));
    }


    public Tongji getResult(){
            Tongji total = new Tongji();
            total.setDate(TimeDate.getTimeStampNow());
            //继承2019.3.4-2019.7.9误删的数据 心痛~ _~
            total.setUv(604);
            total.setPv(5629);
            total.setIp(482);
            if(null == redisTemplate.opsForValue().get("tongji")){
                Map<String,Tongji> map = getJsonResult("20190710",TimeDate.getTimeStampNow());
                String index = tongjiMapper.getIndex();

                ThreadPoolUtil.addTask(new Thread (()->{
                    for(Map.Entry<String,Tongji> entry:map.entrySet()) {
                        if (new Integer(entry.getKey().replaceAll("/", "")) > new Integer(index.replaceAll("/", ""))) {
                            tongjiMapper.add(entry.getValue());
                        } else if(new Integer(TimeDate.getTimeStampNow2(3))<new Integer(entry.getKey().replaceAll("/", ""))) {
                            tongjiMapper.update(entry.getValue());
                        }
                    }
                }));
                for(Map.Entry<String,Tongji> entry:map.entrySet()){
                    total.setUv(total.getUv()+entry.getValue().getUv());
                    total.setPv(total.getPv()+entry.getValue().getPv());
                    total.setIp(total.getIp()+entry.getValue().getIp());
                }
                redisTemplate.opsForValue().set("tongji", total,3600, TimeUnit.MINUTES);
                return total;
            }else{
                return (Tongji)redisTemplate.opsForValue().get("tongji");
            }
    }

    private String  getJson(String start,String end){
        return "{\"header\": {" +
                "        \"username\": \""+name+"\"," +
                "        \"password\": \""+pswd+"\"," +
                "        \"token\": \""+token+"\"," +
                "        \"account_type\": 1" +
                "    }," +
                "   \"body\": {" +
                "        \"site_id\": \""+siteId+"\"," +
                "        \"start_date\": \""+start+"\"," +
                "        \"end_date\": \""+end+"\"," +
                "        \"metrics\": \"pv_count,visitor_count,ip_count\"," +
                "        \"method\": \"overview/getTimeTrendRpt\"" +
                "    }" +
                "}";
    }

    public List<String> dateSplit(String start,String end){
        List list = new ArrayList<String>();
        list.add(start);
        Integer startMonth=new Integer(start.substring(4,6));
        Integer startYear=new Integer(start.substring(0,4));
        String startDayStr=start.substring(6,8);

        while(TimeDate.dateDifference(start,end)>88){
            if(new Integer(startMonth)>10){
                startYear=startYear+1;
                startMonth=1;
                start=startYear+"01"+startDayStr;
            }else{
                startYear=startYear;
                startMonth=startMonth+2;
                start=startYear+formatStrDate(startMonth)+startDayStr;

            }
            list.add(start);
        }
        list.add(end);
        return list;
    }

    private Map<String,Tongji> getJsonResult(String start , String end) {
        List<String> list =new ArrayList<>();
        Map<String,Tongji> results=new LinkedHashMap<>();
        if(TimeDate.dateDifference(start,end)>88){
            //日期拆分
            list=dateSplit(start,end);
        }else{
            list.add(start);
            list.add(end);
        }
        for(int i=0;i<list.size()-1;i++){
        String result="";
            try {
                result = HttpClientUtil.doPost("https://api.baidu.com/json/tongji/v1/ReportService/getData",getJson(list.get(i),list.get(i+1)));
                Map<String,String> map=JSON.parseObject(result,new TypeReference<Map<String,String>>(){});
                map=JSON.parseObject(map.get("body").toString(),new TypeReference<Map<String,String>>(){});
                List<String> listtmp=JSON.parseObject(map.get("data"),new TypeReference<List<String>>(){});
                map=JSON.parseObject(listtmp.get(0),new TypeReference<Map<String,String>>(){});
                map=JSON.parseObject(map.get("result"),new TypeReference<Map<String,String>>(){});
                listtmp=JSON.parseObject(map.get("items"),new TypeReference<List<String>>(){});
                List<String> dates=JSON.parseObject(listtmp.get(0),new TypeReference<List<String>>(){});
                List<String> data=JSON.parseObject(listtmp.get(1),new TypeReference<List<String>>(){});
                for(int j=0;j<dates.size();j++){
                    List<String> datatmp=JSON.parseObject(data.get(j),new TypeReference<List<String>>(){});
                    Tongji tongji=new Tongji();
                    tongji.setDate(dates.get(j).substring(2,dates.get(j).length()-2));
                    tongji.setPv((datatmp.get(0).equals("--")?0:new Integer(datatmp.get(0))));
                    tongji.setUv((datatmp.get(1).equals("--")?0:new Integer(datatmp.get(1))));
                    tongji.setIp((datatmp.get(2).equals("--")?0:new Integer(datatmp.get(2))));
                    results.put(dates.get(j).substring(2,dates.get(j).length()-2),tongji);
                }
            } catch (Exception e) {
                System.out.println("ERROR");
                logger.error("error,{}",e.getMessage());
            }
        }
        return results;
    }


}
