package fishmaple.MainController;


import com.alibaba.fastjson.JSON;
import fishmaple.DAO.*;
import fishmaple.DTO.*;
import fishmaple.DTO.Dictionary;
import fishmaple.Service.*;
import fishmaple.shiro.ShiroService;
import fishmaple.task.LoadBlogListTask;
import fishmaple.utils.PublicConst;
import fishmaple.utils.ThreadPoolUtil;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;


@Controller
public class MainController {

    Logger log= LoggerFactory.getLogger(MainController.class);

    @Autowired
    BlogService blogService;
    @Autowired
    ToolMapper toolMapper;
    @Autowired
    DictionaryMapper dictionaryMapper;
    @Autowired
    ConfigMapper configMapper;
    @Autowired
    MobileService mobileService;
    @Autowired
    IssueService issueService;
    @Autowired
    ShiroService shiroService;
    @Autowired
    FriendLinksMapper friendLinksMapper;
    @Autowired
    BlogTopicService blogTopicService;
    @Autowired
    BlogTopicMapper blogTopicMapper;
    @Autowired
    LoadBlogListTask loadBlogListTask;
    @Autowired
    DocMapper docMapper;
    @Autowired
    EventLogMapper eventLogMapper;

    @RequestMapping("/blogEditor")
    public String blogEditor(@RequestParam(required = false) String xx
    ,@RequestParam(required = false) String bid){
           return "blogEditor";
    }

    @RequestMapping("/uc")
    public String userCenter(Model model){
        model.addAttribute("uName",shiroService.getUserName());
        return "userCenter";
    }
    @RequestMapping("/search")
    public String blogEditor(HttpServletRequest request){
        String ip = request.getRemoteAddr();
        log.info("{} 访问搜索页",ip);
        ThreadPoolUtil.addTask(()->{
            eventLogMapper.insert("搜索页",ip,6,
                    null==request.getHeader("referer")?"-":request.getHeader("referer"));
        });
        return "search";
    }
    @RequestMapping("/issues")
    public String issues(HttpServletRequest request){
        String ip = request.getRemoteAddr();
        log.info("{} 访问issues",ip);
        ThreadPoolUtil.addTask(()->{
            eventLogMapper.insert("Issues页",ip,2,null==request.getHeader("referer")?"-":request.getHeader("referer"));
        });
        return "issues";
    }
    @RequestMapping("/issues/new")
    public String issuesNew(){
        return "issuesEdit";
    }
    @RequestMapping("/issue/d")
    public String issuesDetail(@RequestParam String id,HttpServletRequest request, Model model){
        Issue issue= issueService.getDetail(id);
        model.addAttribute("issue",issue);
        String ip = request.getRemoteAddr();
        log.info("{} 访问issue {}",ip,id);
        ThreadPoolUtil.addTask(()->{
            eventLogMapper.insert("Issue页 "+id,ip,3,
                    null==request.getHeader("referer")?"-":request.getHeader("referer"));
        });
        return "issue/issueDetail";
    }

    @RequestMapping("/blog/d")
    public String blogDetail(HttpServletRequest request,@RequestParam String bid, Model model){
        Blog blog=blogService.getBlogById(bid,false);
        model.addAttribute("describe",blogService.blogLine(blog));
        model.addAttribute("blog",blog);
        String ip = request.getRemoteAddr();
        log.info("{} 访问博客{} {}",ip,bid,blog.getTitle());
        ThreadPoolUtil.addTask(()->{
            eventLogMapper.insert("博客详情页"+blog.getTitle(),ip,1,
                    null==request.getHeader("referer")?"-":request.getHeader("referer"));

        });
        return mobileHandler(request,"blogdetail");
    }

    @RequestMapping("/blog/index")
    public String blogIndex(HttpServletRequest request, Model model, HttpServletResponse response) {
        String content="";
        List<Blog> list= blogService.getBlogList();
        model.addAttribute("blog",list);
        return mobileHandler(request,"blogIndex");
    }

    @RequestMapping("/blog/topicBlog")
    public String blogTopic(@RequestParam Integer topicId,HttpServletRequest request, Model model, HttpServletResponse response) {
        List<Blog> list= new ArrayList<>();
        BlogTopic topic = blogTopicMapper.getTopicById(topicId);
        List<BlogTopic> topics=null;
        model.addAttribute("isSub",false);
        if(topic.getfTopicId()==-1){
             topics=blogTopicMapper.getSubTopicById(topicId);
             for(BlogTopic subTopic:topics ) {
                    List<Blog> tmp = blogService.getBlogListByTopicId(subTopic.getId());
                    if(!tmp.isEmpty()){
                        list.addAll(tmp);
                    }
             }
        }else if(topic.getfTopicId()==0){
            list= blogService.getBlogListByTopicId(topicId);
        }else{
            list= blogService.getBlogListByTopicId(topicId);
            BlogTopic parentTopic = blogTopicMapper.getTopicById(topic.getfTopicId());
            model.addAttribute("PaTopic",parentTopic);
            model.addAttribute("isSub",true);
        }
        model.addAttribute("topic",topic.getTopic());
        model.addAttribute("subTopic",topics);
        model.addAttribute("blog",list);
        return mobileHandler(request,"topicBlog");
    }

