local maximum = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])
local additions = 0

for index = 3, #ARGV, 2 do
    if redis.call('HEXISTS', KEYS[1], ARGV[index]) == 0 then
        additions = additions + 1
    end
end

if redis.call('HLEN', KEYS[1]) + additions > maximum then
    return 0
end

redis.call('HSET', KEYS[1], unpack(ARGV, 3))
if redis.call('TTL', KEYS[1]) < 0 then
    redis.call('EXPIRE', KEYS[1], ttl)
end
return 1
