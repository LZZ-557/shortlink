package com.example.shortlink.service.component;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdGenerator {
    private static final String KEY = "t_short_link:id:max";//获取数据库表中的最大id
    private static final long STEP = 1000;   // 一次取 1000 个号（这就是"号段"）

    @Autowired
    StringRedisTemplate redis;//redis服务


    private long current = 0;//当前可用id，这个在项目启动后会存在Redis中
    private long max = 0;//号段的上限，当current大于max时会被赋值成表中最大id + STEP（这里等于1000）

    //获取id
    public synchronized Long nextId(){
        if(current>=max){
            max = redis.opsForValue().increment(KEY, STEP);//通过redis的方法获取到KEY存的值，这里是数据库表中的最大id值，然后加上1000
            current = max - STEP + 1;//上限+1000，current则是从这1000容量中的第1个开始
        }
        return current++;//返回id
    }

    private static final long MOD = 56800235584L;   // 62^6，代表id数量的最大上限，也代表短码位数为6，62说明使用的所有字符总数是62个
    private static final long K = 20250923L;        // 混淆乘数，为了防止用户恶意攻击
    private static final String CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";//从这里取字符串

    //通过id获取到最终短码
    public String idToShortCode(long id) {
        long mixed = (id * K) % MOD;//进行转化运算
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {//短码恒为6位
            sb.append(CHARS.charAt((int) (mixed % 62)));//一次取62的余数，这样就可以在总字符数为62的CHARS中取到一个字符
            mixed /= 62;//刷新一次
        }
        return sb.toString();//返回最终的短码
    }
}