    @RequestMapping("/sideWall")
    public String sideWall(HttpServletRequest request, Model model, HttpServletResponse response) {
        String ip = request.getRemoteAddr();
        log.info("{} 访问留言页",ip);
        ThreadPoolUtil.addTask(()->{
            eventLogMapper.insert("留言页",ip,7,
                    null==request.getHeader("referer")?"-":request.getHeader("referer"));
        });
        return "sideWall";
    }

    @RequestMapping("/doc")
    public String doc(HttpServletRequest request,Model model){
        String ip = request.getRemoteAddr();
        log.info("{} 访问文档仓库页",ip);
        ThreadPoolUtil.addTask(()->{
            eventLogMapper.insert("文档页",ip,8,
                    null==request.getHeader("referer")?"-":request.getHeader("referer"));
        });
        model.addAttribute("respository",docMapper.getRespositories());
        return "doc";
    }

    @RequestMapping("/reveal")
    public String reveal(HttpServletRequest request, Model model, HttpServletResponse response) {
        return "reveal";
    }

    @RequestMapping("/blog")
    public String blog(HttpServletRequest request, Model model, HttpServletResponse response) {
        String content=loadBlogListTask.getOutLine();
        model.addAttribute("cover",configMapper.getValue("cover"));
        model.addAttribute("content",content);
        model.addAttribute("page",1);
        model.addAttribute("pageD","");
        model.addAttribute("bgcss",configMapper.getValue("bg"));
        model.addAttribute("blogTopics",blogTopicService.getAllTopics());
        String ip = request.getRemoteAddr();
        log.info("{} 访问首页",ip);
        ThreadPoolUtil.addTask(()->{
            eventLogMapper.insert("首页",ip,0,
                    null==request.getHeader("referer")?"-":request.getHeader("referer"));
        });
        return mobileHandler(request,"blog");
    }

    @RequestMapping("/blog/{page}")
    public String blog(HttpServletRequest request,@PathVariable int page, Model model){
        String content="";
        List<Blog> list= blogService.getBlogList(page);
        for(Blog b:list){
            content += b.getOutLine();
        }
        model.addAttribute("content",content);
        model.addAttribute("page",page);
        model.addAttribute("pageD",(page>1)?"第"+page+"页-":"");
        return mobileHandler(request,"blog");
    }
    @RequestMapping("/editor")
    public String editor(HttpServletRequest request){
        log.info(request.getRemoteAddr()+" "+request.getRequestURI()+" "+"编辑博客");
        return "editor";
    }

    @RequestMapping("/master")
    public String master(HttpServletRequest request){
        return "master";
    }


  @RequestMapping("/tool")
    public String main(HttpServletRequest request,Model model){
        String content="";
        List<Tool> list=toolMapper.getAllTools();
        for(Tool tool:list){
            content+=tool.getOutLine();
        }
        model.addAttribute("content",content);

        log.info(request.getRemoteAddr()+" "+request.getRequestURI()+" "+"访问工具箱");
        return "tools";
    }
    @RequestMapping("/dictionary")
    public String wiki(HttpServletRequest request,Model model){
        Long currentUserId = (Long) SecurityUtils.getSubject().getSession().getAttribute("currentUserId");
        String content="";
        List<Dictionary> list=dictionaryMapper.getDictionary();
        for(Dictionary dic:list){
            content+=dic.getOutLine();
        }
        model.addAttribute("content",content);
        log.info(request.getRemoteAddr()+" "+request.getRequestURI()+" "+"访问wiki");
        return "dictionary";
    }

    @RequestMapping("/lab")
    public String lab(){
        log.info("访问实验室");
        return "lab";
    }

  @RequestMapping("/fishchat")
  public String chat(){
        log.info("访问聊天室");
        return "fishChat";
  }

    @RequestMapping("/document")
    public String document(){
        log.info("文件托管");
        return "document";
    }

  @RequestMapping("/")
  public String index2Blog(HttpServletRequest request,Model model,HttpServletResponse response){
     return blog(request,model,response);
  }
    @RequestMapping("/friendLinks")
    public String friendLinks(HttpServletRequest request,Model model,HttpServletResponse response){
        String ip = request.getRemoteAddr();
        log.info("{} 访问友链页",ip);
        ThreadPoolUtil.addTask(()->{
            eventLogMapper.insert("友链页",ip,8,
                    null==request.getHeader("referer")?"-":request.getHeader("referer"));
        });
        model.addAttribute("outLine",friendLinksMapper.getAll());
        return "friendLinks";
    }
    @RequestMapping("/index")
    public String index(HttpServletRequest request,Model model,HttpServletResponse response){
        return blog(request,model,response);
    }

    @RequestMapping("/can")
    public String can(HttpServletRequest request){
        return mobileHandler(request,"can");
    }
    @RequestMapping("/beam")
    public String beam(HttpServletRequest request){
       return mobileService.toMobile(request,"beam");
    }



    @RequestMapping("/m/beam")
    public String mbeam(HttpServletRequest request){
                  return "m/beam";
    }


    private String mobileHandler(HttpServletRequest request,String toUrl){
        if (request.getHeader("User-Agent") != null) {
            if(request.getHeader("User-Agent").indexOf("Trident")>=0){
                return  "ieBan";
            }
            for (String mobileAgent : PublicConst.mobileAgents) {
                if (request.getHeader("User-Agent").toLowerCase().indexOf(mobileAgent) >= 0) {
                    return "m/"+toUrl;
                }
            }
        }
        return toUrl;
    }
}
