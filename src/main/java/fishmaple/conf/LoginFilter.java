package fishmaple.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class LoginFilter implements WebMvcConfigurer {
    @Autowired
    NFHandler nfHandler;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(nfHandler).addPathPatterns("/*.png,/*.jpg,/*.gif");
    }

}
