local generation = redis.call('HGET', KEYS[1], '@generation')
if not generation then
    generation = ARGV[1]
    redis.call('HSET', KEYS[1], '@generation', generation)
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end
return generation
