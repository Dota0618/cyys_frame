local removed = 0
for _, key in ipairs(KEYS) do
    if redis.call('EXISTS', key) == 1 then
        -- 保留其他记录和原 TTL；旋转代次阻止修改前查询回填旧值。
        redis.call('HSET', key, '@generation', ARGV[1])
        for index = 2, #ARGV do removed = removed + redis.call('HDEL', key, ARGV[index]) end
    end
end
return removed
