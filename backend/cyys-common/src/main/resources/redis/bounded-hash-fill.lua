local maximum = tonumber(ARGV[1])
-- DEL 或 TTL 到期后禁止旧查询回填；新读会取得不同的代次。
if redis.call('HGET', KEYS[1], '@generation') ~= ARGV[2] then return -1 end
local additions = 0

for index = 3, #ARGV, 2 do
    if redis.call('HEXISTS', KEYS[1], ARGV[index]) == 0 then
        additions = additions + 1
    end
end

if redis.call('HLEN', KEYS[1]) - 1 + additions > maximum then
    return 0
end

redis.call('HSET', KEYS[1], unpack(ARGV, 3))
return 1
