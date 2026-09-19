package com.luojia.soundscape.album.api;

import com.luojia.soundscape.album.service.AlbumInfoService;
import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.user.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/album")
/**
 * @author Luojia Soundscape Contributors
 */
@Slf4j
public class TestController {

    @Autowired
    private RedissonClient redissonClient;


    /**
     * 测试Redissson分布式锁
     *
     * @param id
     * @return
     */
    @GetMapping("/testLock/{id}")
    public Result testLock(@PathVariable Long id) {
        //1.创建锁对象
        RLock lock = redissonClient.getLock("lock:" + id);
        //2.获取分布式锁
        lock.lock();  //当前线程，一直阻塞到获取锁成功为止
        try {
            TimeUnit.SECONDS.sleep(600);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //lock.lock(10, TimeUnit.SECONDS);  //当前线程，一直阻塞到获取锁成功为止,加锁成功后5s自动释放
        //boolean flag = lock.tryLock();  //如果锁空闲加锁成功返回true，如果被被占用加锁返回false 默认锁过期时间：30s
        //if (flag) {
        //    try {
        //        //3.获取锁成功后，执行业务
        //        log.info("线程：{}，第一次获取锁成功，执行业务...", Thread.currentThread().getName());
        //        //try {
        //        //    TimeUnit.SECONDS.sleep(600);
        //        //} catch (InterruptedException e) {
        //        //    throw new RuntimeException(e);
        //        //}
        //        //if (lock.tryLock()) {
        //        //    try {
        //        //        log.info("线程：{}，第二次获取锁成功，执行业务...", Thread.currentThread().getName());
        //        //    } finally {
        //        //        //释放锁
        //        //        lock.unlock();
        //        //    }
        //        //}
        //
        //    } finally {
        //        //4.释放锁
        //        lock.unlock();
        //    }
        //} else {
        //    log.error("线程：{}，获取锁失败...", Thread.currentThread().getName());
        //    return Result.fail();
        //}
        return Result.ok();
    }


    public static void main(String[] args) {
        List<String> list = Arrays.asList("1", "2", "3");
        // forEach中参数是Consumer函数式接口类型 只有入参没有出参
        list.stream()
                //.forEach(str -> {
                //    System.out.println("str:" + str);
                //});
                .forEach(System.out::println);
        //map 映射 参数是Function函数式接口 既有入参又有出参
        //需求：将集合中string 转为 UserInfo
        List<UserInfo> newList = list.stream().map(str -> {
            UserInfo userInfo = new UserInfo();
            userInfo.setNickname("昵称：" + str);
            return userInfo;
        }).collect(Collectors.toList());
        System.out.println(newList);


        List<Integer> numList = Arrays.asList(1, 3, 678, 899, 899);
        List<Integer> filterList = numList.stream().filter(num -> {
            //true:需要保留数据
            return num > 3;
        }).collect(Collectors.toList());
        System.out.println(filterList);


        filterList = filterList.stream().distinct().collect(Collectors.toList());
        System.out.println(filterList);

    }


    @GetMapping("/addAlbumIdToBloomFilter")
    public void addAlbumIdToBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        for (long i = 1; i < 1700; i++) {
            bloomFilter.add(i);
        }
    }

    @Autowired
    private AlbumInfoService albumInfoService;

    /**
     * 项目维护期间，重建布隆过滤器
     */
    @GetMapping("/rebildBloomFilter")
    public void rebildBloomFilter() {
        albumInfoService.rebildBloomFilter();
    }


    @GetMapping("/read/{id}")
    public Result<String> read(@PathVariable Long id) {
        //1.获取读写锁对象
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rwlock:" + id);
        //2.获取读锁对象
        RLock rLock = readWriteLock.readLock();
        //3.尝试获取读锁，获取读锁成功才执行查询业务
        rLock.lock(5, TimeUnit.SECONDS);
        try {
            log.info("线程：{}，获取读锁成功，开始查询...", Thread.currentThread().getName());
            return Result.ok("数据：" + id);
        } finally {
            //4.释放读锁
            //rLock.unlock();
        }
    }

    @GetMapping("/write/{id}")
    public Result<String> write(@PathVariable Long id) {
        //1.获取读写锁对象
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rwlock:" + id);
        //2.获取写锁对象
        RLock rLock = readWriteLock.writeLock();
        //3.尝试获取写锁，获取写锁成功才执行修改业务
        rLock.lock(10, TimeUnit.SECONDS);
        try {
            log.info("线程：{}，获取写锁成功，开始修改...", Thread.currentThread().getName());
            return Result.ok("修改成功");
        } finally {
            //4.释放写锁
            //rLock.unlock();
        }
    }

}
